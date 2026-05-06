package com.cloudplatform.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ServiceListResponse {
    private List<ServiceInstanceDto> items;
    private long total;
    private int page;
    private int pageSize;
}
