package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.UserDepartment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserDepartmentMapper extends BaseMapper<UserDepartment> {
    // 自定义查询方法可在 XML 中定义
    @Select("SELECT user_id, department_id, is_primary FROM user_departments WHERE user_id = #{userId};")
    List<UserDepartment> findById_UserId(Long userId);

    @Delete("DELETE FROM user_departments WHERE user_id = #{userId};")
    void deleteById_UserId(Long userId);

    @Insert("INSERT INTO user_departments (user_id, department_id, is_primary)\n" +
            "VALUES (#{userId}, #{departmentId}, #{isPrimary});")
    UserDepartment save(UserDepartment ud);
}