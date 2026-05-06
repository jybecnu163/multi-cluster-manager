package com.cloudplatform.manager.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.UserRoleMapper;
import com.cloudplatform.manager.model.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {
    @Autowired private UserRoleMapper userRoleMapper;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // 暂不实现
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        // targetId 可以为服务ID或部门ID，这里简化：根据用户ID和操作 permission 字符串判断
        // 实际应查询服务归属部门、环境，然后检查 role 表。
        // 具体逻辑根据业务实现，此处示例：允许系统管理员所有操作
        CurrentUserDetails user = (CurrentUserDetails) authentication.getPrincipal();
        // 查询用户角色
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getUserId, user.getUserId());
        List<UserRole> roles = userRoleMapper.selectList(wrapper);
        // 检查是否有系统管理员角色（role_id = 1）
        boolean isAdmin = roles.stream().anyMatch(r -> r.getRoleId() == 1);
        if (isAdmin) return true;
        // 其他逻辑根据 permission 和 targetType 实现
        // 此处返回 false 安全
        return false;
    }
}