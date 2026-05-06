package com.cloudplatform.manager.integration.impl;

import com.cloudplatform.manager.integration.KubernetesClientFactory;
import com.cloudplatform.manager.integration.KubernetesRolloutManager;
import com.cloudplatform.manager.mapper.ClusterMapper;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.RollingUpdateDeployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KubernetesRolloutManagerImpl implements KubernetesRolloutManager {
    @Autowired
    private KubernetesClientFactory clientFactory;
    @Autowired
    private ClusterMapper clusterMapper;

    @Override
    public void updateDeploymentBatch(ServiceInstance service, String newImage, int batchNumber, int totalBatches, int batchSize) {
        KubernetesClient client = clientFactory.getClient(clusterMapper.selectById(service.getClusterId()));
        Deployment deployment = client.apps().deployments().inNamespace(service.getNamespace()).withName(service.getWorkloadName()).get();
        if (deployment == null) throw new RuntimeException("Deployment not found");

        // 调整 maxSurge 和 maxUnavailable 来控制每批更新的实例数
        // 每批更新 batchSize 个副本 -> 设置 maxSurge = batchSize, maxUnavailable = 0
        // 确保滚动更新只增加新 Pod，不减少旧 Pod
        if (deployment.getSpec().getStrategy().getRollingUpdate() == null) {
            deployment.getSpec().getStrategy().setRollingUpdate(new RollingUpdateDeployment());
        }
        deployment.getSpec().getStrategy().getRollingUpdate().setMaxSurge(new IntOrString(batchSize));
        deployment.getSpec().getStrategy().getRollingUpdate().setMaxUnavailable(new IntOrString(0));
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(newImage);
        client.apps().deployments().inNamespace(service.getNamespace()).createOrReplace(deployment);
    }

    @Override
    public void updateStatefulSetPartition(ServiceInstance service, String newImage, int partition) {
        KubernetesClient client = clientFactory.getClient(clusterMapper.selectById(service.getClusterId()));
        StatefulSet sts = client.apps().statefulSets().inNamespace(service.getNamespace()).withName(service.getWorkloadName()).get();
        sts.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(newImage);
        sts.getSpec().getUpdateStrategy().getRollingUpdate().setPartition(partition);
        client.apps().statefulSets().inNamespace(service.getNamespace()).createOrReplace(sts);
    }

    @Override
    public void rollbackDeployment(ServiceInstance service) {
        // 通过 annotation 记录上一版本镜像，回滚时设置回去
        // 简化：直接获取当前运行镜像的前一个版本（需存储，这里略）
    }

    @Override
    public void rollbackStatefulSet(ServiceInstance service) {
        // 类似
    }

    @Override
    public int getReadyReplicas(ServiceInstance service) {
        KubernetesClient client = clientFactory.getClient(clusterMapper.selectById(service.getClusterId()));
        Deployment deployment = client.apps().deployments().inNamespace(service.getNamespace()).withName(service.getWorkloadName()).get();
        return deployment.getStatus().getReadyReplicas();
    }

    @Override
    public int getCurrentPartition(ServiceInstance service) {
        KubernetesClient client = clientFactory.getClient(clusterMapper.selectById(service.getClusterId()));
        StatefulSet sts = client.apps().statefulSets().inNamespace(service.getNamespace()).withName(service.getWorkloadName()).get();
        return sts.getSpec().getUpdateStrategy().getRollingUpdate().getPartition();
    }

    @Override
    public boolean isBatchCompleted(ServiceInstance service, int expectedReplicas) {
        // 检查所有 Pod 是否已更新（通过镜像 Version）
        // 简化：检查 readyReplicas >= expectedReplicas
        return getReadyReplicas(service) >= expectedReplicas;
    }
}