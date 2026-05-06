package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@TableName("companies")
public class Company {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER)
    private Instant createdAt;
    @TableField(value = "updated_at", update = "now()")
    private Instant updatedAt;

}