package com.cloudplatform.manager.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String email;
    @TableField("password_hash")
    private String passwordHash;
    @TableField("totp_secret")
    private String totpSecret;
    @TableField("totp_enabled")
    private Boolean totpEnabled;
    @TableField("created_at")
    private Instant createdAt;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", totpSecret='" + totpSecret + '\'' +
                ", totpEnabled=" + totpEnabled +
                ", createdAt=" + createdAt +
                '}';
    }
}