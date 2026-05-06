package com.cloudplatform.manager.service;

import com.cloudplatform.manager.dto.ServiceListResponse;
import com.cloudplatform.manager.dto.ServiceDetailResponse;
import com.cloudplatform.manager.dto.MetricTimeSeries;

public interface ServiceInstanceService {
    ServiceListResponse listServices(Long departmentId, String envType, String name, int page, int pageSize);
    ServiceDetailResponse getServiceDetail(Long serviceId);
    MetricTimeSeries getMetrics(Long serviceId, String metric, String range);
}
