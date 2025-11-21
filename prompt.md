# Prompt for Claude Code: Employee Management System with Keycloak Authentication

## Project Overview
Create a Spring Boot Employee Management System with Keycloak integration for authentication and authorization. The system should support both user-based authentication (via custom Keycloak provider that validates against MySQL) and client-level authentication (for service-to-service communication). **Use MyBatis for all database operations.**

---

## Technical Stack

### Backend
- **Framework**: Spring Boot 2.7.x or 3.x
- **Language**: Java 11 or higher
- **Build Tool**: Maven
- **Database**: MySQL 8.0
- **Database Mapper**: MyBatis (NOT JPA/Hibernate)
- **Authentication**: Keycloak (with custom User Storage Provider)
- **API Documentation**: Swagger/OpenAPI 3.0
- **Security**: Spring Security + OAuth2 Resource Server

### Key Dependencies
```xml
<!-- Core Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- MyBatis (REQUIRED - Use MyBatis instead of JPA) -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>

<!-- Keycloak -->
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-spring-boot-starter</artifactId>
    <version>23.0.0</version>
</dependency>

<!-- Database -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Swagger/OpenAPI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.7.0</version>
</dependency>

<!-- Utilities -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- DO NOT include spring-boot-starter-data-jpa -->
```

---

## Database Schema

### Table: `employees`
```sql
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(100),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### Sample Data
```sql
INSERT INTO employees (name, email, password, phone, department, role) VALUES
('John Doe', 'user2@company.com', 'password123', '9876543210', 'Engineering', 'ADMIN'),
('Sarah Wilson', 'sarah@company.com', 'adminpass', '4444449876', 'IT', 'EMPLOYEE'),
('Vasavi', 'vasavi@company.com', 'Vasavi', '9876543210', 'IT', 'EMPLOYEE');
```

---

## Project Structure

```
employee-management/
├── src/main/java/com/ems/
│   ├── EmployeeManagementApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java              # Spring Security configuration
│   │   ├── SwaggerConfig.java               # Swagger/OpenAPI configuration
│   │   └── MyBatisConfig.java               # MyBatis configuration (optional)
│   ├── controller/
│   │   ├── AuthController.java              # Login endpoint
│   │   └── EmployeeController.java          # CRUD endpoints
│   ├── service/
│   │   ├── AuthService.java
│   │   └── EmployeeService.java
│   ├── mapper/
│   │   └── EmployeeMapper.java              # MyBatis Mapper interface
│   ├── model/
│   │   └── Employee.java                    # POJO (not JPA entity)
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── EmployeeRequest.java
│   │   └── EmployeeResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── ResourceNotFoundException.java
│   └── util/
│       └── ClientUtils.java                 # Optional: Extract client info from JWT
└── src/main/resources/
    ├── application.yml
    ├── mapper/
    │   └── EmployeeMapper.xml               # MyBatis XML mapper
    └── data.sql (optional)

keycloak-employee-provider/                   # Separate module for custom provider
├── src/main/java/com/ems/keycloak/
│   ├── config/
│   │   └── DatabaseConfig.java
│   ├── dao/
│   │   └── EmployeeDao.java                 # Use JDBC or MyBatis here too
│   ├── entity/
│   │   └── EmployeeEntity.java
│   └── provider/
│       ├── CustomUserStorageProvider.java
│       └── CustomUserStorageProviderFactory.java
└── pom.xml
```

---

## MyBatis Implementation Details

### 1. Employee Model (POJO, NOT JPA Entity)
```java
package com.ems.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String department;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**IMPORTANT: Do NOT use JPA annotations like @Entity, @Table, @Id, @Column, etc.**

