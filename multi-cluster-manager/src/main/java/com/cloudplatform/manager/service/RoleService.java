package com.cloudplatform.manager.service;

import com.cloudplatform.manager.model.entity.Role;
import java.util.List;
import java.util.UUID;

public interface RoleService {
    List<Role> listRoles();
    void assignRole(Long userId, Short roleId, String envType, Long departmentId);
}
