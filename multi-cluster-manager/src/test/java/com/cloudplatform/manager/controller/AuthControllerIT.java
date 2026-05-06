package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.base.BaseControllerIT;
import com.cloudplatform.manager.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerIT extends BaseControllerIT {

    @MockBean
    private AuthService authService;

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {
        @Test
        @DisplayName("正确凭据返回200及JWT token")
        void loginSuccess() throws Exception {
            when(authService.login(anyString(), anyString())).thenReturn("valid-jwt");
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"admin@test.com\", \"password\":\"Admin@123456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.access_token").value("valid-jwt"))
                    .andExpect(jsonPath("$.token_type").value("Bearer"))
                    .andExpect(jsonPath("$.expires_in").isNumber());
        }

        @Test
        @DisplayName("缺少email字段返回400")
        void missingEmail() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"password\":\"12345678\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("无效邮箱格式返回400")
        void invalidEmailFormat() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not - an - email\",\"password\":\"12345678\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {
        @Test
        @DisplayName("携带有效token返回204")
        void logoutWithToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("无token返回401")
        void logoutWithoutToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }
}