### 2. EmployeeMapper Interface
```java
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
    
    // Find all employees
    @Select("SELECT * FROM employees")
    List<Employee> findAll();
    
    // Search employees (for search endpoint)
    @Select("SELECT * FROM employees WHERE name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR email LIKE CONCAT('%', #{keyword}, '%') " +
            "OR department LIKE CONCAT('%', #{keyword}, '%')")
    List<Employee> searchEmployees(@Param("keyword") String keyword);
    
    // Insert employee
    @Insert("INSERT INTO employees (name, email, password, phone, department, role) " +
            "VALUES (#{name}, #{email}, #{password}, #{phone}, #{department}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Employee employee);
    
    // Batch insert employees
    @Insert({
        "<script>",
        "INSERT INTO employees (name, email, password, phone, department, role) VALUES ",
        "<foreach collection='list' item='emp' separator=','>",
        "(#{emp.name}, #{emp.email}, #{emp.password}, #{emp.phone}, #{emp.department}, #{emp.role})",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("list") List<Employee> employees);
    
    // Update employee
    @Update("UPDATE employees SET name = #{name}, email = #{email}, " +
            "phone = #{phone}, department = #{department}, role = #{role}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int update(Employee employee);
    
    // Delete employee
    @Delete("DELETE FROM employees WHERE id = #{id}")
    int deleteById(Long id);
    
    // Count employees
    @Select("SELECT COUNT(*) FROM employees")
    long count();
}
```

### 3. Alternative: XML Mapper Configuration
Create `src/main/resources/mapper/EmployeeMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
"http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.ems.mapper.EmployeeMapper">
    
    <!-- Result Map -->
    <resultMap id="EmployeeResultMap" type="com.ems.model.Employee">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="email" column="email"/>
        <result property="password" column="password"/>
        <result property="phone" column="phone"/>
        <result property="department" column="department"/>
        <result property="role" column="role"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>
    
    <!-- Find by ID -->
    <select id="findById" resultMap="EmployeeResultMap">
        SELECT * FROM employees WHERE id = #{id}
    </select>
    
    <!-- Find by email -->
    <select id="findByEmail" resultMap="EmployeeResultMap">
        SELECT * FROM employees WHERE email = #{email}
    </select>
    
    <!-- Find all -->
    <select id="findAll" resultMap="EmployeeResultMap">
        SELECT * FROM employees ORDER BY created_at DESC
    </select>
    
    <!-- Search employees -->
    <select id="searchEmployees" resultMap="EmployeeResultMap">
        SELECT * FROM employees 
        WHERE name LIKE CONCAT('%', #{keyword}, '%')
           OR email LIKE CONCAT('%', #{keyword}, '%')
           OR department LIKE CONCAT('%', #{keyword}, '%')
        ORDER BY created_at DESC
    </select>
    
    <!-- Insert employee -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO employees (name, email, password, phone, department, role)
        VALUES (#{name}, #{email}, #{password}, #{phone}, #{department}, #{role})
    </insert>
    
    <!-- Batch insert -->
    <insert id="batchInsert">
        INSERT INTO employees (name, email, password, phone, department, role)
        VALUES
        <foreach collection="list" item="emp" separator=",">
            (#{emp.name}, #{emp.email}, #{emp.password}, #{emp.phone}, #{emp.department}, #{emp.role})
        </foreach>
    </insert>
    
    <!-- Update employee -->
    <update id="update">
        UPDATE employees 
        SET name = #{name},
            email = #{email},
            phone = #{phone},
            department = #{department},
            role = #{role},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    </update>
    
    <!-- Delete employee -->
    <delete id="deleteById">
        DELETE FROM employees WHERE id = #{id}
    </delete>
    
    <!-- Count -->
    <select id="count" resultType="long">
        SELECT COUNT(*) FROM employees
    </select>
    
</mapper>
```

