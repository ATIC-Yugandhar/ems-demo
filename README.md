# Employee Management System

A comprehensive enterprise-grade Employee Management System built with Spring Boot, MyBatis, and Keycloak authentication, featuring role-based access control and support for both user authentication and service-to-service communication.

## 🚀 Features

- **Complete Employee CRUD Operations**: Create, read, update, and delete employee records
- **Dual Authentication Support**: 
  - User authentication (username/password flow)
  - Client credentials authentication (service-to-service)
- **Role-Based Access Control (RBAC)**: Granular permissions system
- **JWT Token Security**: OAuth2/OpenID Connect compliance via Keycloak
- **MyBatis Integration**: Custom ORM solution for optimized database operations
- **RESTful API Design**: Industry-standard REST endpoints
- **Comprehensive Testing Suite**: Unit, integration, and end-to-end tests
- **Bulk Operations**: CSV upload and batch processing
- **API Documentation**: Swagger/OpenAPI integration

## 🏗️ Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Framework** | Spring Boot | 2.7.18 |
| **ORM** | MyBatis | 2.3.2 |
| **Database** | MySQL | 8.x |
| **Authentication** | Keycloak | 21.1.2 |
| **Java** | OpenJDK | 11.0.29 |
| **Build Tool** | Apache Maven | 3.x |
| **Security** | Spring Security OAuth2 | 5.7.x |

## 📋 Prerequisites

- **Java 11** or higher
- **Maven 3.x**
- **MySQL 8.x**
- **Git**
- **curl** (for API testing)

## 🔧 Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/your-repo/employee-management-system.git
cd employee-management-system
```

### 2. Database Setup
```sql
CREATE DATABASE employee_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ems_user'@'localhost' IDENTIFIED BY 'ems_password';
GRANT ALL PRIVILEGES ON employee_management.* TO 'ems_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Download and Start Keycloak
```bash
# Download Keycloak 21.1.2
wget https://github.com/keycloak/keycloak/releases/download/21.1.2/keycloak-21.1.2.zip
unzip keycloak-21.1.2.zip
cd keycloak-21.1.2

# Start Keycloak
export KEYCLOAK_ADMIN=admin
export KEYCLOAK_ADMIN_PASSWORD=admin
./bin/kc.sh start-dev
```

### 4. Configure Keycloak
Run the setup script to configure realm, client, and roles:
```bash
chmod +x setup_keycloak.sh
./setup_keycloak.sh
```

### 5. Start Application
```bash
cd employee-management
mvn spring-boot:run
```

### 6. Verify Installation
- **Application Health**: http://localhost:8088/health
- **Swagger UI**: http://localhost:8088/swagger-ui.html
- **Keycloak Admin**: http://localhost:8080/admin (admin/admin)

## 📖 Documentation

### For Windows Users
- **[Complete Windows Setup Guide](SETUP-GUIDE-WINDOWS.md)**: Detailed step-by-step installation and configuration guide for Windows environments

### Technical Documentation
- **[Technical Architecture](TECHNICAL-ARCHITECTURE.md)**: Comprehensive system architecture, technology choices, and implementation approach

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend/     │    │   Spring Boot   │    │     Keycloak    │
│   Client App    │───▶│   Application   │───▶│     Server      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
                         ┌─────────────────┐    ┌─────────────────┐
                         │     MySQL       │    │   Custom User   │
                         │   Database      │◀───│ Storage Provider│
                         └─────────────────┘    └─────────────────┘
```

### Database Layer Architecture

The application uses **MyBatis** for all database operations:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │───▶│    Services     │───▶│     MyBatis     │
│   (REST API)    │    │  (Business      │    │    Mappers      │
│                 │    │   Logic)        │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                                       ▼
                                                ┌─────────────────┐
                                                │     MySQL       │
                                                │   Database      │
                                                └─────────────────┘
```

**Key Components:**
- **POJO Models**: Plain Java objects (no JPA annotations)
- **MyBatis Mappers**: Interface-based SQL mapping
- **XML Mappers**: Complex queries and dynamic SQL
- **JDBC Connection**: Direct database access for Keycloak provider

## 🛠️ Setup Instructions

### 1. Database Setup

```bash
# Connect to MySQL
mysql -u root -p

# Run the database creation script
source database-scripts/create_database.sql

# Insert sample data
source database-scripts/sample_data.sql
```

### 2. Keycloak Setup

1. **Start Keycloak**:
   ```bash
   # Download and start Keycloak
   ./bin/kc.sh start-dev --http-port=8080
   ```

2. **Create Admin User** (first time only):
   ```bash
   ./bin/kc.sh start-dev --http-port=8080
   # Access http://localhost:8080 and create admin user
   ```

3. **Deploy Custom Provider**:
   ```bash
   # Build the custom provider
   cd keycloak-employee-provider
   mvn clean package
   
   # Copy JAR to Keycloak providers directory
   cp target/keycloak-employee-provider-1.0.0.jar $KEYCLOAK_HOME/providers/
   
   # Restart Keycloak
   ./bin/kc.sh build
   ./bin/kc.sh start-dev --http-port=8080
   ```

4. **Configure Realm**:
   - Create realm: `employee-realm`
   - Create client: `employee-api-client`
     - Client ID: `employee-api-client`
     - Client Secret: `employee-api-client-secret`
     - Access Type: `confidential`
     - Direct Access Grants: `ON`
     - Service Accounts: `ON`
   
