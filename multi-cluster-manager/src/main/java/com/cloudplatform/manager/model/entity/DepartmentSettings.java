package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@TableName("department_settings")
public class DepartmentSettings {
    @TableId
    private Long departmentId;
    private Boolean allowOpsBypassProdScale;
    private Instant updatedAt;

   }