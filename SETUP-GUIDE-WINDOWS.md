# Employee Management System - Complete Setup Guide for Windows

## Table of Contents
1. [Prerequisites Installation](#prerequisites-installation)
2. [Project Setup](#project-setup)
3. [Database Setup](#database-setup)
4. [Keycloak Setup](#keycloak-setup)
5. [Application Configuration](#application-configuration)
6. [Running the Application](#running-the-application)
7. [Authentication Testing](#authentication-testing)
8. [RBAC Testing](#rbac-testing)
9. [Client-Level Authentication Testing](#client-level-authentication-testing)
10. [Troubleshooting](#troubleshooting)

## Prerequisites Installation

### 1. Install Java 11
1. Download OpenJDK 11 from [Eclipse Adoptium](https://adoptium.net/temurin/releases/?version=11)
2. Choose **Windows x64** installer (.msi file)
3. Run the installer with default settings
4. Verify installation:
   ```cmd
   java -version
   javac -version
   ```
5. Set JAVA_HOME environment variable:
   - Press `Win + R`, type `sysdm.cpl`, press Enter
   - Click "Environment Variables"
   - Click "New" under System variables
   - Variable name: `JAVA_HOME`
   - Variable value: `C:\Program Files\Eclipse Adoptium\jdk-11.0.xx-hotspot` (adjust path as needed)
   - Add `%JAVA_HOME%\bin` to PATH variable

### 2. Install Maven
1. Download Maven from [Apache Maven](https://maven.apache.org/download.cgi)
2. Download the "Binary zip archive" (apache-maven-3.x.x-bin.zip)
3. Extract to `C:\Program Files\Apache\maven`
4. Add Maven to PATH:
   - Add `C:\Program Files\Apache\maven\bin` to PATH environment variable
5. Verify installation:
   ```cmd
   mvn -version
   ```

### 3. Install MySQL
1. Download MySQL Community Server from [MySQL Downloads](https://dev.mysql.com/downloads/mysql/)
2. Choose **Windows (x86, 64-bit), MSI Installer**
3. Run the installer:
   - Choose "Developer Default" setup type
   - Set root password as `root` (or remember your chosen password)
   - Configure MySQL Server as Windows Service
   - Start service automatically
4. Verify installation:
   ```cmd
   mysql -u root -p
   ```

### 4. Install Git (if not already installed)
1. Download Git from [Git for Windows](https://gitforwindows.org/)
2. Run installer with default settings
3. Verify installation:
   ```cmd
   git --version
   ```

### 5. Install curl (for API testing)
1. Download curl from [curl.se](https://curl.se/windows/)
2. Or use PowerShell (Windows 10/11 has curl built-in)
3. Verify installation:
   ```cmd
   curl --version
   ```

### 6. Install jq (for JSON processing - optional but recommended)
1. Download jq from [stedolan.github.io/jq](https://stedolan.github.io/jq/download/)
2. Download `jq-win64.exe`
3. Rename to `jq.exe` and place in `C:\Windows\System32\` or add to PATH
4. Verify installation:
   ```cmd
   jq --version
   ```

## Project Setup

### 1. Clone Repository
```cmd
git clone https://github.com/your-repo/employee-management-system.git
cd employee-management-system
```

### 2. Verify Project Structure
```
employee-management-system/
├── employee-management/          # Spring Boot application
│   ├── src/
│   ├── pom.xml
│   └── target/
├── setup_keycloak.sh            # Keycloak setup script
├── assign_client_roles.sh       # Client role assignment script
├── test_api_endpoints.sh        # API testing script
├── keycloak-21.1.2/             # Keycloak installation (if included)
└── README.md
```

## Database Setup

### 1. Create Database and User
1. Open MySQL Command Line Client or MySQL Workbench
2. Login as root user
3. Execute the following commands:

```sql
-- Create database
CREATE DATABASE employee_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user
CREATE USER 'ems_user'@'localhost' IDENTIFIED BY 'ems_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON employee_management.* TO 'ems_user'@'localhost';
FLUSH PRIVILEGES;

-- Use the database
USE employee_management;

-- Create employees table
CREATE TABLE employees (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(100),
    role ENUM('ADMIN', 'MANAGER', 'HR', 'EMPLOYEE') DEFAULT 'EMPLOYEE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT INTO employees (name, email, password, phone, department, role) VALUES
('Admin User', 'admin@company.com', '$2a$10$example.hash', '555-0001', 'IT', 'ADMIN'),
('Manager User', 'manager@company.com', '$2a$10$example.hash', '555-0002', 'Operations', 'MANAGER'),
('HR User', 'hr@company.com', '$2a$10$example.hash', '555-0003', 'Human Resources', 'HR'),
('Employee User', 'employee@company.com', '$2a$10$example.hash', '555-0004', 'Engineering', 'EMPLOYEE');

-- Verify data
SELECT * FROM employees;
```

### 2. Test Database Connection
```cmd
mysql -u ems_user -pems_password -h localhost employee_management -e "SELECT COUNT(*) FROM employees;"
```

## Keycloak Setup

### 1. Download and Install Keycloak 21.1.2
1. Download Keycloak from [Keycloak Downloads](https://www.keycloak.org/downloads)
2. Choose **Server** distribution (ZIP file)
3. Extract to `C:\keycloak-21.1.2`

### 2. Start Keycloak
1. Open Command Prompt as Administrator
2. Navigate to Keycloak directory:
   ```cmd
   cd C:\keycloak-21.1.2
   ```
3. Set environment variables and start:
   ```cmd
   set KEYCLOAK_ADMIN=admin
   set KEYCLOAK_ADMIN_PASSWORD=admin
   bin\kc.bat start-dev
   ```
4. Wait for startup (you'll see "Keycloak server started")
5. Access Keycloak Admin Console: http://localhost:8080
6. Login with admin/admin

### 3. Configure Keycloak Realm and Client

#### Method 1: Using Setup Script (Recommended)
1. Install Git Bash or use WSL to run shell scripts
2. Convert the shell script for Windows PowerShell:

**Create `setup_keycloak.ps1`:**
```powershell
# Keycloak Setup Script for Windows
$KEYCLOAK_URL = "http://localhost:8080"
$ADMIN_USER = "admin"
$ADMIN_PASSWORD = "admin"

Write-Host "🔐 Setting up Keycloak for Employee Management System..." -ForegroundColor Green

# Get admin token
Write-Host "Getting admin access token..." -ForegroundColor Yellow
$tokenResponse = Invoke-RestMethod -Uri "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" `
    -Method POST `
    -ContentType "application/x-www-form-urlencoded" `
    -Body "username=$ADMIN_USER&password=$ADMIN_PASSWORD&grant_type=password&client_id=admin-cli"

$ADMIN_TOKEN = $tokenResponse.access_token

if (-not $ADMIN_TOKEN) {
    Write-Host "❌ Failed to get admin token" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Admin token obtained" -ForegroundColor Green

# Create realm
Write-Host "Creating employee-realm..." -ForegroundColor Yellow
$realmData = @{
    realm = "employee-realm"
    displayName = "Employee Management Realm"
    enabled = $true
    registrationAllowed = $false
    loginWithEmailAllowed = $true
    duplicateEmailsAllowed = $false
    resetPasswordAllowed = $true
    editUsernameAllowed = $false
    bruteForceProtected = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms" `
    -Method POST `
    -Headers @{ "Authorization" = "Bearer $ADMIN_TOKEN"; "Content-Type" = "application/json" } `
    -Body $realmData

Write-Host "✅ Realm created" -ForegroundColor Green

# Create client
Write-Host "Creating employee-api-client..." -ForegroundColor Yellow
$clientData = @{
    clientId = "employee-api-client"
    name = "Employee API Client"
    enabled = $true
    protocol = "openid-connect"
    publicClient = $false
    standardFlowEnabled = $true
    directAccessGrantsEnabled = $true
    serviceAccountsEnabled = $true
    authorizationServicesEnabled = $false
    secret = "employee-api-client-secret"
    redirectUris = @("http://localhost:8088/*")
    webOrigins = @("http://localhost:8088")
} | ConvertTo-Json

Invoke-RestMethod -Uri "$KEYCLOAK_URL/admin/realms/employee-realm/clients" `
    -Method POST `
    -Headers @{ "Authorization" = "Bearer $ADMIN_TOKEN"; "Content-Type" = "application/json" } `
    -Body $clientData

Write-Host "✅ Client created" -ForegroundColor Green

Write-Host "🎉 Keycloak setup completed!" -ForegroundColor Green
Write-Host "📋 Access Keycloak Admin Console at: http://localhost:8080/admin" -ForegroundColor Cyan
```

Run the PowerShell script:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
.\setup_keycloak.ps1
```

#### Method 2: Manual Setup via Admin Console
1. Access Keycloak Admin Console: http://localhost:8080
2. Login with admin/admin
3. Create a new realm:
   - Click "Add realm"
   - Name: `employee-realm`
   - Click "Create"

4. Create a client:
   - Go to "Clients" → "Create"
   - Client ID: `employee-api-client`
   - Client Protocol: `openid-connect`
   - Root URL: `http://localhost:8088`
   - Click "Save"

5. Configure client:
   - Access Type: `confidential`
   - Service Accounts Enabled: `ON`
   - Authorization Enabled: `OFF`
   - Standard Flow Enabled: `ON`
   - Direct Access Grants Enabled: `ON`
   - Valid Redirect URIs: `http://localhost:8088/*`
   - Web Origins: `http://localhost:8088`
   - Click "Save"

6. Get client secret:
   - Go to "Credentials" tab
   - Copy the "Secret" value (you'll need this)

7. Create client roles:
   - Go to "Roles" tab → "Add Role"
   - Create these roles one by one:
     - `CLIENT_READ`
     - `CLIENT_WRITE`
     - `FULL_ACCESS`
     - `READ_EMPLOYEES`
     - `CREATE_EMPLOYEES`
     - `UPDATE_EMPLOYEES`
     - `DELETE_EMPLOYEES`

8. Assign roles to service account:
   - Go to "Service Account Roles" tab
   - In "Client Roles", select `employee-api-client`
   - Add all available roles to "Assigned Roles"

## Application Configuration

### 1. Update Application Configuration
Navigate to `employee-management/src/main/resources/application.yml`:

```yaml
server:
  port: 8088

spring:
  application:
    name: employee-management
  
  datasource:
    url: jdbc:mysql://localhost:3306/employee_management?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: ems_user
    password: ems_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/employee-realm

# MyBatis Configuration
mybatis:
  type-aliases-package: com.ems.model
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

keycloak:
  realm: employee-realm
  auth-server-url: http://localhost:8080
  ssl-required: external
  resource: employee-api-client
  credentials:
    secret: employee-api-client-secret
  use-resource-role-mappings: true
  bearer-only: true

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true

logging:
  level:
    org.springframework.security: DEBUG
    com.ems: DEBUG
    com.ems.mapper: DEBUG
    org.keycloak: DEBUG
```

## Running the Application

### 1. Build the Application
```cmd
cd employee-management
mvn clean compile
```

### 2. Start the Application
```cmd
mvn spring-boot:run
```

### 3. Verify Application Startup
- Check logs for successful startup
- Application should start on port 8088
- Verify endpoints:
  - Health check: http://localhost:8088/health
  - Swagger UI: http://localhost:8088/swagger-ui.html

## Authentication Testing

### Understanding Authentication Flows

#### 1. User Authentication Flow (Resource Owner Password Credentials)
- **Purpose**: Direct user login with username/password
- **Use Case**: Web applications, mobile apps where user enters credentials
- **Token Contains**: User information, user roles from Keycloak realm
- **Expiration**: Configurable (default: 5 minutes)

#### 2. Client Credentials Flow (Service-to-Service)
- **Purpose**: Application-to-application authentication
- **Use Case**: Microservices, API integrations, automated systems
- **Token Contains**: Client information, client roles assigned to service account
- **Expiration**: Configurable (default: 5 minutes)

#### 3. Authentication Flow Comparison

| Aspect | User Authentication | Client Authentication |
|--------|-------------------|---------------------|
| Grant Type | `password` | `client_credentials` |
| Credentials | username + password | client_id + client_secret |
| Token Subject | User ID | Service Account ID |
| Roles Source | User's realm roles | Client's service account roles |
| Use Case | Human users | Applications/Services |

### 1. Test User Authentication (Username/Password Flow)

#### Get User Token:
```cmd
curl -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer"
}
```

#### Test API with User Token:
```cmd
set USER_TOKEN=eyJhbGciOiJSUzI1NiIs...

curl -H "Authorization: Bearer %USER_TOKEN%" "http://localhost:8088/api/employees/search"
```

### 2. Test Client Credentials Authentication (Service-to-Service Flow)

#### Get Client Token:
```cmd
curl -X POST "http://localhost:8080/realms/employee-realm/protocol/openid-connect/token" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "grant_type=client_credentials&client_id=employee-api-client&client_secret=employee-api-client-secret"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "expires_in": 300,
  "token_type": "Bearer",
  "scope": "profile email"
}
```

#### Test API with Client Token:
```cmd
set CLIENT_TOKEN=eyJhbGciOiJSUzI1NiIs...

curl -H "Authorization: Bearer %CLIENT_TOKEN%" "http://localhost:8088/api/employees/search"
```

## RBAC Testing

### Understanding Role-Based Access Control

#### Role Hierarchy:
1. **READ_EMPLOYEES**: Can view employee data
2. **CREATE_EMPLOYEES**: Can create new employees  
3. **UPDATE_EMPLOYEES**: Can modify existing employees
4. **DELETE_EMPLOYEES**: Can remove employees
5. **FULL_ACCESS**: Can perform all operations
6. **CLIENT_READ**: Service-to-service read access
7. **CLIENT_WRITE**: Service-to-service write access

#### Endpoint Permissions:
| Endpoint | Method | Required Roles |
|----------|--------|---------------|
| `/api/employees/search` | GET | `READ_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_READ` |
| `/api/employees/{id}` | GET | `READ_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_READ` |
| `/api/employees/add` | POST | `CREATE_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_WRITE` |
| `/api/employees/add-Multiple` | POST | `FULL_ACCESS`, `CLIENT_WRITE` |
| `/api/employees/bulk-upload` | POST | `FULL_ACCESS`, `CLIENT_WRITE` |
| `/api/employees/update/{id}` | PUT | `UPDATE_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_WRITE` |
| `/api/employees/delete/{id}` | DELETE | `DELETE_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_WRITE` |

### RBAC Test Cases

#### Test Case 1: Read Operations (CLIENT_READ role)

**Setup**: Ensure token has only `CLIENT_READ` role

```cmd
rem Get token (should have CLIENT_READ role)
for /f "tokens=*" %%i in ('curl -s -X POST "http://localhost:8080/realms/employee-realm/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials&client_id=employee-api-client&client_secret=employee-api-client-secret" ^| jq -r ".access_token"') do set TOKEN=%%i

rem Test 1.1: Search employees (should succeed)
curl -w "\nStatus: %%{http_code}\n" -H "Authorization: Bearer %TOKEN%" "http://localhost:8088/api/employees/search"

rem Test 1.2: Get specific employee (should succeed)  
curl -w "\nStatus: %%{http_code}\n" -H "Authorization: Bearer %TOKEN%" "http://localhost:8088/api/employees/1"

rem Test 1.3: Try to create employee (should fail with 403)
curl -w "\nStatus: %%{http_code}\n" -X POST -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"name\":\"Test User\",\"email\":\"test@example.com\",\"password\":\"password123\",\"phone\":\"555-1234\",\"department\":\"IT\",\"role\":\"EMPLOYEE\"}" "http://localhost:8088/api/employees/add"
```

**Expected Results:**
- ✅ Search employees: `200 OK`
- ✅ Get specific employee: `200 OK`  
- ❌ Create employee: `403 Forbidden`

#### Test Case 2: Write Operations (CLIENT_WRITE role)

```cmd
rem Test 2.1: Create employee (should succeed)
curl -w "\nStatus: %%{http_code}\n" -X POST -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"name\":\"Test User\",\"email\":\"test@example.com\",\"password\":\"password123\",\"phone\":\"555-1234\",\"department\":\"IT\",\"role\":\"EMPLOYEE\"}" "http://localhost:8088/api/employees/add"

rem Test 2.2: Update employee (should succeed)
curl -w "\nStatus: %%{http_code}\n" -X PUT -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"name\":\"Updated User\",\"email\":\"test@example.com\",\"password\":\"password123\",\"phone\":\"555-1234\",\"department\":\"IT\",\"role\":\"EMPLOYEE\"}" "http://localhost:8088/api/employees/update/1"

rem Test 2.3: Delete employee (should succeed)
curl -w "\nStatus: %%{http_code}\n" -X DELETE -H "Authorization: Bearer %TOKEN%" "http://localhost:8088/api/employees/delete/1"
```

**Expected Results:**
- ✅ Create employee: `201 Created`
- ✅ Update employee: `200 OK`
- ✅ Delete employee: `200 OK`

#### Test Case 3: No Authentication (should fail)

```cmd
rem Test 3.1: Access without token (should fail)
curl -w "\nStatus: %%{http_code}\n" "http://localhost:8088/api/employees/search"

rem Test 3.2: Access with invalid token (should fail)  
curl -w "\nStatus: %%{http_code}\n" -H "Authorization: Bearer invalid_token" "http://localhost:8088/api/employees/search"
```

**Expected Results:**
- ❌ No token: `401 Unauthorized`
- ❌ Invalid token: `401 Unauthorized`

#### Test Case 4: FULL_ACCESS role (should access everything)

```cmd
rem Test with FULL_ACCESS role (modify Keycloak to assign only FULL_ACCESS to service account)

rem Test 4.1: Read operations (should succeed)
curl -w "\nStatus: %%{http_code}\n" -H "Authorization: Bearer %TOKEN%" "http://localhost:8088/api/employees/search"

rem Test 4.2: Write operations (should succeed)
curl -w "\nStatus: %%{http_code}\n" -X POST -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "{\"name\":\"Full Access Test\",\"email\":\"fullaccess@example.com\",\"password\":\"password123\",\"phone\":\"555-9999\",\"department\":\"Admin\",\"role\":\"EMPLOYEE\"}" "http://localhost:8088/api/employees/add"

rem Test 4.3: Bulk operations (should succeed)
curl -w "\nStatus: %%{http_code}\n" -X POST -H "Authorization: Bearer %TOKEN%" -H "Content-Type: application/json" -d "[{\"name\":\"Bulk User 1\",\"email\":\"bulk1@example.com\",\"password\":\"password123\",\"phone\":\"555-0001\",\"department\":\"IT\",\"role\":\"EMPLOYEE\"},{\"name\":\"Bulk User 2\",\"email\":\"bulk2@example.com\",\"password\":\"password123\",\"phone\":\"555-0002\",\"department\":\"HR\",\"role\":\"EMPLOYEE\"}]" "http://localhost:8088/api/employees/add-Multiple"
```

**Expected Results:**
- ✅ All operations: `200/201 OK`

## Client-Level Authentication Testing

### Comprehensive Client Authentication Test Suite

#### Test Suite 1: Token Validation Tests

```cmd
rem Create test script: test_client_auth.bat
@echo off
echo 🧪 Client Authentication Test Suite
echo =====================================

rem Test 1: Get valid client token
echo.
echo 📋 Test 1: Getting client credentials token...
for /f "tokens=*" %%i in ('curl -s -X POST "http://localhost:8080/realms/employee-realm/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials&client_id=employee-api-client&client_secret=employee-api-client-secret"') do set TOKEN_RESPONSE=%%i

rem Extract token (requires jq or manual parsing)
for /f "tokens=*" %%i in ('echo %TOKEN_RESPONSE% ^| jq -r ".access_token"') do set CLIENT_TOKEN=%%i

if "%CLIENT_TOKEN%"=="null" (
    echo ❌ Failed to get client token
    exit /b 1
)

echo ✅ Client token obtained: %CLIENT_TOKEN:~0,50%...

rem Test 2: Validate token structure
echo.
echo 📋 Test 2: Validating token structure...
curl -s -H "Authorization: Bearer %CLIENT_TOKEN%" "http://localhost:8088/health"
if %errorlevel%==0 (
    echo ✅ Token structure valid
) else (
    echo ❌ Token structure invalid
)

rem Test 3: Test API endpoints with client token
echo.
echo 📋 Test 3: Testing API endpoints with client credentials...

echo Testing GET /api/employees/search...
curl -s -w "Status: %%{http_code}\n" -H "Authorization: Bearer %CLIENT_TOKEN%" "http://localhost:8088/api/employees/search"

echo.
echo Testing GET /api/employees/1...
curl -s -w "Status: %%{http_code}\n" -H "Authorization: Bearer %CLIENT_TOKEN%" "http://localhost:8088/api/employees/1"

echo.
echo Testing POST /api/employees/add...
curl -s -w "Status: %%{http_code}\n" -X POST -H "Authorization: Bearer %CLIENT_TOKEN%" -H "Content-Type: application/json" -d "{\"name\":\"Client Test User\",\"email\":\"clienttest@example.com\",\"password\":\"password123\",\"phone\":\"555-CLIENT\",\"department\":\"Testing\",\"role\":\"EMPLOYEE\"}" "http://localhost:8088/api/employees/add"

echo.
echo 🎉 Client authentication tests completed!
```

#### Test Suite 2: Role Authorization Tests

```cmd
rem Create test script: test_client_roles.bat
@echo off
echo 🔐 Client Role Authorization Test Suite
echo =======================================

rem Get client token
for /f "tokens=*" %%i in ('curl -s -X POST "http://localhost:8080/realms/employee-realm/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials&client_id=employee-api-client&client_secret=employee-api-client-secret" ^| jq -r ".access_token"') do set CLIENT_TOKEN=%%i

echo Testing CLIENT_READ permissions...
echo.

rem Test read operations
echo 📖 Testing read permissions:
curl -w "GET /search - Status: %%{http_code}\n" -o nul -s -H "Authorization: Bearer %CLIENT_TOKEN%" "http://localhost:8088/api/employees/search"
curl -w "GET /1 - Status: %%{http_code}\n" -o nul -s -H "Authorization: Bearer %CLIENT_TOKEN%" "http://localhost:8088/api/employees/1"

echo.
echo 📝 Testing write permissions:
curl -w "POST /add - Status: %%{http_code}\n" -o nul -s -X POST -H "Authorization: Bearer %CLIENT_TOKEN%" -H "Content-Type: application/json" -d "{\"name\":\"Write Test\",\"email\":\"writetest@example.com\",\"password\":\"password123\",\"phone\":\"555-WRITE\",\"department\":\"Testing\",\"role\":\"EMPLOYEE\"}" "http://localhost:8088/api/employees/add"

echo.
echo 🗑️ Testing delete permissions:
curl -w "DELETE /delete/999 - Status: %%{http_code}\n" -o nul -s -X DELETE -H "Authorization: Bearer %CLIENT_TOKEN%" "http://localhost:8088/api/employees/delete/999"

echo.
echo 🎯 Role authorization tests completed!
```

#### Test Suite 3: Security Boundary Tests

```cmd
rem Create test script: test_security_boundaries.bat
@echo off
echo 🛡️ Security Boundary Test Suite
echo ===============================

echo Testing security boundaries...

rem Test 1: No authentication
echo.
echo 📋 Test 1: No authentication (should get 401)
curl -w "No Auth - Status: %%{http_code}\n" -o nul -s "http://localhost:8088/api/employees/search"

rem Test 2: Invalid token
echo.
echo 📋 Test 2: Invalid token (should get 401)
curl -w "Invalid Token - Status: %%{http_code}\n" -o nul -s -H "Authorization: Bearer invalid.jwt.token" "http://localhost:8088/api/employees/search"

rem Test 3: Expired token (simulate)
echo.
echo 📋 Test 3: Malformed token (should get 401)
curl -w "Malformed Token - Status: %%{http_code}\n" -o nul -s -H "Authorization: Bearer not-a-valid-jwt" "http://localhost:8088/api/employees/search"

rem Test 4: Wrong realm token
echo.
echo 📋 Test 4: Getting token from wrong realm...
for /f "tokens=*" %%i in ('curl -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" ^| jq -r ".access_token"') do set WRONG_TOKEN=%%i

curl -w "Wrong Realm Token - Status: %%{http_code}\n" -o nul -s -H "Authorization: Bearer %WRONG_TOKEN%" "http://localhost:8088/api/employees/search"

echo.
echo 🔒 Security boundary tests completed!
```

### Running All Test Suites

**Create master test runner: `run_all_tests.bat`:**
```cmd
@echo off
echo 🧪 Employee Management System - Complete Test Suite
echo ==================================================

echo Starting all test suites...
echo.

call test_client_auth.bat
echo.

call test_client_roles.bat  
echo.

call test_security_boundaries.bat
echo.

echo 🎉 All test suites completed!
echo.
echo 📊 Summary:
echo   ✅ Client Authentication Tests
echo   ✅ Role Authorization Tests  
echo   ✅ Security Boundary Tests
echo.
echo Check output above for any ❌ failures.
```

## Troubleshooting

### Common Issues and Solutions

#### Issue 1: "JAVA_HOME not set"
**Solution:**
```cmd
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-11.0.xx-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
```

#### Issue 2: "Maven not found"
**Solution:**
```cmd
set MAVEN_HOME=C:\Program Files\Apache\maven
set PATH=%MAVEN_HOME%\bin;%PATH%
```

#### Issue 3: "Connection refused to MySQL"
**Solution:**
1. Start MySQL service:
   ```cmd
   net start mysql
   ```
2. Check MySQL is running on port 3306:
   ```cmd
   netstat -an | findstr 3306
   ```

#### Issue 4: "Keycloak not accessible"
**Solution:**
1. Check if Keycloak is running:
   ```cmd
   netstat -an | findstr 8080
   ```
2. Restart Keycloak:
   ```cmd
   cd C:\keycloak-21.1.2
   bin\kc.bat start-dev
   ```

#### Issue 5: "401 Unauthorized" for all requests
**Possible Causes:**
1. Wrong realm in JWT issuer URL
2. Client not configured properly in Keycloak
3. Service account roles not assigned
4. Token expired (tokens expire in 5 minutes)

**Solution:**
1. Verify application.yml has correct issuer URL:
   ```yaml
   spring:
     security:
       oauth2:
         resourceserver:
           jwt:
             issuer-uri: http://localhost:8080/realms/employee-realm
   ```

2. Check client configuration in Keycloak Admin Console
3. Re-run role assignment script
4. Get fresh token

#### Issue 6: "403 Forbidden" errors
**Cause:** Missing roles for the operation
**Solution:**
1. Check service account roles in Keycloak
2. Verify JWT token contains required roles:
   ```cmd
   rem Decode JWT token payload (base64 decode middle part)
   echo %CLIENT_TOKEN% | jq -R 'split(".") | .[1] | @base64d | fromjson | .resource_access'
   ```

#### Issue 7: Port conflicts
**Solution:**
1. Check what's using the port:
   ```cmd
   netstat -ano | findstr :8088
   ```
2. Kill the process or change port in application.yml

#### Issue 8: Database connection issues
**Solution:**
1. Test MySQL connection:
   ```cmd
   mysql -u ems_user -pems_password -h localhost -e "SELECT 1"
   ```
2. Check firewall settings
3. Verify MySQL user permissions

### Debug Commands

#### Check Application Status:
```cmd
curl http://localhost:8088/health
```

#### Verify JWT Token Content:
```cmd
rem Get token
for /f "tokens=*" %%i in ('curl -s -X POST "http://localhost:8080/realms/employee-realm/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials&client_id=employee-api-client&client_secret=employee-api-client-secret" ^| jq -r ".access_token"') do set TOKEN=%%i

rem Decode token (requires online JWT decoder or jq with base64 decode)
echo Token: %TOKEN%
```

#### Check Keycloak Configuration:
```cmd
curl -H "Authorization: Bearer ADMIN_TOKEN" "http://localhost:8080/admin/realms/employee-realm/clients"
```

#### Monitor Application Logs:
- Check console output where Spring Boot is running
- Look for security-related DEBUG logs
- Watch for JWT validation errors

### Performance Tips

1. **Token Caching**: Tokens are valid for 5 minutes - cache them to avoid frequent requests
2. **Connection Pooling**: MySQL connections are pooled by default
3. **Keycloak Performance**: For production, use external database instead of H2
4. **JVM Settings**: For better performance, set JVM heap size:
   ```cmd
   set JAVA_OPTS=-Xmx1024m -Xms512m
   mvn spring-boot:run
   ```

## Next Steps

1. **Production Deployment**: Configure for production environment
2. **SSL/HTTPS**: Enable HTTPS for both Keycloak and Spring Boot
3. **Database Tuning**: Optimize MySQL configuration for production load
4. **Monitoring**: Add application monitoring and logging
5. **CI/CD**: Set up automated testing and deployment pipeline

---

## Support

If you encounter issues not covered in this guide:

1. Check application logs for error details
2. Verify all services are running (MySQL, Keycloak, Spring Boot)
3. Test each component individually
4. Consult the troubleshooting section above
5. Review Keycloak and Spring Security documentation

**Happy testing! 🚀**