### 4. EmployeeService with MyBatis
```java
package com.ems.service;

import com.ems.mapper.EmployeeMapper;
import com.ems.model.Employee;
import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {
    
    @Autowired
    private EmployeeMapper employeeMapper;
    
    // Create employee
    @Transactional
    public EmployeeResponse addEmployee(EmployeeRequest request) {
        Employee employee = mapToEntity(request);
        employeeMapper.insert(employee);
        return mapToResponse(employee);
    }
    
    // Bulk create employees
    @Transactional
    public List<EmployeeResponse> addMultipleEmployees(List<EmployeeRequest> requests) {
        List<Employee> employees = requests.stream()
            .map(this::mapToEntity)
            .collect(Collectors.toList());
        
        employeeMapper.batchInsert(employees);
        
        return employees.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // Get employee by ID
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        return mapToResponse(employee);
    }
    
    // Search employees
    public List<EmployeeResponse> searchEmployees(String keyword) {
        List<Employee> employees;
        if (keyword == null || keyword.trim().isEmpty()) {
            employees = employeeMapper.findAll();
        } else {
            employees = employeeMapper.searchEmployees(keyword);
        }
        
        return employees.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    // Update employee
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee existing = employeeMapper.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        
        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setDepartment(request.getDepartment());
        existing.setRole(request.getRole());
        
        employeeMapper.update(existing);
        
        return mapToResponse(existing);
    }
    
    // Delete employee
    @Transactional
    public void deleteEmployee(Long id) {
        Employee existing = employeeMapper.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        
        employeeMapper.deleteById(id);
    }
    
    // Helper methods
    private Employee mapToEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setPassword(request.getPassword()); // Should hash in production
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setRole(request.getRole());
        return employee;
    }
    
    private EmployeeResponse mapToResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setName(employee.getName());
        response.setEmail(employee.getEmail());
        response.setPhone(employee.getPhone());
        response.setDepartment(employee.getDepartment());
        response.setRole(employee.getRole());
        return response;
    }
}
```

---

## API Endpoints

### 1. Authentication
```
POST /api/auth/login
  Request: { "email": "user2@company.com", "password": "password123" }
  Response: { 
    "success": true, 
    "message": "Login successful",
    "token": "Bearer eyJhbGc..." 
  }
```

### 2. Employee CRUD Operations

#### Create Employee
```
POST /api/employees/add
  Authorization: Bearer <token>
  Requires: CREATE_EMPLOYEES or FULL_ACCESS or CLIENT_WRITE role
  Request: {
    "name": "New Employee",
    "email": "new@company.com",
    "department": "Engineering",
    "phone": "1234567890",
    "password": "securepass",
    "role": "EMPLOYEE"
  }
  Response: {
    "code": 201,
    "message": "Employee created successfully",
    "data": { employee object }
  }
```

#### Bulk Create Employees
```
POST /api/employees/add-Multiple
  Authorization: Bearer <token>
  Requires: FULL_ACCESS or CLIENT_WRITE role
  Request: [ array of employee objects ]
  Response: {
    "code": 201,
    "message": "Employees created successfully",
    "data": [ array of created employees ]
  }
```

#### Bulk Upload via CSV
```
POST /api/employees/bulk-upload
  Authorization: Bearer <token>
  Requires: FULL_ACCESS or CLIENT_WRITE role
  Content-Type: multipart/form-data
  Request: file (CSV)
  Response: {
    "code": 200,
    "message": "Upload employees via CSV",
    "data": { upload result }
  }
```

#### Get Employee by ID
```
GET /api/employees/{id}
  Authorization: Bearer <token>
  Requires: READ_EMPLOYEES or FULL_ACCESS or CLIENT_READ role
  Response: { employee object }
```

#### Search Employees
```
GET /api/employees/search
  Authorization: Bearer <token>
  Requires: READ_EMPLOYEES or FULL_ACCESS or CLIENT_READ role
  Query params: keyword (optional)
  Response: [ array of employees ]
```

#### Update Employee
```
PUT /api/employees/update/{id}
  Authorization: Bearer <token>
  Requires: UPDATE_EMPLOYEES or FULL_ACCESS or CLIENT_WRITE role
  Request: { employee update data }
  Response: {
    "code": 200,
    "message": "Update employee information",
    "data": { updated employee }
  }
```

