package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("pipeline_runs")
public class PipelineRun {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pipelineId;
    private String status;
    private String inputParams;
    private Boolean approvalNeeded;
    // 审批人编号
    private Long approvedBy;
    private Instant startedAt;
    private Instant finishedAt;
    private String executionLog;   // 可选，执行日志;
}