package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("service_instances")
public class ServiceInstance {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long departmentId;
    private Long clusterId;
    private String namespace;
    private String workloadType;
    private String workloadName;
    private Integer replicas;
    private String nacosServiceName;
    private String envType;
}