#### Delete Employee
```
DELETE /api/employees/delete/{id}
  Authorization: Bearer <token>
  Requires: DELETE_EMPLOYEES or FULL_ACCESS or CLIENT_WRITE role
  Response: {
    "code": 200,
    "message": "Employee deleted successfully with id <id>"
  }
```

---

## Security Configuration Requirements

### Role-Based Access Control (RBAC)

**User Roles (from MySQL via Keycloak Provider):**
- `READ_EMPLOYEES` - Can read employee data
- `CREATE_EMPLOYEES` - Can create employees
- `UPDATE_EMPLOYEES` - Can update employees
- `DELETE_EMPLOYEES` - Can delete employees
- `FULL_ACCESS` - Can perform all operations

**Client Roles (from Keycloak for service-to-service):**
- `CLIENT_READ` - Client applications can read employee data
- `CLIENT_WRITE` - Client applications can create/update/delete employees

### Security Rules

```java
// Public endpoints (no authentication required)
- /health
- /migrate-passwords
- /api/auth/**
- /swagger-ui/**
- /swagger-ui.html
- /swagger-resources/**
- /v2/api-docs

// READ operations
GET /api/employees/search
  → hasAnyRole("READ_EMPLOYEES", "FULL_ACCESS", "CLIENT_READ")

GET /api/employees/{id}
  → hasAnyRole("READ_EMPLOYEES", "FULL_ACCESS", "CLIENT_READ")

// CREATE operations
POST /api/employees/add
  → hasAnyRole("CREATE_EMPLOYEES", "FULL_ACCESS", "CLIENT_WRITE")

POST /api/employees/add-Multiple
  → hasAnyRole("FULL_ACCESS", "CLIENT_WRITE")

POST /api/employees/bulk-upload
  → hasAnyRole("FULL_ACCESS", "CLIENT_WRITE")

// UPDATE operations
PUT /api/employees/update/**
  → hasAnyRole("UPDATE_EMPLOYEES", "FULL_ACCESS", "CLIENT_WRITE")

// DELETE operations
DELETE /api/employees/delete/**
  → hasAnyRole("DELETE_EMPLOYEES", "FULL_ACCESS", "CLIENT_WRITE")

// Admin endpoints
/api/admin/**
  → authenticated()
```

---

## Authentication Flow

### User Authentication (Primary Flow)
```
1. User → POST /api/auth/login with { email, password }
2. Spring Boot → Forwards to Keycloak
3. Keycloak → Calls Custom User Storage Provider
4. Provider → Queries MySQL database using MyBatis or JDBC
5. Provider → Validates credentials
6. Provider → Returns user info to Keycloak
7. Keycloak → Generates JWT with user roles
8. JWT contains:
   - sub: user email
   - realm_access.roles: ["READ_EMPLOYEES", "CREATE_EMPLOYEES", ...]
9. Spring Boot → Returns JWT to user
10. User → Uses JWT for subsequent API calls
```

### Client Authentication (Service-to-Service)
```
1. Service → POST to Keycloak token endpoint
   URL: http://localhost:8080/realms/{realm}/protocol/openid-connect/token
   Body:
     client_id=employee-api-client
     client_secret=abc123-secret-xyz
     grant_type=client_credentials

2. Keycloak → Validates client credentials (no MySQL check)

3. Keycloak → Generates JWT with client roles
   JWT contains:
   - sub: client_id
   - resource_access.{client_id}.roles: ["CLIENT_READ", "CLIENT_WRITE"]

4. Service → Uses JWT to call API

5. Spring Security → Validates JWT and extracts client roles
```

---

## Configuration Files

