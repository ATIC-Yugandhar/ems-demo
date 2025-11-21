-- Sample data for Employee Management System
USE emsdb;

-- Insert sample employees with BCrypt hashed passwords
-- Note: In production, passwords should be properly hashed using BCrypt
-- The passwords used here are: password123, adminpass, Vasavi
INSERT INTO employees (name, email, password, phone, department, role) VALUES
('John Doe', 'user2@company.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8ioctKQVkbzNEktXgCx7FSZH.VY', '9876543210', 'Engineering', 'ADMIN'),
('Sarah Wilson', 'sarah@company.com', '$2a$10$5HAc/5nGGE4zbggZrWH8i.yfUfFP.k6kHGnAqQaJq3ckYwN8x9K.', '4444449876', 'IT', 'EMPLOYEE'),
('Vasavi', 'vasavi@company.com', '$2a$10$4XmV/JhVOtV5C.cWKKlYSePUWa4XSHD3yJ9Ct9/6sJu4w8JDZQK.', '9876543210', 'IT', 'EMPLOYEE');

-- Additional sample employees for testing
INSERT INTO employees (name, email, password, phone, department, role) VALUES
('Alice Manager', 'alice.manager@company.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi', '1234567890', 'HR', 'MANAGER'),
('Bob HR', 'bob.hr@company.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi', '2345678901', 'HR', 'HR'),
('Charlie Developer', 'charlie.dev@company.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi', '3456789012', 'Engineering', 'EMPLOYEE'),
('Diana Analyst', 'diana.analyst@company.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi', '4567890123', 'Analytics', 'EMPLOYEE'),
('Eve Admin', 'eve.admin@company.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi', '5678901234', 'IT', 'ADMIN');

-- Display inserted data
SELECT id, name, email, department, role, created_at FROM employees;