package com.cloudplatform.manager.dto;

import lombok.Data;

@Data
public class WebhookPayload {
    private String event;
    private Object data;
    private String signature;   // 实际在 header 中
}