### application.yml
```yaml
server:
  port: 8088

spring:
  application:
    name: employee-management
  
  datasource:
    url: jdbc:mysql://localhost:3306/emsdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/your-realm
          jwk-set-uri: http://localhost:8080/realms/your-realm/protocol/openid-connect/certs

# MyBatis Configuration
mybatis:
  type-aliases-package: com.ems.model
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # Or use SLF4J: org.apache.ibatis.logging.slf4j.Slf4jImpl

keycloak:
  realm: your-realm-name
  auth-server-url: http://localhost:8080
  ssl-required: external
  resource: employee-api-client
  credentials:
    secret: your-client-secret
  use-resource-role-mappings: true
  bearer-only: true

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true

# Logging
logging:
  level:
    com.ems.mapper: DEBUG
    org.springframework.security: DEBUG
```

---

## Key Implementation Details

### 1. JWT Authentication Converter
Must extract authorities from BOTH:
- User realm roles: `realm_access.roles`
- Client resource roles: `resource_access.{client_id}.roles`

```java
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        
        // Extract user realm roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            authorities.addAll(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList()));
        }
        
        // Extract client roles
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            Map<String, Object> clientAccess = 
                (Map<String, Object>) resourceAccess.get("employee-api-client");
            if (clientAccess != null && clientAccess.get("roles") != null) {
                List<String> clientRoles = (List<String>) clientAccess.get("roles");
                authorities.addAll(clientRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList()));
            }
        }
        
        return authorities;
    });
    return converter;
}
```

### 2. Custom Keycloak User Storage Provider
- Implement `UserStorageProvider` interface
- Connect to MySQL database using **MyBatis or JDBC** (not JPA)
- Validate user credentials from employees table
- Map database roles to Keycloak roles
- Return user attributes (email, name, roles)

**Example DAO for Keycloak Provider:**
```java
package com.ems.keycloak.dao;

import com.ems.keycloak.entity.EmployeeEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EmployeeDao {
    
    @Select("SELECT * FROM employees WHERE email = #{email}")
    EmployeeEntity findByEmail(String email);
    
    @Select("SELECT * FROM employees WHERE id = #{id}")
    EmployeeEntity findById(Long id);
    
    @Select("SELECT COUNT(*) FROM employees WHERE email = #{email}")
    int countByEmail(String email);
}
```

### 3. MyBatis Best Practices

**Use annotations for simple queries:**
- Single row select
- Simple inserts/updates/deletes

**Use XML mappers for complex queries:**
- Dynamic SQL
- Complex joins
- Batch operations
- Result mapping with associations

**Transaction Management:**
```java
@Service
public class EmployeeService {
    
    @Transactional  // Spring will manage MyBatis transactions
    public void performComplexOperation() {
        employeeMapper.insert(employee1);
        employeeMapper.update(employee2);
        // Both operations in same transaction
    }
}
```

### 4. Swagger Configuration
- Configure JWT Bearer token authentication
- Support both user login and client credentials flows
- Document all endpoints with proper annotations
- Include role requirements in documentation

### 5. Exception Handling
- Global exception handler for consistent error responses
- Handle authentication failures
- Handle authorization failures (403 Forbidden)
- Handle resource not found (404)
- Handle validation errors (400 Bad Request)
- Handle MyBatis exceptions

### 6. Response Format
Standardize all API responses:
```java
{
  "code": 200,
  "message": "Operation description",
  "data": { ... }  // or [ ... ] for lists
}
```

---

## Testing Requirements

### 1. User Authentication Testing
```bash
# Login
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user2@company.com","password":"password123"}'

# Use token
curl -X GET http://localhost:8088/api/employees/search \
  -H "Authorization: Bearer <token>"
```

### 2. Client Authentication Testing
```bash
# Get client token
curl -X POST 'http://localhost:8080/realms/your-realm/protocol/openid-connect/token' \
  -d 'client_id=employee-api-client' \
  -d 'client_secret=your-secret' \
  -d 'grant_type=client_credentials'

# Use client token
curl -X GET http://localhost:8088/api/employees/search \
  -H "Authorization: Bearer <client-token>"
```

