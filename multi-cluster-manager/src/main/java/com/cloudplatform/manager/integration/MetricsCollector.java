package com.cloudplatform.manager.integration;

import com.cloudplatform.manager.model.entity.AutoScalingPolicy;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsCollector {
    @Value("${prometheus.url:http://prometheus:9090}")
    private String prometheusUrl;
    private final RestTemplate restTemplate = new RestTemplate();
    private final K8sClientManager k8sClientManager;
    private final NacosClient nacosClient;


    public double fetchMetric(AutoScalingPolicy policy, ServiceInstance inst) {
        String metricType = policy.getMetricType();
        String source = policy.getFallbackSource();

        try {
            if ("cpu".equals(metricType) || "memory".equals(metricType)) {
                if ("prometheus".equalsIgnoreCase(source)) {
                    return queryPrometheus(inst, metricType);
                } else if ("metrics_server".equalsIgnoreCase(source)) {
                    return queryMetricsServer(inst, metricType);
                }
            } else if ("qps".equals(metricType)) {
                // 优先从 Nacos 获取 QPS（需要 Nacos 支持统计），否则从 Prometheus 获取 Ingress QPS
                return getQpsFromNacos(inst);
            } else if ("custom".equals(metricType)) {
                return queryPrometheusCustom(policy.getMetricQuery());
            }
        } catch (Exception e) {
            log.error("Metric fetch failed, fallback to default threshold", e);
            // 降级返回目标阈值的一半，避免误触发扩容
            return policy.getTargetThreshold() * 0.5;
        }
        return 0.0;
    }

    private double queryPrometheus(ServiceInstance inst, String metric) {
        String query;
        if ("cpu".equals(metric)) {
            query = String.format("sum(rate(container_cpu_usage_seconds_total{namespace=\"%s\",pod=~\"%s-.*\"}[2m]))",
                    inst.getNamespace(), inst.getWorkloadName());
        } else {
            query = String.format("sum(container_memory_working_set_bytes{namespace=\"%s\",pod=~\"%s-.*\"})",
                    inst.getNamespace(), inst.getWorkloadName());
        }
        String url = prometheusUrl + "/api/v1/query?query=" + query;
        ResponseEntity<PrometheusQueryResponse> response = restTemplate.getForEntity(url, PrometheusQueryResponse.class);
        if (response.getBody() != null && response.getBody().getData() != null && !response.getBody().getData().getResult().isEmpty()) {
            String valueStr = response.getBody().getData().getResult().get(0).getValue().getValue().get(1);
            return Double.parseDouble(valueStr);
        }
        return 0.0;
    }

    private double queryMetricsServer(ServiceInstance inst, String metric) {
        // 调用 K8s metrics API 获取 Pod 实际使用量
        // 简化实现：返回平均值
        return k8sClientManager.getPodMetricAvg(
                inst.getClusterId(), inst.getNamespace(), inst.getWorkloadName(), metric);
    }

    private double getQpsFromNacos(ServiceInstance inst) {
        // 从 Nacos 获取应用的 QPS（若 Nacos 集成了监控） todo
        // 否则返回 0
        return 0.0;
    }

    private double queryPrometheusCustom(String query) {
        // 执行自定义 PromQL
        return 0.0;
    }

    // 内部类用于 Prometheus 响应
    @lombok.Data
    static class PrometheusQueryResponse {
        private String status;
        private Data data;

        @lombok.Data
        static class Data {
            private List<Result> result;
        }

        @lombok.Data
        static class Result {
            private Value value;

            @lombok.Data
            static class Value {
                private List<String> value;
            }

            public Value getValue() {
                return value;
            }

            public void setValue(Value value) {
                this.value = value;
            }
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
