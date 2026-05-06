package com.cloudplatform.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cloudplatform.manager.mapper.CompanyMapper;
import com.cloudplatform.manager.mapper.DepartmentMapper;
import com.cloudplatform.manager.mapper.ServiceInstanceMapper;
import com.cloudplatform.manager.model.entity.Company;
import com.cloudplatform.manager.model.entity.Department;
import com.cloudplatform.manager.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService {
    @Autowired
    private CompanyMapper companyRepository;
    @Autowired
    private DepartmentMapper departmentRepository;
    @Autowired
    private ServiceInstanceMapper serviceInstanceRepository;

    @Override
    public List<Company> listCompanies() {
        return companyRepository.selectList(null);
//        return companyRepository.findAll();
    }

    @Override
    @Transactional
    public Company createCompany(String name) {
        Company companies = companyRepository.selectOne(
                new LambdaQueryWrapper<Company>()
                        .eq(Company::getName, name));
        if (null != companies) {
            throw new RuntimeException("Company name already exists");
        }
        Company company = new Company();
        company.setName(name);
        long id = companyRepository.insert(company);
        company.setId(id);

        return company;
    }

    @Override
    @Transactional
    public int deleteCompany(Long id) {
        long deptCount = departmentRepository
                .selectCount(new LambdaQueryWrapper<Department>()
                        .eq(Department::getCompanyId, id));
//        long deptCount = departmentRepository.countByCompanyId(id);
        if (deptCount > 0) {
            throw new RuntimeException("Company has departments");
        }
        // 联表操作的可以先查询一个表，获取到数据后再查一个表
        long svcCount = serviceInstanceRepository.countByCompanyId(id);
        if (svcCount > 0) {
            throw new RuntimeException("Company has services");
        }
        return companyRepository.deleteById(id);
    }
}
