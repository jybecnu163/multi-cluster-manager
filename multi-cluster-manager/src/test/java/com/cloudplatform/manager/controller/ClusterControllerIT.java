package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.base.BaseControllerIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ClusterControllerIT extends BaseControllerIT {

    @Test
    @WithMockUser(roles = {"系统管理员"})
    @DisplayName("注册集群 - 管理员返回201")
    void registerCluster() throws Exception {
        String request = """
                {
                    "name": "test-cluster",
                    "env_type": "dev",
                    "api_endpoint": "https://localhost:6443",
                    "kubeconfig": "dummy"
                }
                """;
        mockMvc.perform(post("/api/v1/clusters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-cluster"));
    }

    @Test
    @WithMockUser(roles = {"运维工程师"})
    @DisplayName("健康检查 - 运维可查看")
    void getHealth() throws Exception {
        mockMvc.perform(get("/api/v1/clusters/1/health"))
                .andExpect(status().isOk());  // 实际可能 offline 但 200 正常
    }

    @Test
    @WithMockUser(roles = {"开发工程师"})
    @DisplayName("开发工程师不能注册集群")
    void devCannotRegister() throws Exception {
        mockMvc.perform(post("/api/v1/clusters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\",\"env_type\":\"dev\",\"api_endpoint\":\"https://example.com\",\"kubeconfig\":\"dummy\"}"))
                .andExpect(status().isForbidden());
    }
}