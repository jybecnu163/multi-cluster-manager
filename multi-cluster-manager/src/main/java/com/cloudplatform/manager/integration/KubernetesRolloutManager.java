package com.cloudplatform.manager.integration;

import com.cloudplatform.manager.model.entity.ServiceInstance;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;

public interface KubernetesRolloutManager {
    void updateDeploymentBatch(ServiceInstance service, String newImage, int batchNumber, int totalBatches, int batchSize);
    void updateStatefulSetPartition(ServiceInstance service, String newImage, int partition);
    void rollbackDeployment(ServiceInstance service);
    void rollbackStatefulSet(ServiceInstance service);
    int getReadyReplicas(ServiceInstance service);
    int getCurrentPartition(ServiceInstance service);
    boolean isBatchCompleted(ServiceInstance service, int expectedReplicas);
}