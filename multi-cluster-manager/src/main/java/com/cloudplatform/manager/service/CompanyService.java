package com.cloudplatform.manager.service;

import com.cloudplatform.manager.exception.BusinessException;
import com.cloudplatform.manager.model.entity.Company;

import java.util.List;

public interface CompanyService {
    List<Company> listCompanies();

    Company createCompany(String name);

    int deleteCompany(Long id) throws BusinessException;

}
