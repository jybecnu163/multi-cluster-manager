package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.base.BaseControllerIT;
import com.cloudplatform.manager.model.entity.User;
import com.cloudplatform.manager.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerIT extends BaseControllerIT {

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(username = "admin", roles = {"系统管理员"})
    @DisplayName("创建成员返回201")
    void createUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("张三");
        user.setEmail("zhangsan@test.com");
        when(userService.createUser(anyString(), anyString(), anyString(), anyList(), anyLong()))
                .thenReturn(user);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\":\"张三\"," +
                                "\"email\":\"zhangsan@test.com\"," +
                                "\"password\":\"Zhangsan@123\"," +
                                "\"department_ids\":[1,2]," +
                                "\"primary_department_id\":1" +
                                "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("张三"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"系统管理员"})
    @DisplayName("分配角色返回200")
    void assignRole() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role_id\":3,\"env_type\":\"prod\",\"department_id\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dev", roles = {"开发工程师"})
    @DisplayName("普通用户创建成员返回403")
    void devCreateUserForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\",\"email\":\"t@t.com\",\"password\":\"12345678\"," +
                                "\"department_ids\":[1],\"primary_department_id\":1}"))
                .andExpect(status().isForbidden());
    }
}