5. **Configure User Federation**:
   - Go to User Federation → Add Provider → `employee-user-storage`
   - Save the configuration

6. **Configure Roles**:
   
   **Realm Roles**:
   - `READ_EMPLOYEES`
   - `CREATE_EMPLOYEES`
   - `UPDATE_EMPLOYEES`
   - `DELETE_EMPLOYEES`
   - `FULL_ACCESS`
   
   **Client Roles** (for employee-api-client):
   - `CLIENT_READ`
   - `CLIENT_WRITE`

### 3. Application Setup

```bash
# Navigate to the main application
cd employee-management

# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on port 8088.

### 4. Verify Setup

1. **Swagger UI**: http://localhost:8088/swagger-ui.html
2. **Test Login Endpoint**: 
   ```bash
   curl -X POST http://localhost:8088/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"user2@company.com","password":"password123"}'
   ```

## 🔐 Authentication Flows

### User Authentication

```bash
# 1. Login to get JWT token
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user2@company.com","password":"password123"}'

# 2. Use token for API calls
curl -X GET http://localhost:8088/api/employees/search \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Client Authentication (Service-to-Service)

```bash
# 1. Get client credentials token
curl -X POST 'http://localhost:8080/realms/employee-realm/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'client_id=employee-api-client' \
  -d 'client_secret=employee-api-client-secret' \
  -d 'grant_type=client_credentials'

# 2. Use client token for API calls
curl -X GET http://localhost:8088/api/employees/search \
  -H "Authorization: Bearer <client-jwt-token>"
```

## 📚 API Endpoints

### Authentication
- `POST /api/auth/login` - User login

### Employee Management
- `POST /api/employees/add` - Create employee
- `POST /api/employees/add-Multiple` - Create multiple employees
- `POST /api/employees/bulk-upload` - CSV bulk upload
- `GET /api/employees/{id}` - Get employee by ID
- `GET /api/employees/search` - Search employees
- `PUT /api/employees/update/{id}` - Update employee
- `DELETE /api/employees/delete/{id}` - Delete employee

### Role Requirements

| Endpoint | Required Roles |
|----------|----------------|
| `POST /api/employees/add` | `CREATE_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_WRITE` |
| `POST /api/employees/add-Multiple` | `FULL_ACCESS`, `CLIENT_WRITE` |
| `POST /api/employees/bulk-upload` | `FULL_ACCESS`, `CLIENT_WRITE` |
| `GET /api/employees/*` | `READ_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_READ` |
| `PUT /api/employees/update/*` | `UPDATE_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_WRITE` |
| `DELETE /api/employees/delete/*` | `DELETE_EMPLOYEES`, `FULL_ACCESS`, `CLIENT_WRITE` |

## 🔧 Configuration

### Application Properties

Key configuration in `application.yml`:

```yaml
server:
  port: 8088

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/emsdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: root
    password: root

# MyBatis Configuration
mybatis:
  type-aliases-package: com.ems.model
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/employee-realm

keycloak:
  realm: employee-realm
  auth-server-url: http://localhost:8080
  resource: employee-api-client
  credentials:
    secret: employee-api-client-secret
```

## 📊 Sample Data

The system comes with pre-configured sample users:

| Email | Password | Role | Department |
|-------|----------|------|------------|
| user2@company.com | password123 | ADMIN | Engineering |
| sarah@company.com | adminpass | EMPLOYEE | IT |
| vasavi@company.com | Vasavi | EMPLOYEE | IT |

## 🧪 Testing

### Using Swagger UI

1. Go to http://localhost:8088/swagger-ui.html
2. Click "Authorize" button
3. Login via `/api/auth/login` to get JWT token
4. Paste token in authorization field: `Bearer <your-token>`
5. Test all endpoints

### CSV Upload Format

```csv
name,email,department,phone,password,role
John Doe,john@company.com,Engineering,1234567890,pass123,EMPLOYEE
Jane Smith,jane@company.com,HR,0987654321,jane456,MANAGER
```

## 🚨 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Verify MySQL is running on port 3306
   - Check database credentials in `application.yml`
   - Ensure `emsdb` database exists

2. **Keycloak Connection Issues**
   - Verify Keycloak is running on port 8080
   - Check realm name and client configuration
   - Ensure custom provider is deployed

3. **Authentication Failed**
   - Verify user exists in MySQL database
   - Check if custom User Storage Provider is active
   - Verify role mappings in Keycloak

4. **JWT Token Issues**
   - Check token expiration
   - Verify issuer URI in application.yml
   - Ensure proper role extraction in SecurityConfig

### Logs to Check

- Application logs: Look for authentication and authorization messages
- Keycloak logs: Check for custom provider initialization and user lookup
- MySQL logs: Verify database queries are executed

## 🏆 Success Criteria

✅ User can login with email/password from MySQL database  
✅ JWT token is generated by Keycloak  
✅ Token contains user roles from database  
✅ All CRUD endpoints work with proper authorization  
✅ Client authentication works for service-to-service calls  
✅ Swagger UI is accessible and functional  
✅ Role-based access control is enforced correctly  
✅ CSV bulk upload functionality works  
✅ Error handling provides meaningful responses  

## 📝 Additional Notes

- Passwords are stored using BCrypt hashing
- The custom Keycloak provider maps database roles to Keycloak roles
- Both user and client authentication coexist seamlessly
- All API responses follow a consistent format
- Comprehensive logging for debugging and auditing

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.