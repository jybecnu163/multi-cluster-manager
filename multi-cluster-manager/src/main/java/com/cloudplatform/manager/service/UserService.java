package com.cloudplatform.manager.service;

import com.cloudplatform.manager.model.entity.User;

import java.util.List;

public interface UserService {
    List<User> listUsers(Long departmentId);

    User createUser(String name, String email, String password, List<Long> departmentIds, Long primaryDepartmentId);

    void assignDepartments(Long userId, List<Long> departmentIds, Long primaryDepartmentId);
}
