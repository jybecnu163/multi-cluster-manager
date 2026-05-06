package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.Pipeline;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PipelineMapper extends BaseMapper<Pipeline> {
    // 自定义查询方法可在 XML 中定义
}