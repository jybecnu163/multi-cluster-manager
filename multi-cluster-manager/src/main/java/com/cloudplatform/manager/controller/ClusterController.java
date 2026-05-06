package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.ClusterRequest;
import com.cloudplatform.manager.dto.ClusterResponse;
import com.cloudplatform.manager.service.ClusterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clusters")
public class ClusterController extends BaseController{
    @Autowired private ClusterService clusterService;

    @GetMapping
    @PreAuthorize("hasRole('系统管理员') or hasRole('审计员')")
    public ResponseEntity<List<ClusterResponse>> listClusters() {
        return ResponseEntity.ok(clusterService.listClusters());
    }

    @PostMapping
    @PreAuthorize("hasRole('系统管理员')")
    public ResponseEntity<ClusterResponse> registerCluster(@Valid @RequestBody ClusterRequest request) {
        ClusterResponse response = clusterService.registerCluster(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{cluster_id}")
    @PreAuthorize("hasRole('系统管理员')")
    public ResponseEntity<Void> deleteCluster(@PathVariable("cluster_id") Long clusterId) {
        clusterService.deleteCluster(clusterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{cluster_id}/health")
    @PreAuthorize("hasRole('系统管理员') or hasRole('运维工程师')")
    public ResponseEntity<Object> getClusterHealth(@PathVariable("cluster_id") Long clusterId) {
        boolean healthy = clusterService.testHealth(clusterId);
        if (healthy) {
            return ResponseEntity.ok(java.util.Map.of("status", "online"));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(java.util.Map.of("status", "offline"));
        }
    }
}