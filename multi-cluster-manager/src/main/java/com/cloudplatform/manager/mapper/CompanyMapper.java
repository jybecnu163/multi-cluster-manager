package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.Company;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CompanyMapper extends BaseMapper<Company> {
//    @Select("SELECT id, name, created_at, updated_at FROM companies ORDER BY created_at desc;")
//    List<Company> findAll();

//    @Select("SELECT COUNT(*) > 0 FROM companies WHERE name = #{name};")
//    boolean existsByName(String name);
}