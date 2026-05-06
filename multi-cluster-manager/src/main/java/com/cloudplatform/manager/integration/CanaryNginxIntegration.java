package com.cloudplatform.manager.integration;

public interface CanaryNginxIntegration {
    void setCanaryWeight(String namespace, String ingressName, String canaryServiceName, int weight);
    void removeCanaryAnnotations(String namespace, String ingressName);
}