package com.cloudplatform.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cloudplatform.manager.model.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 自定义查询方法可在 XML 中定义

    @Select("SELECT id, name, email, password_hash, totp_secret, totp_enabled, created_at\n" +
            "FROM users WHERE id IN\n" +
            "<foreach collection=\"list\" item=\"id\" open=\"(\" separator=\",\" close=\")\">\n" +
            "    #{id}\n" +
            "</foreach>;")
    List<User> findByIds(List<Long> ids);

    @Insert("INSERT INTO users (id, name, email, password_hash, totp_secret, totp_enabled, created_at)\n" +
            "VALUES (#{id}, #{name}, #{email}, #{passwordHash}, #{totpSecret}, #{totpEnabled}, #{createdAt});")
    User save(User user);
}