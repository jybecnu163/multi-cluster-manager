package com.cloudplatform.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.DepartmentMapper;
import com.cloudplatform.manager.mapper.DepartmentSettingsMapper;
import com.cloudplatform.manager.model.entity.Department;
import com.cloudplatform.manager.model.entity.DepartmentSettings;
import com.cloudplatform.manager.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    @Autowired
    private DepartmentMapper departmentRepository;
    @Autowired
    private DepartmentSettingsMapper settingsRepository;

    @Override
    public List<Department> listDepartments(Long companyId) {
        if (companyId != null) {
            return departmentRepository.selectList(new LambdaQueryWrapper<Department>().eq(Department::getCompanyId,companyId));
//            return departmentRepository.findByCompanyId(companyId);
        }
        return departmentRepository.selectList(new LambdaQueryWrapper<Department>().orderByDesc(Department::getCompanyId));
//        return departmentRepository.findAll();
    }

    @Override
    @Transactional
    public Department createDepartment(Long companyId, String name, Long directorUserId) {
//        ;departmentRepository.existsByCompanyIdAndName(companyId, name)
        if (departmentRepository.exists(new LambdaQueryWrapper<Department>()
                .eq(Department::getCompanyId,companyId)
                .eq(Department::getName,name))) {
            throw new RuntimeException("Department name already exists in this company");
        }
        Department dept = new Department();
        dept.setCompanyId(companyId);
        dept.setName(name);
        dept.setDirectorUserId(directorUserId);
        return departmentRepository.save(dept);
    }

    @Override
    public DepartmentSettings getSettings(Long departmentId) {
        return settingsRepository.findById(departmentId)
                .orElseGet(() -> {
                    DepartmentSettings ds = new DepartmentSettings();
                    ds.setDepartmentId(departmentId);
                    ds.setAllowOpsBypassProdScale(false);
                    return ds;
                });
    }

    @Override
    @Transactional
    public void updateSettings(Long departmentId, Boolean allowOpsBypassProdScale) {
        DepartmentSettings ds = settingsRepository.findById(departmentId)
                .orElse(new DepartmentSettings());
        ds.setDepartmentId(departmentId);
        ds.setAllowOpsBypassProdScale(allowOpsBypassProdScale);
        ds.setUpdatedAt(Instant.now());
        settingsRepository.save(ds);
    }
}