### 3. Swagger UI Testing
- Access: http://localhost:8088/swagger-ui.html
- Click "Authorize" button
- Paste Bearer token
- Test all endpoints

---

## Important Notes

1. **Port Configuration**:
   - Spring Boot app: 8088
   - Keycloak: 8080
   - MySQL: 3306

2. **Role Naming**:
   - Spring Security adds "ROLE_" prefix automatically
   - In code, use `hasAnyRole("READ_EMPLOYEES")` without prefix
   - Actual authority will be "ROLE_READ_EMPLOYEES"

3. **Token Validation**:
   - All tokens validated by Keycloak
   - Spring Security verifies JWT signature using Keycloak's public key
   - Check expiration, issuer, and audience

4. **CSV Upload Format**:
   ```csv
   name,email,department,phone,password,role
   John Doe,john@company.com,Engineering,1234567890,pass123,EMPLOYEE
   ```

5. **Password Handling**:
   - Store hashed passwords in MySQL
   - Use BCrypt or similar for password hashing
   - Keycloak provider should handle password verification

6. **MyBatis Logging**:
   - Enable SQL logging in development
   - Use proper logging level in production
   - Log parameter bindings for debugging

7. **MyBatis vs JPA**:
   - NO @Entity, @Table, @Id annotations
   - Use @Mapper on mapper interfaces
   - Use plain POJOs for models
   - MyBatis handles all SQL explicitly

---

## Deliverables

1. **Main Spring Boot Application**:
   - Complete source code with all packages
   - pom.xml with MyBatis dependencies
   - application.yml configuration with MyBatis settings
   - MyBatis mapper interfaces and XML files
   - README.md with setup instructions

2. **Custom Keycloak Provider** (separate module):
   - User Storage Provider implementation
   - Database connection using MyBatis or JDBC
   - Deployment instructions for Keycloak

3. **Database Scripts**:
   - Table creation SQL
   - Sample data SQL
   - Migration scripts if needed

4. **Documentation**:
   - API documentation (via Swagger)
   - Authentication flow diagrams
   - MyBatis mapper documentation
   - Setup and deployment guide
   - Testing guide

5. **Configuration Files**:
   - Keycloak realm export (JSON)
   - Client configuration details
   - Role mappings

---

## Setup Instructions to Include

1. **Prerequisites**:
   - Java 11+
   - Maven 3.6+
   - MySQL 8.0
   - Keycloak 20+ (or compatible version)

2. **Database Setup**:
   ```bash
   mysql -u root -p
   CREATE DATABASE emsdb;
   USE emsdb;
   # Run table creation and sample data scripts
   ```

3. **Keycloak Setup** (DETAILED):

### 3.1 Start Keycloak
```bash
# Download Keycloak from https://www.keycloak.org/downloads
# Extract and start
cd keycloak-23.0.0
bin/kc.sh start-dev  # Linux/Mac
# OR
bin\kc.bat start-dev  # Windows

# Access: http://localhost:8080
# Create admin user on first access
```

### 3.2 Create Realm
```
1. Login to Keycloak Admin Console (http://localhost:8080)
2. Hover over "master" dropdown (top-left)
3. Click "Create Realm"
4. Realm name: employee-realm_2 (or your preferred name)
5. Enabled: ON
6. Click "Create"
```

### 3.3 Create User Realm Roles
```
1. Select your realm (employee-realm_2)
2. Go to: Realm roles (left sidebar)
3. Click "Create role" and create these roles:

Role 1:
  - Role name: READ_EMPLOYEES
  - Description: Can read employee data
  - Save

Role 2:
  - Role name: CREATE_EMPLOYEES
  - Description: Can create employees
  - Save

Role 3:
  - Role name: UPDATE_EMPLOYEES
  - Description: Can update employees
  - Save

Role 4:
  - Role name: DELETE_EMPLOYEES
  - Description: Can delete employees
  - Save

Role 5:
  - Role name: FULL_ACCESS
  - Description: Can perform all operations
  - Save

Role 6:
  - Role name: ADMIN
  - Description: Administrator role
  - Save
```

