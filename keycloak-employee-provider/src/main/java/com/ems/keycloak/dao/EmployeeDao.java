package com.ems.keycloak.dao;

import com.ems.keycloak.entity.EmployeeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import com.mysql.cj.jdbc.MysqlDataSource;

public class EmployeeDao {
    private static final Logger logger = LoggerFactory.getLogger(EmployeeDao.class);
    
    private final DataSource dataSource;
    
    public EmployeeDao() {
        this.dataSource = createDataSource();
    }
    
    private DataSource createDataSource() {
        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        mysqlDataSource.setURL("jdbc:mysql://localhost:3306/emsdb?useSSL=false&serverTimezone=UTC");
        mysqlDataSource.setUser("root");
        mysqlDataSource.setPassword("root");
        return mysqlDataSource;
    }
    
    public EmployeeEntity findByEmail(String email) {
        String sql = "SELECT * FROM employees WHERE email = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    EmployeeEntity employee = mapResultSetToEntity(rs);
                    logger.debug("Found employee with email: {}", email);
                    return employee;
                } else {
                    logger.debug("No employee found with email: {}", email);
                    return null;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding employee by email: {}", email, e);
            return null;
        }
    }
    
    public boolean validatePassword(String email, String plainPassword) {
        EmployeeEntity employee = findByEmail(email);
        if (employee == null) {
            logger.debug("Employee not found for password validation: {}", email);
            return false;
        }
        
        // For BCrypt passwords (starting with $2a$, $2b$, etc.)
        if (employee.getPassword().startsWith("$2")) {
            return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(plainPassword, employee.getPassword());
        }
        
        // For plain text passwords (fallback)
        boolean isValid = employee.getPassword().equals(plainPassword);
        logger.debug("Password validation for {}: {}", email, isValid ? "SUCCESS" : "FAILED");
        return isValid;
    }
    
    public EmployeeEntity findById(Long id) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEntity(rs);
                } else {
                    return null;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error finding employee by ID: {}", id, e);
            return null;
        }
    }
    
    public int countByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM employees WHERE email = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error counting employees by email: {}", email, e);
        }
        
        return 0;
    }
    
    public void updateEmployee(EmployeeEntity employee) {
        String sql = "UPDATE employees SET name = ?, email = ?, phone = ?, department = ?, role = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, employee.getName());
            stmt.setString(2, employee.getEmail());
            stmt.setString(3, employee.getPhone());
            stmt.setString(4, employee.getDepartment());
            stmt.setString(5, employee.getRole());
            stmt.setLong(6, employee.getId());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.debug("Updated employee: {}", employee.getEmail());
            } else {
                logger.warn("No rows updated for employee: {}", employee.getEmail());
            }
            
        } catch (SQLException e) {
            logger.error("Error updating employee: {}", employee.getEmail(), e);
        }
    }
    
    private EmployeeEntity mapResultSetToEntity(ResultSet rs) throws SQLException {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setId(rs.getLong("id"));
        employee.setName(rs.getString("name"));
        employee.setEmail(rs.getString("email"));
        employee.setPassword(rs.getString("password"));
        employee.setPhone(rs.getString("phone"));
        employee.setDepartment(rs.getString("department"));
        employee.setRole(rs.getString("role"));
        
        // Handle timestamps
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            employee.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            employee.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return employee;
    }
}