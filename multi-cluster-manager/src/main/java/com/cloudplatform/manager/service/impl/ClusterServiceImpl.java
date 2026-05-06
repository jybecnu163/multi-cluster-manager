package com.cloudplatform.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.dto.ClusterRequest;
import com.cloudplatform.manager.dto.ClusterResponse;
import com.cloudplatform.manager.exception.BusinessException;
import com.cloudplatform.manager.integration.KubernetesClientFactory;
import com.cloudplatform.manager.mapper.ClusterMapper;
import com.cloudplatform.manager.model.entity.Cluster;
import com.cloudplatform.manager.service.ClusterService;
import com.cloudplatform.manager.util.EncryptionUtil;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClusterServiceImpl implements ClusterService {
    @Autowired
    private ClusterMapper clusterRepository;
    @Autowired
    private EncryptionUtil encryptionUtil;
    @Autowired
    private KubernetesClientFactory clientFactory;

    @Override
    public List<ClusterResponse> listClusters() {
        List<Cluster> clusters = clusterRepository.selectList(null);
        return clusters.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClusterResponse registerCluster(ClusterRequest request) {
        // 1. 检查名称唯一性
        LambdaQueryWrapper<Cluster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cluster::getName, request.getName());
        if (clusterRepository.selectCount(wrapper) > 0) {
            throw new BusinessException("Cluster name already exists", 409);
        }

        // 2. 解码 kubeconfig（从 Base64 转为原始字符串）
        String originalKubeconfig;
        try {
            byte[] decoded = Base64.getDecoder().decode(request.getKubeconfig());
            originalKubeconfig = new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid base64 kubeconfig", 400);
        }

        // 3. 验证原始 kubeconfig 格式是否合法（可选，调用 fabric8 尝试解析）
        try {
            Config.fromKubeconfig(originalKubeconfig);
        } catch (Exception e) {
            throw new BusinessException("Invalid kubeconfig format", 400);
        }

        Cluster cluster = new Cluster();
        cluster.setName(request.getName());
        cluster.setEnvType(request.getEnvType());
        cluster.setApiEndpoint(request.getApiEndpoint());
        // // 4. 加密存储原始 kubeconfig 内容
        cluster.setKubeconfigEncrypted(encryptionUtil.encrypt(originalKubeconfig));
//        cluster.setKubeconfigEncrypted(encryptionUtil.encrypt(request.getKubeconfig()));

        if (request.getCaCert() != null) {
            cluster.setCaCertEncrypted(encryptionUtil.encrypt(request.getCaCert()));
        }
        if (request.getToken() != null) {
            cluster.setTokenEncrypted(encryptionUtil.encrypt(request.getToken()));
        }

        // 5. 其他证书同理（如有 caCert, token，也需要先解码再加密）
        if (request.getCaCert() != null) {
            String originalCa = new String(Base64.getDecoder().decode(request.getCaCert()), StandardCharsets.UTF_8);
            cluster.setCaCertEncrypted(encryptionUtil.encrypt(originalCa));
        }
        if (request.getToken() != null) {
            String originalToken = new String(Base64.getDecoder().decode(request.getToken()), StandardCharsets.UTF_8);
            cluster.setTokenEncrypted(encryptionUtil.encrypt(originalToken));
        }

        cluster.setStatus("offline"); // 初始 offline，待首次健康检查
        cluster.setCreatedAt(Instant.now());
        clusterRepository.insert(cluster);

        // 6. 注册后立即测试连通性
        boolean ok = testHealth(cluster.getId());
        if (ok) {
            cluster.setStatus("online");
            cluster.setLastHeartbeat(Instant.now());
            clusterRepository.updateById(cluster);
        }
        return toResponse(cluster);
    }

    @Override
    @Transactional
    public void deleteCluster(Long id) {
        // 检查是否有服务实例依赖（可选，但 PRD 未强制，可允许删除，但需清理）
        // 先清除缓存中的 client
        clientFactory.evictClient(id);
        clusterRepository.deleteById(id);
    }

    @Override
    public boolean testHealth(Long id) {
        Cluster cluster = clusterRepository.selectById(id);
        if (cluster == null) throw new BusinessException("Cluster not found", 404);
        try {
            KubernetesClient client = clientFactory.getClient(cluster);
            // 调用 version() 检查连通性
            client.getKubernetesVersion();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void checkAndUpdateClusterHealth() {
        List<Cluster> clusters = clusterRepository.selectList(null);
        for (Cluster cluster : clusters) {
            boolean reachable = testHealth(cluster.getId());
            String newStatus = reachable ? "online" : "offline";
            if (!newStatus.equals(cluster.getStatus())) {
                cluster.setStatus(newStatus);
                if (reachable) {
                    cluster.setLastHeartbeat(Instant.now());
                }
                clusterRepository.updateById(cluster);
            }
        }
    }

    private ClusterResponse toResponse(Cluster cluster) {
        ClusterResponse resp = new ClusterResponse();
        resp.setId(cluster.getId());
        resp.setName(cluster.getName());
        resp.setEnvType(cluster.getEnvType());
        resp.setApiEndpoint(cluster.getApiEndpoint());
        resp.setStatus(cluster.getStatus());
        resp.setLastHeartbeat(cluster.getLastHeartbeat());
        resp.setCreatedAt(cluster.getCreatedAt());
        return resp;
    }
}