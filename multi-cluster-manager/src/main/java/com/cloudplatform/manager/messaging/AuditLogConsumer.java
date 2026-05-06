package com.cloudplatform.manager.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.AuditLogMapper;
import com.cloudplatform.manager.model.entity.AuditLog;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogConsumer {

    private final AuditLogMapper auditLogMapper;
    private final BlockingQueue<AuditLog> buffer = new LinkedBlockingQueue<>(1000);

    // 每100条或每1秒批量写入（使用@Scheduled也可以，这里用RabbitListener批量处理）
    @RabbitListener(queues = "audit.queue", concurrency = "1")
    @Transactional
    public void handleAuditLog(AuditLog auditLog, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws Exception {
        // 计算前一条日志的哈希
//        String lastHash = auditLogMapper.getLatestHash();
        String lastHash = auditLogMapper.selectOne(
                        new LambdaQueryWrapper<AuditLog>()
                                .orderByDesc(AuditLog::getId))
                .getPrevHash();
        auditLog.setPrevHash(lastHash);
        // 插入数据库
        auditLogMapper.insert(auditLog);
        // 手动确认消息
        channel.basicAck(tag, false);
        log.debug("Audit log saved: {}", auditLog.getOperation());
    }

    // 可选：批量消费可进一步提高性能，此处简化，单条确认但使用手动Ack保证可靠
}
