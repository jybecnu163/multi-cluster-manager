package com.cloudplatform.manager.integration;

import com.cloudplatform.manager.dto.MetricTimeSeries;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final MeterRegistry meterRegistry;

    @Value("${prometheus.url:http://prometheus:9090}")
    private String prometheusUrl;

    @Value("${metrics-server.enabled:true}")
    private boolean metricsServerEnabled;

    public MetricTimeSeries queryTimeSeries(Long clusterId, String namespace, String workloadName, String metric, String range, String source) {
        if ("prometheus".equalsIgnoreCase(source)) {
            return queryPrometheus(namespace, workloadName, metric, range);
        } else {
            return queryMetricsServer(clusterId, namespace, workloadName, metric);
        }
    }

    private MetricTimeSeries queryPrometheus(String namespace, String workloadName, String metric, String range) {
        String query;
        if ("cpu".equalsIgnoreCase(metric)) {
            query = fmt("sum(rate(container_cpu_usage_seconds_total{" +
                    "namespace=\"%s\",pod=~\"%s-.*\"}[5m]))", namespace, workloadName);
        } else if ("memory".equalsIgnoreCase(metric)) {
            query = fmt("sum(container_memory_working_set_bytes{" +
                    "namespace=\"%s\",pod=~\"%s-.*\"})", namespace, workloadName);
        } else {
            query = metric;
        }
        // 根据range参数计算时间范围
        long rangeSeconds = range.equals("1h") ? 3600 : range.equals("6h") ? 21600 : 86400;
        long start = Instant.now().minusSeconds(rangeSeconds).getEpochSecond();
        long end = Instant.now().getEpochSecond();
        String promQuery = fmt("%s/api/v1/query_range?query=%s&start=%d&end=%d&step=60",
                prometheusUrl, query, start, end);
        try {
            ResponseEntity<PrometheusResponse> response
                    = restTemplate.getForEntity(promQuery, PrometheusResponse.class);
            if (response.getBody() != null && response.getBody().getData() != null) {
                List<String> timestamps = new ArrayList<>();
                List<Double> values = new ArrayList<>();
                for (List<Object> point : response.getBody().getData().getResult().get(0).getValues()) {
                    timestamps.add(Instant.ofEpochSecond(((Number) point.get(0)).longValue()).toString());
                    values.add(((Number) point.get(1)).doubleValue());
                }
                MetricTimeSeries ts = new MetricTimeSeries();
                ts.setTimestamps(timestamps);
                ts.setValues(values);
                return ts;
            }
        } catch (Exception e) {
            log.error("Prometheus query failed", e);
        }
        return new MetricTimeSeries();
    }

    private MetricTimeSeries queryMetricsServer(Long clusterId, String namespace, String workloadName, String metric) {
        // 模拟 metrics-server 数据（可调用 k8s metrics API）
        MetricTimeSeries ts = new MetricTimeSeries();
        ts.setTimestamps(List.of(Instant.now().toString()));
        ts.setValues(List.of(0.0));
        return ts;
    }

    private String fmt(String format, Object... args) {
        return String.format(format, args);
    }

    // 内部类用于解析Prometheus响应
    @lombok.Data
    static class PrometheusResponse {
        private String status;
        private Data data;

        @lombok.Data
        static class Data {
            private List<Result> result;
        }

        @lombok.Data
        static class Result {
            private List<List<Object>> values;
        }
    }
}
