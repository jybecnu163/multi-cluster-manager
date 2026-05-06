package com.cloudplatform.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.exception.BusinessException;
import com.cloudplatform.manager.mapper.UserMapper;
import com.cloudplatform.manager.model.entity.User;
import com.cloudplatform.manager.service.AuditService;
import com.cloudplatform.manager.service.AuthService;
import com.cloudplatform.manager.util.JwtUtil;
import com.cloudplatform.manager.util.PasswordEncoder;
import com.cloudplatform.manager.util.RedisUtil;
import com.cloudplatform.manager.util.TotpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserMapper userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private TotpUtil totpUtil;
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;
    @Autowired
    private AuditService auditService;

    @Override
    public String login(String email, String password) {
        try {
            User user = userRepository.selectList(new LambdaQueryWrapper<User>().eq(User::getEmail, email)).getFirst();
            if (null == user) {
                throw new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED.value());
            }
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED.value());
            }

            String token = jwtUtil.generateToken(user.getId(), user.getEmail());

            // 记录登录成功审计日志
            auditService.log("LOGIN_SUCCESS", "User", user.getId(), Map.of("email", email));

            return token;
        } catch (BusinessException e) {
            // 记录登录失败审计日志
            auditService.log("LOGIN_FAILED", "User", null, Map.of("email", email, "reason", e.getMessage()));
            throw e;
        }
    }

    @Override
    public void logout(String token) {
        long ttl = jwtUtil.parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            redisUtil.setBlacklist(token, ttl);
        }
    }

    @Override
    @Transactional
    public String setupTotp(Long userId) {
        User user = userRepository.selectList(
                new LambdaQueryWrapper<User>().eq(User::getId, userId)).getFirst();
        if (null == user) {
            throw new BusinessException("User not found");
        }
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
        String secret = totpUtil.generateSecret();
        user.setTotpSecret(secret);
        user.setTotpEnabled(true);
        userRepository.insert(user);
//        userRepository.save(user);
        return totpUtil.getProvisioningUri(secret, user.getEmail(), "MultiClusterManager");
    }

    @Override
    public boolean verifyTotp(Long userId, int code) {
        User user = userRepository.selectList(
                new LambdaQueryWrapper<User>().eq(User::getId, userId)).getFirst();
        if (null == user) {
            throw new BusinessException("User not found");
        }
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.getTotpEnabled()) return true;
        return totpUtil.verifyCode(user.getTotpSecret(), code);
    }
}
