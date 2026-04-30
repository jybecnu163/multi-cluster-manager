package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clusters")
public class ClusterController extends BaseController {

    @GetMapping
    public ResponseEntity<?> listClusters() {
        return notImplemented();
    }

    @PostMapping
    public ResponseEntity<?> registerCluster(@RequestBody Object request) {
        return notImplemented();
    }

    @GetMapping("/{cluster_id}/health")
    public ResponseEntity<?> getClusterHealth(@PathVariable UUID clusterId) {
        return notImplemented();
    }
}