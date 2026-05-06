package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.AutoScalingPolicy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AutoScalingPolicyMapper extends BaseMapper<AutoScalingPolicy> {
//    @Select("SELECT * FROM auto_scaling_policies WHERE enabled = true")
//    List<AutoScalingPolicy> findEnabledPolicies();
}
