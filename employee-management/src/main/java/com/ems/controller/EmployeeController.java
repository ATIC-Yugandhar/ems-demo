package com.ems.controller;

import com.ems.dto.ApiResponse;
import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.service.EmployeeService;
import com.ems.util.ClientUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Validated
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('CREATE_EMPLOYEES', 'FULL_ACCESS', 'CLIENT_WRITE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        log.info("Creating employee with email: {} by user: {}", request.getEmail(), ClientUtils.getCurrentUserEmail());
        
        EmployeeResponse employee = employeeService.createEmployee(request);
        ApiResponse<EmployeeResponse> response = ApiResponse.created(employee, "Employee created successfully");
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/add-Multiple")
    @PreAuthorize("hasAnyRole('FULL_ACCESS', 'CLIENT_WRITE')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> createMultipleEmployees(
            @Valid @RequestBody List<EmployeeRequest> requests) {
        log.info("Creating {} employees by user: {}", requests.size(), ClientUtils.getCurrentUserEmail());
        
        List<EmployeeResponse> employees = employeeService.createMultipleEmployees(requests);
        ApiResponse<List<EmployeeResponse>> response = ApiResponse.created(employees, "Employees created successfully");
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FULL_ACCESS', 'CLIENT_WRITE')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> bulkUpload(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Bulk uploading employees from CSV by user: {}", ClientUtils.getCurrentUserEmail());
            
            List<EmployeeResponse> employees = employeeService.bulkUploadFromCSV(file);
            ApiResponse<List<EmployeeResponse>> response = ApiResponse.success(employees, "Upload employees via CSV");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading CSV: {}", e.getMessage());
            ApiResponse<List<EmployeeResponse>> response = ApiResponse.error(400, "CSV upload failed: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('READ_EMPLOYEES', 'FULL_ACCESS', 'CLIENT_READ')")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        log.info("Getting employee by ID: {} by user: {}", id, ClientUtils.getCurrentUserEmail());
        
        EmployeeResponse employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('READ_EMPLOYEES', 'FULL_ACCESS', 'CLIENT_READ')")
    public ResponseEntity<List<EmployeeResponse>> searchEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String role) {
        log.info("Searching employees by user: {}", ClientUtils.getCurrentUserEmail());
        
        List<EmployeeResponse> employees = employeeService.searchEmployees(name, email, department, role);
        return ResponseEntity.ok(employees);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('UPDATE_EMPLOYEES', 'FULL_ACCESS', 'CLIENT_WRITE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id, 
            @Valid @RequestBody EmployeeRequest request) {
        log.info("Updating employee ID: {} by user: {}", id, ClientUtils.getCurrentUserEmail());
        
        EmployeeResponse employee = employeeService.updateEmployee(id, request);
        ApiResponse<EmployeeResponse> response = ApiResponse.success(employee, "Update employee information");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('DELETE_EMPLOYEES', 'FULL_ACCESS', 'CLIENT_WRITE')")
    public ResponseEntity<ApiResponse<String>> deleteEmployee(@PathVariable Long id) {
        log.info("Deleting employee ID: {} by user: {}", id, ClientUtils.getCurrentUserEmail());
        
        employeeService.deleteEmployee(id);
        ApiResponse<String> response = ApiResponse.success(null, "Employee deleted successfully with id " + id);
        
        return ResponseEntity.ok(response);
    }
}