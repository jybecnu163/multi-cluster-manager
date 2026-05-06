package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.base.BaseControllerIT;
import com.cloudplatform.manager.model.entity.Company;
import com.cloudplatform.manager.service.CompanyService;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CompanyControllerIT extends BaseControllerIT {

    @MockBean
    private CompanyService companyService;

    @Test
    @WithMockUser(username = "admin", roles = {"系统管理员"})
    @DisplayName("创建公司 - 管理员返回201")
    void createCompanyAsAdmin() throws Exception {
        Company company = new Company();
        company.setId(1L);
        company.setName("测试公司");
        company.setCreatedAt(Instant.now());
        when(companyService.createCompany(anyString())).thenReturn(company);

        mockMvc.perform(post("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试公司\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("测试公司"))
                .andExpect(jsonPath("$.created_at").exists()); // 注意下划线
    }

    @Test
    @WithMockUser(username = "dev", roles = {"开发工程师"})
    @DisplayName("普通用户创建公司返回403")
    void createCompanyForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未认证访问返回401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"系统管理员"})
    @DisplayName("获取公司列表返回200")
    void listCompanies() throws Exception {
        when(companyService.listCompanies()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isOk());
    }
}