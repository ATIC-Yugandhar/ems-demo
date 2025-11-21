package com.ems.mapper;

import com.ems.model.Employee;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EmployeeMapper {
    
    // Find by ID
    @Select("SELECT * FROM employees WHERE id = #{id}")
    Employee findById(Long id);
    
    // Find by email
    @Select("SELECT * FROM employees WHERE email = #{email}")
    Employee findByEmail(String email);
    
    // Check if email exists
    @Select("SELECT COUNT(*) FROM employees WHERE email = #{email}")
    int countByEmail(String email);
    
    // Find all employees
    @Select("SELECT * FROM employees ORDER BY created_at DESC")
    List<Employee> findAll();
    
    // Search employees with filters
    List<Employee> searchEmployees(@Param("name") String name,
                                   @Param("email") String email,
                                   @Param("department") String department,
                                   @Param("role") String role);
    
    // Insert employee
    @Insert("INSERT INTO employees (name, email, password, phone, department, role) " +
            "VALUES (#{name}, #{email}, #{password}, #{phone}, #{department}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Employee employee);
    
    // Batch insert employees
    int batchInsert(@Param("list") List<Employee> employees);
    
    // Update employee
    @Update("UPDATE employees SET name = #{name}, email = #{email}, " +
            "phone = #{phone}, department = #{department}, role = #{role}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(Employee employee);
    
    // Update employee password
    @Update("UPDATE employees SET password = #{password}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    // Delete employee
    @Delete("DELETE FROM employees WHERE id = #{id}")
    int deleteById(Long id);
    
    // Count employees
    @Select("SELECT COUNT(*) FROM employees")
    long count();
    
    // Find by department
    @Select("SELECT * FROM employees WHERE department = #{department} ORDER BY created_at DESC")
    List<Employee> findByDepartment(String department);
    
    // Find by role
    @Select("SELECT * FROM employees WHERE role = #{role} ORDER BY created_at DESC")
    List<Employee> findByRole(String role);
}