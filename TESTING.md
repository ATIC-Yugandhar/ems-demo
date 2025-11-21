# MyBatis Migration Testing Guide

## 🧪 Required Tests

### 1. Build and Compilation
```bash
cd employee-management
mvn clean compile
mvn package -DskipTests
```

### 2. Database Setup
```bash
# Start MySQL
mysql -u root -p

# Create database and tables
source database-scripts/create_database.sql
source database-scripts/sample_data.sql
```

### 3. Application Startup Test
```bash
cd employee-management
mvn spring-boot:run
```

**Expected:** Application starts without errors and MyBatis SQL logs appear

### 4. MyBatis Mapper Tests
```bash
# Check logs for MyBatis mapper registration
grep "Registered mapper" application.log

# Expected output:
# Registered mapper: com.ems.mapper.EmployeeMapper
```

### 5. API Endpoint Tests

#### Test 1: Get All Employees
```bash
curl -X GET http://localhost:8088/api/employees/search
```
**Expected:** Should return employees from database (requires authentication)

#### Test 2: Login Test
```bash
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user2@company.com","password":"password123"}'
```
**Expected:** Should return JWT token if Keycloak is configured

#### Test 3: MyBatis SQL Logging
Check application logs for SQL execution:
```
DEBUG com.ems.mapper.EmployeeMapper.findAll - ==>  Preparing: SELECT * FROM employees ORDER BY created_at DESC
DEBUG com.ems.mapper.EmployeeMapper.findAll - ==> Parameters: 
DEBUG com.ems.mapper.EmployeeMapper.findAll - <==    Total: 3
```

### 6. Keycloak Provider Test
```bash
cd keycloak-employee-provider
mvn clean package

# Deploy to Keycloak
cp target/keycloak-employee-provider-1.0.0.jar $KEYCLOAK_HOME/providers/
```

### 7. Database Transaction Test
Test Create Employee with validation:
```bash
# Should succeed
curl -X POST http://localhost:8088/api/employees/add \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@test.com","department":"IT","role":"EMPLOYEE","password":"test123"}'

# Should fail (duplicate email)
curl -X POST http://localhost:8088/api/employees/add \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User 2","email":"test@test.com","department":"IT","role":"EMPLOYEE","password":"test456"}'
```

## 🚨 Potential Issues to Check

### Issue 1: MyBatis Mapper Not Found
**Error:** `org.apache.ibatis.binding.BindingException: Invalid bound statement`

**Solution:** 
- Verify `@Mapper` annotation on EmployeeMapper
- Check `mybatis.mapper-locations` in application.yml
- Ensure XML mapper namespace matches interface package

### Issue 2: Database Connection Failed
**Error:** `java.sql.SQLException: Access denied for user`

**Solution:**
- Update database credentials in application.yml
- Ensure MySQL is running on port 3306
- Check database permissions

### Issue 3: Keycloak Provider Issues
**Error:** Provider not loading or authentication failures

**Solution:**
- Verify JAR is in Keycloak providers directory
- Check Keycloak logs for provider registration
- Ensure database connection from Keycloak

### Issue 4: BCrypt Password Mismatch
**Error:** Login fails for sample users

**Solution:**
- Update sample_data.sql with properly hashed passwords
- Or implement plain password fallback in EmployeeDao

## ✅ Success Criteria

- [ ] Application starts without errors
- [ ] MyBatis mappers are registered and logged
- [ ] Database queries execute successfully
- [ ] SQL logging shows parameter binding
- [ ] API endpoints return valid responses
- [ ] Transaction rollback works on errors
- [ ] Keycloak provider authenticates users
- [ ] No JPA/Hibernate dependencies remain

## 📊 Performance Verification

Compare before/after metrics:
- Application startup time
- Database query performance
- Memory usage
- SQL execution plans

MyBatis should show:
- Faster startup (no ORM metadata processing)
- Predictable SQL execution
- Lower memory footprint
- Explicit query optimization opportunities