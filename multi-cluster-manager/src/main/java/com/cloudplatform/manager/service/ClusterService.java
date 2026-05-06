package com.cloudplatform.manager.service;

import com.cloudplatform.manager.dto.ClusterRequest;
import com.cloudplatform.manager.dto.ClusterResponse;
import java.util.List;

public interface ClusterService {
    List<ClusterResponse> listClusters();
    ClusterResponse registerCluster(ClusterRequest request);
    void deleteCluster(Long id);
    boolean testHealth(Long id);
    void checkAndUpdateClusterHealth();  // 定时调用
}