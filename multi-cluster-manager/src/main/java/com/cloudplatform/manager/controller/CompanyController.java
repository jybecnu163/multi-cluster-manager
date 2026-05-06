package com.cloudplatform.manager.controller;

import com.cloudplatform.manager.dto.CompanyRequest;
import com.cloudplatform.manager.exception.BusinessException;
import com.cloudplatform.manager.model.entity.Company;
import com.cloudplatform.manager.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController extends BaseController{
    @Autowired private CompanyService companyService;

    @GetMapping
    @PreAuthorize("hasRole('系统管理员') or hasRole('审计员')")
    public ResponseEntity<List<Company>> listCompanies() {
        return ResponseEntity.ok(companyService.listCompanies());
    }

    @PostMapping
    @PreAuthorize("hasRole('系统管理员')")
    public ResponseEntity<Company> createCompany(@Valid @RequestBody CompanyRequest request) {
        Company company = companyService.createCompany(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }

    @DeleteMapping("/{company_id}")
    @PreAuthorize("hasRole('系统管理员')")
    public ResponseEntity<Void> deleteCompany(@PathVariable("company_id") Long companyId) {
        try {
            companyService.deleteCompany(companyId);
            return ResponseEntity.noContent().build();
        } catch (BusinessException e) {
            throw e; // GlobalExceptionHandler 会处理
        }
    }
}