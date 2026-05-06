package com.cloudplatform.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudplatform.manager.dto.MetricTimeSeries;
import com.cloudplatform.manager.dto.ServiceDetailResponse;
import com.cloudplatform.manager.dto.ServiceInstanceDto;
import com.cloudplatform.manager.dto.ServiceListResponse;
import com.cloudplatform.manager.exception.BusinessException;
import com.cloudplatform.manager.integration.K8sClientManager;
import com.cloudplatform.manager.integration.MetricsClient;
import com.cloudplatform.manager.integration.NacosClient;
import com.cloudplatform.manager.mapper.ServiceInstanceMapper;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import com.cloudplatform.manager.service.ServiceInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceInstanceServiceImpl implements ServiceInstanceService {

    private final ServiceInstanceMapper serviceInstanceMapper;
    private final K8sClientManager k8sClientManager;
    private final MetricsClient metricsClient;
    private final NacosClient nacosClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "service:list:";

    @Override
    public ServiceListResponse listServices(Long departmentId, String envType, String name, int page, int pageSize) {
        // 获取当前认证用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("未认证", 401);
        }
        // 判断是否为系统管理员（检查是否有 ROLE_系统管理员 或 authority '系统管理员'）
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_系统管理员") || granted.getAuthority().equals("系统管理员"));


        if (isAdmin) {
            LambdaQueryWrapper<ServiceInstance> wrapper = new LambdaQueryWrapper<>();
            // 分页
            Page<ServiceInstance> pageParam = new Page<>(page, pageSize);
            Page<ServiceInstance> pageResult = serviceInstanceMapper.selectPage(pageParam, wrapper);

            return new ServiceListResponse(getServiceInstanceDtoList(pageResult),
                    pageResult.getTotal(), page, pageSize);
        }
        
        // 构造缓存key
        String cacheKey = CACHE_KEY_PREFIX + departmentId + ":" + envType + ":" + name + ":" + page + ":" + pageSize;
        ServiceListResponse cached = (ServiceListResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<ServiceInstance> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null) {
            wrapper.eq(ServiceInstance::getDepartmentId, departmentId);
        }
        if (envType != null) {
            wrapper.eq(ServiceInstance::getEnvType, envType);
        }
        if (name != null) {
            wrapper.like(ServiceInstance::getName, name);
        }
        Page<ServiceInstance> mpPage = new Page<>(page, pageSize);
        Page<ServiceInstance> result = serviceInstanceMapper.selectPage(mpPage, wrapper);

        List<ServiceInstanceDto> dtoList = getServiceInstanceDtoList(result);

        ServiceListResponse response = new ServiceListResponse(dtoList, result.getTotal(), page, pageSize);
        // 缓存5秒
        redisTemplate.opsForValue().set(cacheKey, response, Duration.ofSeconds(5));
        return response;
    }

    private List<ServiceInstanceDto> getServiceInstanceDtoList(Page<ServiceInstance> result) {
        return result.getRecords().stream().map(inst -> {
            ServiceInstanceDto dto = new ServiceInstanceDto();
            // 复制基础字段
            dto.setId(inst.getId());
            dto.setName(inst.getName());
            dto.setDepartmentId(inst.getDepartmentId());
            dto.setClusterId(inst.getClusterId());
            dto.setNamespace(inst.getNamespace());
            dto.setWorkloadType(inst.getWorkloadType());
            dto.setWorkloadName(inst.getWorkloadName());
            dto.setReplicas(inst.getReplicas());
            dto.setNacosServiceName(inst.getNacosServiceName());
            // 实时状态从 K8s 获取（带缓存，5秒）
            String k8sStatus = k8sClientManager.getServiceStatus(inst.getClusterId(), inst.getNamespace(), inst.getWorkloadName(), inst.getWorkloadType());
            dto.setCurrentStatus(k8sStatus);
            // Nacos 健康状态从 Redis 或最新拉取
            String nacosHealth = nacosClient.getServiceHealth(inst.getClusterId(), inst.getNacosServiceName());
            dto.setNacosHealthStatus(nacosHealth != null ? nacosHealth : "unknown");
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public ServiceDetailResponse getServiceDetail(Long serviceId) {
        ServiceInstance inst = serviceInstanceMapper.selectById(serviceId);
        if (inst == null) {
            throw new RuntimeException("Service not found");
        }
        ServiceDetailResponse detail = new ServiceDetailResponse();
        // 拷贝基础信息
        detail.setId(inst.getId());
        detail.setName(inst.getName());
        detail.setDepartmentId(inst.getDepartmentId());
        detail.setClusterId(inst.getClusterId());
        detail.setNamespace(inst.getNamespace());
        detail.setWorkloadType(inst.getWorkloadType());
        detail.setWorkloadName(inst.getWorkloadName());
        detail.setReplicas(inst.getReplicas());
        detail.setNacosServiceName(inst.getNacosServiceName());
        detail.setNacosHealthStatus(nacosClient.getServiceHealth(inst.getClusterId(), inst.getNacosServiceName()));

        // 从K8s获取详细信息
        var k8sDetail = k8sClientManager.getWorkloadDetail(inst.getClusterId(), inst.getNamespace(), inst.getWorkloadName(), inst.getWorkloadType());
        detail.setStartupCommand(k8sDetail.getStartupCommand());
        detail.setEnvVariables(k8sDetail.getEnvVariables());
        detail.setCpuRequest(k8sDetail.getCpuRequest());
        detail.setMemoryRequest(k8sDetail.getMemoryRequest());
        detail.setPods(k8sDetail.getPods().stream().map(p -> {
            ServiceDetailResponse.PodInfo podInfo = new ServiceDetailResponse.PodInfo();
            podInfo.setName(p.getName());
            podInfo.setStatus(p.getStatus());
            podInfo.setRestartCount(p.getRestartCount());
            podInfo.setIp(p.getIp());
            return podInfo;
        }).collect(Collectors.toList()));
        return detail;
    }

    @Override
    public MetricTimeSeries getMetrics(Long serviceId, String metric, String range) {
        ServiceInstance inst = serviceInstanceMapper.selectById(serviceId);
        if (inst == null) {
            throw new RuntimeException("Service not found");
        }
        // 根据环境类型确定指标来源（Prometheus 或 metrics-server）
        String source = "prometheus";
        if ("dev".equals(inst.getEnvType())) {
            // 可配置降级
            source = "metrics-server";
        }
        return metricsClient.queryTimeSeries(inst.getClusterId(), inst.getNamespace(), inst.getWorkloadName(), metric, range, source);
    }
}
