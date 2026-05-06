package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

//    @Select("SELECT id, name, env_permission_mask FROM roles ORDER BY id;")
//    List<Role> findAll();
//    // 自定义查询方法可在 XML 中定义
}