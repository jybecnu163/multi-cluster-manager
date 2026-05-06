package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.DepartmentSettings;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

@Mapper
public interface DepartmentSettingsMapper extends BaseMapper<DepartmentSettings> {
    @Select("SELECT department_id, allow_ops_bypass_prod_scale, updated_at\n" +
            "FROM department_settings WHERE department_id = #{departmentId};")
    Optional<DepartmentSettings> findById(Long departmentId);

    @Insert("INSERT INTO department_settings (department_id, allow_ops_bypass_prod_scale, updated_at)\n" +
            "VALUES (#{departmentId}, #{allowOpsBypassProdScale}, #{updatedAt})\n" +
            "ON CONFLICT (department_id) DO UPDATE\n" +
            "SET allow_ops_bypass_prod_scale = EXCLUDED.allow_ops_bypass_prod_scale,\n" +
            "    updated_at = EXCLUDED.updated_at;")
    DepartmentSettings save(DepartmentSettings ds);
    // 自定义查询方法可在 XML 中定义
}