package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.base.BaseControllerIT;
import com.cloudplatform.manager.model.entity.Department;
import com.cloudplatform.manager.model.entity.DepartmentSettings;
import com.cloudplatform.manager.service.DepartmentService;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class DepartmentControllerIT extends BaseControllerIT {

    @MockBean
    private DepartmentService departmentService;

    @Test
    @WithMockUser(username = "admin", roles = {"系统管理员"})
    @DisplayName("创建部门返回201")
    void createDepartment() throws Exception {
        Department dept = new Department();
        dept.setId(1L);
        dept.setName("研发部");
        when(departmentService.createDepartment(anyLong(), anyString(), any())).thenReturn(dept);

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"company_id\":1,\"name\":\"研发部\"}")) // 下划线
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("研发部"));
    }

    @Test
    @WithMockUser(username = "director", roles = {"部门主管"})
    @DisplayName("获取部门设置返回200")
    void getSettings() throws Exception {
        DepartmentSettings settings = new DepartmentSettings();
        settings.setDepartmentId(1L);
        settings.setAllowOpsBypassProdScale(false);
        when(departmentService.getSettings(1L)).thenReturn(settings);

        mockMvc.perform(get("/api/v1/departments/1/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allow_ops_bypass_prod_scale").value(false)); // 下划线
    }

    @Test
    @WithMockUser(username = "dev", roles = {"开发工程师"})
    @DisplayName("普通用户修改部门设置返回403")
    void devCannotUpdateSettings() throws Exception {
        mockMvc.perform(patch("/api/v1/departments/1/settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"allow_ops_bypass_prod_scale\":true}"))
                .andExpect(status().isForbidden());
    }
}