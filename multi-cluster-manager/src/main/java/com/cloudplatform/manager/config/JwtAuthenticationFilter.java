package com.cloudplatform.manager.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.RoleMapper;
import com.cloudplatform.manager.mapper.UserRoleMapper;
import com.cloudplatform.manager.model.entity.Role;
import com.cloudplatform.manager.model.entity.UserRole;
import com.cloudplatform.manager.security.CurrentUserDetails;
import com.cloudplatform.manager.util.JwtUtil;
import com.cloudplatform.manager.util.RedisUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (redisUtil.isBlacklisted(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revoked");
                return;
            }
            try {
                Claims claims = jwtUtil.parseToken(token);
                Long userId = Long.valueOf(claims.getSubject());
                String email = claims.get("email", String.class);
                // 在 JwtAuthenticationFilter 中，当解析 token 成功后
                CurrentUserDetails userDetails = new CurrentUserDetails(userId, email);
// 从数据库查询用户角色
                List<GrantedAuthority> authorities = loadUserAuthorities(userId);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);  // 注意第三个参数为 authorities
                SecurityContextHolder.getContext().setAuthentication(auth);

                request.setAttribute("userId", userId);
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private List<GrantedAuthority> loadUserAuthorities(Long userId) {
        // 查询用户角色关联
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) return Collections.emptyList();

        // 查询角色名称
        List<Short> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        Map<Short, String> roleNameMap = roles.stream().collect(Collectors.toMap(Role::getId, Role::getName));

        // 构造 GrantedAuthority，如果使用 hasRole 则加 ROLE_ 前缀
        return userRoles.stream()
                .map(ur -> new SimpleGrantedAuthority("ROLE_" + roleNameMap.get(ur.getRoleId())))
                .collect(Collectors.toList());
    }
}
