package com.cloudplatform.manager.security;

import com.cloudplatform.manager.base.BaseControllerIT;
import org.junit.jupiter.api.*;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PermissionIntegrationTest extends BaseControllerIT {

    @Test
    @WithMockUser(username = "ops-auditor", roles = {"运维工程师", "审计员"})
    @DisplayName("多角色用户可查看生产环境服务（权限并集）")
    void multiRoleCanViewProd() throws Exception {
        mockMvc.perform(post("/api/v1/services?env_type=prod"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dev", roles = {"开发工程师"})
    @DisplayName("开发工程师不能在生产环境扩缩容")
    void devCannotScaleProd() throws Exception {
        mockMvc.perform(post("/api/v1/services/1/scale")
                .contentType("application/json")
                .content("{\"target_replicas\":2,\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ops", roles = {"运维工程师"})
    @DisplayName("运维工程师生产扩缩容返回202")
    void opsScaleProdAccepted() throws Exception {
        mockMvc.perform(post("/api/v1/services/1/scale")
                .contentType("application/json")
                .content("{\"target_replicas\":3,\"reason\":\"扩容\",\"ignore_approval\":false}"))
                .andExpect(status().isAccepted());
    }
}