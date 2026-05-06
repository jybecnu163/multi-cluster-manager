package com.cloudplatform.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.UserDepartmentMapper;
import com.cloudplatform.manager.mapper.UserMapper;
import com.cloudplatform.manager.model.entity.User;
import com.cloudplatform.manager.model.entity.UserDepartment;
import com.cloudplatform.manager.service.UserService;
import com.cloudplatform.manager.util.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userRepository;
    @Autowired
    private UserDepartmentMapper userDepartmentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<User> listUsers(Long departmentId) {
        if (departmentId == null) {
            return userRepository.selectList(null);
        } else {
            return userRepository.findByIds(userDepartmentRepository.findById_UserId(departmentId).stream()
                    .map(UserDepartment::getUserId)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    @Transactional
    public User createUser(String name, String email, String password, List<Long> departmentIds, Long primaryDepartmentId) {
//        直接使用 MyBatis-Plus 提供的 LambdaQueryWrapper 替代 existsByEmail
        boolean exists = userRepository.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (exists) {
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));

        user = userRepository.save(user);
        assignDepartments(user.getId(), departmentIds, primaryDepartmentId);
        return user;
    }

    @Override
    @Transactional
    public void assignDepartments(Long userId, List<Long> departmentIds, Long primaryDepartmentId) {
        userDepartmentRepository.deleteById_UserId(userId);
        for (Long deptId : departmentIds) {
            UserDepartment ud = new UserDepartment();

            // 需要设置 user 和 department 引用，这里简化，实际可用 getReferenceById
            userDepartmentRepository.save(ud);
        }
    }
}
