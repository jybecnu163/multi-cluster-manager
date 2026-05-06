package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.ServiceInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

@Mapper
public interface ServiceInstanceMapper extends BaseMapper<ServiceInstance> {
    // 自定义查询方法可在 XML 中定义
    @Select("SELECT COUNT(*) FROM service_instances si JOIN departments d ON si.department_id = d.id WHERE d.company_id = #{companyId}")
    long countByCompanyId(@Param("companyId") Long companyId);
}