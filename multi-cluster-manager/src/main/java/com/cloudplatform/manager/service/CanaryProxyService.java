package com.cloudplatform.manager.service;

import java.net.URL;

public interface CanaryProxyService {
    URL getInternalEndpoint(Long taskId);
    void forwardToCanaryPod(Long taskId, String path);
}