### 3.4 Create Client for User Authentication
```
1. Go to: Clients (left sidebar)
2. Click "Create client"

Step 1: General Settings
  - Client type: OpenID Connect
  - Client ID: employee-app
  - Name: Employee Management Application
  - Click "Next"

Step 2: Capability config
  - Client authentication: ON
  - Authorization: OFF
  - Authentication flow:
    ✓ Standard flow
    ✓ Direct access grants
  - Click "Next"

Step 3: Login settings
  - Root URL: http://localhost:8088
  - Home URL: http://localhost:8088
  - Valid redirect URIs: http://localhost:8088/*
  - Click "Save"

4. Go to Credentials tab
  - Copy the Client Secret (you'll need this)
```

### 3.5 Create Client for Service-to-Service (CLIENT-LEVEL AUTH)
```
1. Go to: Clients (left sidebar)
2. Click "Create client"

Step 1: General Settings
  - Client type: OpenID Connect
  - Client ID: employee-api-client
  - Name: Employee Management API Client
  - Description: Client for service-to-service authentication
  - Click "Next"

Step 2: Capability config
  - Client authentication: ON ✅ (CRITICAL)
  - Authorization: ON (optional)
  - Authentication flow:
    ✓ Service accounts roles ✅ (CRITICAL - enables client credentials)
    ✗ Standard flow (uncheck)
    ✗ Direct access grants (uncheck)
  - Click "Next"

Step 3: Login settings
  - Leave empty for service accounts
  - Click "Save"

4. Go to Credentials tab
  - Client Authenticator: Client Id and Secret
  - Copy the Client Secret
  - Example: abc123-def456-ghi789
  - IMPORTANT: Save this secret, you'll need it in application.yml

5. Go to Roles tab (IMPORTANT - CREATE CLIENT ROLES)
  - Click "Create role"
  
  Role 1:
    - Role name: CLIENT_READ
    - Description: Client can read employee data
    - Save
  
  Role 2:
    - Role name: CLIENT_WRITE
    - Description: Client can create/update/delete employees
    - Save

6. Go to Service account roles tab (IMPORTANT - ASSIGN ROLES)
  - Click "Assign role"
  - Filter by clients: Select "employee-api-client"
  - You should see:
    □ CLIENT_READ
    □ CLIENT_WRITE
  - Select BOTH roles
  - Click "Assign"
  
  Result: You should see:
    ✓ CLIENT_READ (Assigned)
    ✓ CLIENT_WRITE (Assigned)
```

### 3.6 Deploy Custom User Storage Provider
```
1. Build the keycloak-employee-provider module:
   cd keycloak-employee-provider
   mvn clean package
   
2. Copy JAR to Keycloak:
   cp target/keycloak-employee-provider-1.0.jar \
      /path/to/keycloak/providers/
   
3. Restart Keycloak:
   bin/kc.sh build  # Rebuild to include new provider
   bin/kc.sh start-dev

4. Configure User Federation:
   - Go to: User federation (left sidebar)
   - Click "Add provider"
   - Select your custom provider
   - Configure database connection:
     * JDBC URL: jdbc:mysql://localhost:3306/emsdb
     * Username: root
     * Password: your_password
   - Save
   
5. Test the provider:
   - Go to Users → Add user
   - Try to login with a user from MySQL
```

### 3.7 Test Keycloak Setup

**Test User Authentication:**
```bash
# Get token using user credentials
curl -X POST 'http://localhost:8080/realms/employee-realm_2/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=employee-app' \
  -d 'username=user2@company.com' \
  -d 'password=password123' \
  -d 'grant_type=password'

# Should return:
# {
#   "access_token": "eyJhbGc...",
#   "expires_in": 300,
#   "refresh_token": "...",
#   "token_type": "Bearer"
# }
```

