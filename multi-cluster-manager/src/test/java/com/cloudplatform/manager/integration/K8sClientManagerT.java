package com.cloudplatform.manager.integration;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static reactor.core.publisher.Mono.when;

public class K8sClientManagerT {

    @Test
    void testGetCurrentReplicas_Deployment() {
//        KubernetesClient client;
//
//        Deployment deployment = mock(Deployment.class);
//        DeploymentSpec spec = mock(DeploymentSpec.class);
//        when((Publisher<?>) deployment.getSpec()).thenReturn(spec);
//        when(spec.getReplicas()).thenReturn(5);
//        when(client.apps().deployments().inNamespace("default").withName("test").get()).thenReturn(deployment);
//
//        int replicas = k8sClientManager.getCurrentReplicas(1L, "default", "test", "Deployment");
//        assertEquals(5, replicas);
    }
}
