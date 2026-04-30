package com.cloudplatform.manager.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController extends BaseController {

    @GetMapping
    public ResponseEntity<?> listCompanies() {
        return notImplemented();
    }

    @PostMapping
    public ResponseEntity<?> createCompany(@RequestBody Object request) {
        return notImplemented();
    }

    @DeleteMapping("/{company_id}")
    public ResponseEntity<?> deleteCompany(@PathVariable UUID companyId) {
        return notImplemented();
    }
}