package com.ems.service;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.exception.ResourceNotFoundException;
import com.ems.mapper.EmployeeMapper;
import com.ems.model.Employee;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeMapper.countByEmail(request.getEmail()) > 0) {
            throw new IllegalArgumentException("Employee with email " + request.getEmail() + " already exists");
        }

        Employee employee = mapToEntity(request);
        employee.setPassword(passwordEncoder.encode(request.getPassword()));

        employeeMapper.insert(employee);
        log.info("Created employee with ID: {}", employee.getId());
        
        return convertToResponse(employee);
    }

    public List<EmployeeResponse> createMultipleEmployees(List<EmployeeRequest> requests) {
        List<Employee> validEmployees = new ArrayList<>();
        
        for (EmployeeRequest request : requests) {
            if (employeeMapper.countByEmail(request.getEmail()) == 0) {
                Employee employee = mapToEntity(request);
                employee.setPassword(passwordEncoder.encode(request.getPassword()));
                validEmployees.add(employee);
                log.info("Prepared employee for batch insert: {}", request.getEmail());
            } else {
                log.warn("Skipped employee with existing email: {}", request.getEmail());
            }
        }
        
        if (!validEmployees.isEmpty()) {
            employeeMapper.batchInsert(validEmployees);
            log.info("Batch inserted {} employees", validEmployees.size());
        }
        
        return validEmployees.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<EmployeeResponse> bulkUploadFromCSV(MultipartFile file) throws IOException, CsvException {
        List<Employee> validEmployees = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> records = reader.readAll();
            
            if (records.size() > 1) { // Skip header row
                for (int i = 1; i < records.size(); i++) {
                    String[] record = records.get(i);
                    
                    if (record.length >= 6) {
                        String email = record[1].trim();
                        
                        if (employeeMapper.countByEmail(email) == 0) {
                            Employee employee = new Employee();
                            employee.setName(record[0].trim());
                            employee.setEmail(email);
                            employee.setDepartment(record[2].trim());
                            employee.setPhone(record[3].trim());
                            employee.setPassword(passwordEncoder.encode(record[4].trim()));
                            employee.setRole(record[5].trim());

                            validEmployees.add(employee);
                        } else {
                            log.warn("Skipped employee from CSV with existing email: {}", email);
                        }
                    }
                }
            }
        }
        
        if (!validEmployees.isEmpty()) {
            employeeMapper.batchInsert(validEmployees);
            log.info("Batch inserted {} employees from CSV", validEmployees.size());
        }
        
        return validEmployees.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found with ID: " + id);
        }
        
        return convertToResponse(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> searchEmployees(String name, String email, String department, String role) {
        List<Employee> employees;
        
        // If all search parameters are null or empty, return all employees
        if ((name == null || name.trim().isEmpty()) && 
            (email == null || email.trim().isEmpty()) && 
            (department == null || department.trim().isEmpty()) && 
            (role == null || role.trim().isEmpty())) {
            employees = employeeMapper.findAll();
        } else {
            employees = employeeMapper.searchEmployees(name, email, department, role);
        }
        
        return employees.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee existing = employeeMapper.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Employee not found with ID: " + id);
        }

        // Check if email is being changed and if new email already exists
        if (!existing.getEmail().equals(request.getEmail()) && 
            employeeMapper.countByEmail(request.getEmail()) > 0) {
            throw new IllegalArgumentException("Employee with email " + request.getEmail() + " already exists");
        }

        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setDepartment(request.getDepartment());
        existing.setRole(request.getRole());
        
        // Only update password if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            employeeMapper.updatePassword(id, passwordEncoder.encode(request.getPassword()));
        }

        employeeMapper.update(existing);
        log.info("Updated employee with ID: {}", id);
        
        // Fetch updated employee to get the latest data including timestamps
        Employee updatedEmployee = employeeMapper.findById(id);
        return convertToResponse(updatedEmployee);
    }

    public void deleteEmployee(Long id) {
        Employee existing = employeeMapper.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Employee not found with ID: " + id);
        }

        employeeMapper.deleteById(id);
        log.info("Deleted employee with ID: {}", id);
    }

    private Employee mapToEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setRole(request.getRole());
        return employee;
    }

    private EmployeeResponse convertToResponse(Employee employee) {
        return new EmployeeResponse(
            employee.getId(),
            employee.getName(),
            employee.getEmail(),
            employee.getPhone(),
            employee.getDepartment(),
            employee.getRole(),
            employee.getCreatedAt(),
            employee.getUpdatedAt()
        );
    }
}