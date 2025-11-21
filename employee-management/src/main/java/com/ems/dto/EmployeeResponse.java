package com.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String department;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}