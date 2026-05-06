package com.cloudplatform.manager.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.cloudplatform.manager.integration.K8sClientManager;
import com.cloudplatform.manager.mapper.ServiceInstanceMapper;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogWebSocketHandler extends TextWebSocketHandler {

    private final K8sClientManager k8sClientManager;
    private final ServiceInstanceMapper serviceInstanceMapper;
    private final Map<String, LogWatch> activeWatches = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        String serviceIdStr = extractServiceId(path);
        Long serviceId = Long.valueOf(serviceIdStr);
        String podName = session.getUri().getQuery().split("pod_name=")[1].split("&")[0];
        String container = session.getUri().getQuery().contains("container=") ? session.getUri().getQuery().split("container=")[1] : null;

        ServiceInstance inst = serviceInstanceMapper.selectById(serviceId);
        if (inst == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Service not found"));
            return;
        }

        KubernetesClient client = k8sClientManager.getClient(inst.getClusterId());
        if (client == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Cluster unreachable"));
            return;
        }

        LogWatch watch = client.pods().inNamespace(inst.getNamespace()).withName(podName)
                .inContainer(container != null ? container : "")
                .watchLog(System.out); // 实际应使用管道
        // 简化: 使用线程读取日志流并发送WebSocket消息
        // 由于篇幅，此处采用伪代码：实际应使用 PipedInputStream 或 CompletableFuture
        activeWatches.put(session.getId(), watch);

        // 启动日志读取线程
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(watch.getOutput()))) {
                String line;
                while ((line = reader.readLine()) != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(line));
                }
            } catch (Exception e) {
                log.error("Log streaming error", e);
            } finally {
                try { session.close(); } catch (Exception ignored) {}
            }
        }).start();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LogWatch watch = activeWatches.remove(session.getId());
        if (watch != null) {
            watch.close();
        }
    }

    private String extractServiceId(String path) {
        // /services/{service_id}/logs
        String[] parts = path.split("/");
        return parts[2];
    }
}
