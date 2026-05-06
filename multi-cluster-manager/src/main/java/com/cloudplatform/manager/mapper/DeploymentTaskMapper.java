package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.DeploymentTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeploymentTaskMapper extends BaseMapper<DeploymentTask> {
    // 自定义查询方法可在 XML 中定义
}