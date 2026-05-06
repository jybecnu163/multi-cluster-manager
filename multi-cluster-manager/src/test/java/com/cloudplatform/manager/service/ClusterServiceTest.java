package com.cloudplatform.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.dto.ClusterRequest;
import com.cloudplatform.manager.dto.ClusterResponse;
import com.cloudplatform.manager.integration.KubernetesClientFactory;
import com.cloudplatform.manager.mapper.ClusterMapper;
import com.cloudplatform.manager.model.entity.Cluster;
import com.cloudplatform.manager.service.impl.ClusterServiceImpl;
import com.cloudplatform.manager.util.EncryptionUtil;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {
    @Mock
    private ClusterMapper clusterRepository;
    @Mock
    private EncryptionUtil encryptionUtil;
    @Mock
    private KubernetesClientFactory clientFactory;
    @Mock
    private KubernetesClient kubernetesClient;
    @InjectMocks
    private ClusterServiceImpl clusterService;

    private ClusterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new ClusterRequest();
        validRequest.setName("prod-cluster");
        validRequest.setEnvType("prod");
        validRequest.setApiEndpoint("https://k8s.example.com:6443");
        validRequest.setKubeconfig("dummy-base64-kubeconfig");
    }

    @Test
    void registerClusterSuccess() {
        when(clusterRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(encryptionUtil.encrypt(anyString())).thenReturn("encrypted");
        when(clusterRepository.insert(any(Cluster.class))).thenReturn(1);
        // Mock testHealth true
        doReturn(true).when(clusterService).testHealth(anyLong());

        ClusterResponse resp = clusterService.registerCluster(validRequest);
        assertNotNull(resp);
        assertEquals("prod-cluster", resp.getName());
        verify(clusterRepository, times(1)).insert(any(Cluster.class));
    }

    @Test
    void registerClusterDuplicateName() {
        when(clusterRepository.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThrows(RuntimeException.class, () -> clusterService.registerCluster(validRequest));
    }

    @Test
    void testHealthOnline() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        when(clusterRepository.selectById(1L)).thenReturn(cluster);
        when(clientFactory.getClient(cluster)).thenReturn(kubernetesClient);
        when(kubernetesClient.getKubernetesVersion())
                .thenReturn(new VersionInfo.Builder().withGitVersion("v1.24").build());
        assertTrue(clusterService.testHealth(1L));
    }

    @Test
    void testHealthOffline() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        when(clusterRepository.selectById(1L)).thenReturn(cluster);
        when(clientFactory.getClient(cluster)).thenThrow(new RuntimeException("Connection refused"));
        assertFalse(clusterService.testHealth(1L));
    }

    @Test
    void deleteClusterSuccess() {
        doNothing().when(clientFactory).evictClient(1L);
        when(clusterRepository.deleteById(1L)).thenReturn(1);
        assertDoesNotThrow(() -> clusterService.deleteCluster(1L));
    }
}