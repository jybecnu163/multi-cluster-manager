package com.cloudplatform.manager.integration;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NacosClient {

    private final Map<Long, NamingService> namingServices = new ConcurrentHashMap<>();

    @Value("${nacos.server.addr:localhost:8848}")
    private String nacosServerAddr;


    public void registerCluster(Long clusterId, String serverAddr) {
        try {
            NamingService namingService = NacosFactory.createNamingService(serverAddr);
            namingServices.put(clusterId, namingService);
        } catch (Exception e) {
            log.error("Failed to create Nacos naming service for cluster {}", clusterId, e);
        }
    }

    public String getServiceHealth(Long clusterId, String serviceName) {
        if (serviceName == null || serviceName.isEmpty()) return "unknown";
        NamingService namingService = namingServices.get(clusterId);
        if (namingService == null) return "unknown";
        try {
            List<Instance> instances = namingService.getAllInstances(serviceName);
            boolean allHealthy = instances.stream().allMatch(Instance::isHealthy);
            if (instances.isEmpty()) return "unregistered";
            return allHealthy ? "registered" : "degraded";
        } catch (Exception e) {
            log.warn("Failed to get Nacos health for service {}", serviceName, e);
            return "unknown";
        }
    }
}
