package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.base.BaseControllerIT;
import com.cloudplatform.manager.service.RoleService;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleControllerIT extends BaseControllerIT {

    @MockBean
    private RoleService roleService;

    @Test
    @WithMockUser(username = "admin", roles = {"系统管理员"})
    @DisplayName("系统管理员获取角色列表")
    void adminCanGetRoles() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dev", roles = {"开发工程师"})
    @DisplayName("非管理员获取角色列表返回403")
    void nonAdminCannotGetRoles() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isForbidden());
    }
}