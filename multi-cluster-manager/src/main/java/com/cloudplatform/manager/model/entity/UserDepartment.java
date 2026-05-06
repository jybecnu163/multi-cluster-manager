package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_departments")
public class UserDepartment {

    @TableId(type = IdType.AUTO)
    private Long userId;
    private Long departmentId;
    private Boolean isPrimary;
}