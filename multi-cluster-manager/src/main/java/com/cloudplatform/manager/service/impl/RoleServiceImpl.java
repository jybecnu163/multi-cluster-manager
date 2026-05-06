package com.cloudplatform.manager.service.impl;

import com.cloudplatform.manager.mapper.RoleMapper;
import com.cloudplatform.manager.mapper.UserRoleMapper;
import com.cloudplatform.manager.model.entity.Role;
import com.cloudplatform.manager.model.entity.UserRole;
import com.cloudplatform.manager.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {
    @Autowired
    private RoleMapper roleRepository;
    @Autowired
    private UserRoleMapper userRoleRepository;

    @Override
    public List<Role> listRoles() {
        return roleRepository.selectList(null);
//        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public void assignRole(Long userId, Short roleId, String envType, Long departmentId) {
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        ur.setEnvType(envType);
        ur.setDepartmentId(departmentId);
        userRoleRepository.insert(ur);
    }
}