**Test Client Authentication:**
```bash
# Get token using client credentials
curl -X POST 'http://localhost:8080/realms/employee-realm_2/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=employee-api-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET_HERE' \
  -d 'grant_type=client_credentials'

# Should return:
# {
#   "access_token": "eyJhbGc...",
#   "expires_in": 300,
#   "token_type": "Bearer",
#   "scope": "profile email"
# }
```

**Verify Token Contents:**
```bash
# Decode the JWT at https://jwt.io

# User token should contain:
# {
#   "realm_access": {
#     "roles": ["READ_EMPLOYEES", "CREATE_EMPLOYEES", ...]
#   }
# }

# Client token should contain:
# {
#   "resource_access": {
#     "employee-api-client": {
#       "roles": ["CLIENT_READ", "CLIENT_WRITE"]
#     }
#   }
# }
```

4. **Application Setup**:
   ```bash
   cd employee-management
   
   # Update application.yml with:
   # - Keycloak realm name
   # - Client ID and secret from step 3.4 and 3.5
   # - Database credentials
   
   mvn clean install
   mvn spring-boot:run
   ```

5. **Verify Setup**:
   - Swagger UI: http://localhost:8088/swagger-ui.html
   - Test login endpoint with user from MySQL
   - Test CRUD operations with user token
   - Test CRUD operations with client token
   - Check MyBatis SQL logs

---

## Success Criteria

✅ User can login with email/password from MySQL database
✅ JWT token is generated by Keycloak
✅ Token contains user roles from database
✅ All CRUD endpoints work with proper authorization
✅ Client authentication works for service-to-service calls
✅ Client tokens are validated and authorized correctly
✅ Swagger UI is accessible and functional
✅ Bearer token authorization works in Swagger
✅ Role-based access control is enforced correctly
✅ Both user and client authentication paths coexist
✅ Error handling provides meaningful responses
✅ Code follows Spring Boot best practices
✅ **Database operations use MyBatis correctly (NOT JPA)**
✅ **MyBatis mappers work for all CRUD operations**
✅ **SQL queries are properly logged and debuggable**
✅ Application is properly documented

---

## Additional Features (Optional)

- Password migration utility (/migrate-passwords endpoint)
- Audit logging for all operations
- Pagination for search results with MyBatis
- Filtering and sorting capabilities using dynamic SQL
- Employee profile pictures
- Department management
- Role management through API
- Password reset functionality
- Email notifications
- API rate limiting
- Request/response logging

---

## MyBatis-Specific Requirements

### CRITICAL: Do NOT use JPA
- **NO** spring-boot-starter-data-jpa dependency
- **NO** @Entity, @Table, @Id, @Column annotations
- **NO** EntityManager or JpaRepository
- **NO** @Query annotations

### DO use MyBatis
- **USE** mybatis-spring-boot-starter dependency
- **USE** @Mapper annotation on mapper interfaces
- **USE** @Select, @Insert, @Update, @Delete annotations
- **USE** XML mapper files for complex queries
- **USE** plain POJOs (with Lombok @Data)
- **USE** @Transactional for transaction management

### MyBatis Annotations vs XML
Choose based on complexity:
- **Annotations**: Simple CRUD, single table operations
- **XML**: Dynamic SQL, joins, complex conditions, batch operations

---

## Reference Implementation Details

Based on the provided screenshots, the system should:
1. Use similar package structure as shown (com.ems.*)
2. Implement the exact endpoints shown in the Swagger UI
3. Return responses in the format shown
4. Support the Keycloak authentication workflow demonstrated
5. Handle both user and client authentication as illustrated
6. **Use MyBatis for ALL database operations**

This prompt provides complete context for building an Employee Management System 
with dual authentication (user + client) using Spring Boot, Keycloak, and **MyBatis**.
