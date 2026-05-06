package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.UserRole;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    // 自定义查询方法可在 XML 中定义
    @Select("SELECT user_id, role_id, env_type, department_id FROM user_roles WHERE user_id = #{userId};")
    List<UserRole> findById_UserId(Long userId);

    @Select("SELECT ur FROM UserRole ur WHERE ur.id.userId = :userId AND (ur.envType = 'all' OR ur.envType = :envType)")
    List<UserRole> findByUserIdAndEnv(@Param("userId") Long userId, @Param("envType") String envType);

    @Insert("INSERT INTO user_roles (user_id, role_id, env_type, department_id)\n" +
            "VALUES (#{userId}, #{roleId}, #{envType}, #{departmentId});")
    UserRole save(UserRole ur);
}