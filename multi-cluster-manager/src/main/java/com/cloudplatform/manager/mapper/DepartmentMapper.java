package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.Department;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
    @Insert("INSERT INTO departments (id, company_id, name, director_user_id)\n" +
            "VALUES (#{id}, #{companyId}, #{name}, #{directorUserId});")
    Department save(Department dept);

    // 自定义查询方法可在 XML 中定义
//    @Select("SELECT id, company_id, name, director_user_id FROM departments WHERE company_id = #{companyId};")
//    List<Department> findByCompanyId(Long companyId);

//    @Select("SELECT COUNT(*) > 0 FROM departments WHERE company_id = #{companyId} AND name = #{name};")
//    boolean existsByCompanyIdAndName(Long companyId, String name);

//    @Select("SELECT COUNT(*) FROM departments WHERE company_id = #{companyId};")
//    long countByCompanyId(Long companyId);

//    @Select("SELECT id, company_id, name, director_user_id FROM departments ORDER BY company_id, name;")
//    List<Department> findAll();
}