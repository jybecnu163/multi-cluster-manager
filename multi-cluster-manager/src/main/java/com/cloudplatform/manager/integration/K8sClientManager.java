package com.cloudplatform.manager.integration;

import com.cloudplatform.manager.mapper.ClusterMapper;
import com.cloudplatform.manager.model.entity.Cluster;
import com.cloudplatform.manager.util.EncryptionUtil;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class K8sClientManager {

    private final ClusterMapper clusterMapper;
    private final EncryptionUtil encryptionUtil;

    // 存储活跃的 KubernetesClient，key = clusterId
    private final Map<Long, KubernetesClient> clientMap = new ConcurrentHashMap<>();
    // 存储集群的离线计数（连续失败次数）
    private final Map<Long, AtomicInteger> offlineCounter = new ConcurrentHashMap<>();

    @Value("${k8s.client.connection-timeout-ms:30000}")
    private long connectionTimeoutMs;

    /**
     * 启动时加载所有集群，并初始化客户端
     */
    @PostConstruct
    public void initClients() {
        List<Cluster> clusters = clusterMapper.selectList(null);
        for (Cluster cluster : clusters) {
            registerCluster(cluster);
        }
        log.info("Initialized {} K8s clients", clientMap.size());
    }

    /**
     * 注册（或刷新）集群客户端
     */
    public void registerCluster(Cluster cluster) {
        try {
            // 解密 kubeconfig
            String kubeconfigContent = encryptionUtil.decrypt(cluster.getKubeconfigEncrypted());
            Config config = Config.fromKubeconfig(kubeconfigContent);
            // 可选：覆盖 API Endpoint（如果数据库中的 endpoint 与 kubeconfig 中不一致）
            if (cluster.getApiEndpoint() != null && !cluster.getApiEndpoint().isEmpty()) {
                config.setMasterUrl(cluster.getApiEndpoint());
            }
            KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build();
            // 测试连通性
            client.getVersion(); // 会触发实际请求
            // 关闭旧的客户端（如果存在）
            KubernetesClient old = clientMap.put(cluster.getId(), client);
            if (old != null) {
                old.close();
            }
            offlineCounter.remove(cluster.getId());
            updateClusterStatus(cluster.getId(), "online");
            log.info("Registered cluster {} ({}) successfully", cluster.getName(), cluster.getId());
        } catch (Exception e) {
            log.error("Failed to register cluster {}: {}", cluster.getName(), e.getMessage(), e);
            updateClusterStatus(cluster.getId(), "offline");
        }
    }

    /**
     * 移除集群客户端
     */
    public void removeCluster(Long clusterId) {
        KubernetesClient client = clientMap.remove(clusterId);
        if (client != null) {
            client.close();
        }
        offlineCounter.remove(clusterId);
        log.info("Removed cluster client for id {}", clusterId);
    }

    /**
     * 获取指定集群的 KubernetesClient
     * @throws RuntimeException 如果客户端不存在或集群离线
     */
    public KubernetesClient getClient(Long clusterId) {
        KubernetesClient client = clientMap.get(clusterId);
        if (client == null) {
            throw new RuntimeException("K8s client not found for cluster: " + clusterId);
        }
        return client;
    }

    /**
     * 定时健康检查：每30秒探测一次所有已注册集群
     */
    @Scheduled(fixedDelay = 30000)
    public void healthCheck() {
        for (Map.Entry<Long, KubernetesClient> entry : clientMap.entrySet()) {
            Long clusterId = entry.getKey();
            KubernetesClient client = entry.getValue();
            try {
                client.getVersion(); // 轻量请求
                // 成功：重置失败计数
                AtomicInteger counter = offlineCounter.get(clusterId);
                if (counter != null && counter.get() > 0) {
                    offlineCounter.remove(clusterId);
                    updateClusterStatus(clusterId, "online");
                    log.info("Cluster {} recovered", clusterId);
                }
            } catch (Exception e) {
                AtomicInteger counter = offlineCounter.computeIfAbsent(clusterId, id -> new AtomicInteger(0));
                int fails = counter.incrementAndGet();
                log.warn("Health check failed for cluster {}, consecutive failures: {}", clusterId, fails);
                if (fails >= 3) {
                    // 连续失败3次，标记为 offline
                    updateClusterStatus(clusterId, "offline");
                    // 可选：尝试重新创建客户端
                    Cluster cluster = clusterMapper.selectById(clusterId);
                    if (cluster != null) {
                        registerCluster(cluster);
                    }
                }
            }
        }
    }

    private void updateClusterStatus(Long clusterId, String status) {
        Cluster cluster = new Cluster();
        cluster.setId(clusterId);
        cluster.setStatus(status);
        cluster.setLastHeartbeat(Instant.now());
        clusterMapper.updateById(cluster);
    }

    // ==================== 业务方法 ====================

    public String getServiceStatus(Long clusterId, String namespace, String workloadName, String workloadType) {
        try {
            KubernetesClient client = getClient(clusterId);
            if ("Deployment".equalsIgnoreCase(workloadType)) {
                Deployment deploy = client.apps().deployments().inNamespace(namespace).withName(workloadName).get();
                if (deploy == null) return "not found";
                Integer replicas = deploy.getSpec().getReplicas();
                Integer ready = deploy.getStatus().getReadyReplicas();
                return (ready != null && replicas != null && ready.equals(replicas)) ? "healthy" : "degraded";
            } else if ("StatefulSet".equalsIgnoreCase(workloadType)) {
                StatefulSet sts = client.apps().statefulSets().inNamespace(namespace).withName(workloadName).get();
                if (sts == null) return "not found";
                Integer replicas = sts.getSpec().getReplicas();
                Integer ready = sts.getStatus().getReadyReplicas();
                return (ready != null && replicas != null && ready.equals(replicas)) ? "healthy" : "degraded";
            }
        } catch (Exception e) {
            log.error("Error getting status for {}/{}", namespace, workloadName, e);
            return "error";
        }
        return "unknown";
    }

    public WorkloadDetail getWorkloadDetail(Long clusterId, String namespace, String workloadName, String workloadType) {
        WorkloadDetail detail = new WorkloadDetail();
        try {
            KubernetesClient client = getClient(clusterId);
            List<Pod> pods;
            if ("Deployment".equalsIgnoreCase(workloadType)) {
                Deployment deploy = client.apps().deployments().inNamespace(namespace).withName(workloadName).get();
                if (deploy != null && deploy.getSpec().getTemplate().getSpec().getContainers() != null) {
                    var container = deploy.getSpec().getTemplate().getSpec().getContainers().get(0);
                    detail.startupCommand = String.join(" ", container.getCommand());
                    detail.envVariables = new HashMap<>();
                    if (container.getEnv() != null) {
                        container.getEnv().forEach(env -> detail.envVariables.put(env.getName(), env.getValue()));
                    }
                    if (container.getResources() != null && container.getResources().getRequests() != null) {
                        detail.cpuRequest = String.valueOf(container.getResources().getRequests().get("cpu"));
                        detail.memoryRequest = String.valueOf(container.getResources().getRequests().get("memory"));
                    }
                }
                pods = client.pods().inNamespace(namespace).withLabel("app", workloadName).list().getItems();
            } else {
                StatefulSet sts = client.apps().statefulSets().inNamespace(namespace).withName(workloadName).get();
                if (sts != null && sts.getSpec().getTemplate().getSpec().getContainers() != null) {
                    var container = sts.getSpec().getTemplate().getSpec().getContainers().get(0);
                    detail.startupCommand = String.join(" ", container.getCommand());
                    detail.envVariables = new HashMap<>();
                    if (container.getEnv() != null) {
                        container.getEnv().forEach(env -> detail.envVariables.put(env.getName(), env.getValue()));
                    }
                }
                pods = client.pods().inNamespace(namespace).withLabel("app", workloadName).list().getItems();
            }
            detail.pods = pods.stream().map(pod -> {
                PodInfo info = new PodInfo();
                info.setName(pod.getMetadata().getName());
                info.setStatus(pod.getStatus().getPhase());
                info.setRestartCount(pod.getStatus().getContainerStatuses() != null ? pod.getStatus().getContainerStatuses().get(0).getRestartCount() : 0);
                info.setIp(pod.getStatus().getPodIP());
                return info;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching workload detail", e);
        }
        return detail;
    }

    public int getCurrentReplicas(Long clusterId, String namespace, String workloadName, String workloadType) {
        try {
            KubernetesClient client = getClient(clusterId);
            if ("Deployment".equalsIgnoreCase(workloadType)) {
                Deployment deployment = client.apps().deployments().inNamespace(namespace).withName(workloadName).get();
                if (deployment == null) return 0;
                Integer replicas = deployment.getSpec().getReplicas();
                return replicas != null ? replicas : 0;
            } else if ("StatefulSet".equalsIgnoreCase(workloadType)) {
                StatefulSet sts = client.apps().statefulSets().inNamespace(namespace).withName(workloadName).get();
                if (sts == null) return 0;
                Integer replicas = sts.getSpec().getReplicas();
                return replicas != null ? replicas : 0;
            }
        } catch (Exception e) {
            log.error("Failed to get current replicas", e);
            throw new RuntimeException("Failed to fetch replicas from K8s API", e);
        }
        return 0;
    }

    public void scaleWorkload(Long clusterId, String namespace, String workloadName, String workloadType, int targetReplicas) {
        try {
            KubernetesClient client = getClient(clusterId);
            if ("Deployment".equalsIgnoreCase(workloadType)) {
                client.apps().deployments().inNamespace(namespace).withName(workloadName)
                        .scale(targetReplicas);
            } else if ("StatefulSet".equalsIgnoreCase(workloadType)) {
                client.apps().statefulSets().inNamespace(namespace).withName(workloadName)
                        .scale(targetReplicas);
            } else {
                throw new IllegalArgumentException("Unsupported workload type: " + workloadType);
            }
            log.info("Scaled {} {} to {} replicas", workloadType, workloadName, targetReplicas);
        } catch (Exception e) {
            log.error("Failed to scale workload", e);
            throw new RuntimeException("Scale operation failed", e);
        }
    }

    public double getPodMetricAvg(Long clusterId, String namespace, String workloadName, String metric) {
        // 简化：通过 metrics-server API 获取 Pod 资源使用平均值
        // 实际可使用 client.top().pods() 或直接调用 metrics API
        // 这里返回一个示例值，生产环境需实现
        return 0.0;
    }

    // 内部数据传输类
    @lombok.Data
    public static class WorkloadDetail {
        private String startupCommand;
        private Map<String, String> envVariables;
        private String cpuRequest;
        private String memoryRequest;
        private List<PodInfo> pods = new ArrayList<>();
    }

    @lombok.Data
    public static class PodInfo {
        private String name;
        private String status;
        private int restartCount;
        private String ip;
    }
}