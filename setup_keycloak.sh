#!/bin/bash

# Setup Keycloak for Employee Management System Testing
export KEYCLOAK_URL="http://localhost:8080"
export ADMIN_USER="admin"
export ADMIN_PASSWORD="admin"

echo "🔐 Setting up Keycloak for Employee Management System..."

# Get admin token
echo "Getting admin access token..."
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASSWORD" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo "❌ Failed to get admin token. Is Keycloak running?"
  exit 1
fi

echo "✅ Admin token obtained"

# Create realm
echo "Creating employee-realm..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "realm": "employee-realm",
    "displayName": "Employee Management Realm",
    "enabled": true,
    "registrationAllowed": false,
    "loginWithEmailAllowed": true,
    "duplicateEmailsAllowed": false,
    "resetPasswordAllowed": true,
    "editUsernameAllowed": false,
    "bruteForceProtected": true
  }' > /dev/null

echo "✅ Realm created"

# Create client for the Employee Management API
echo "Creating employee-api-client..."
curl -s -X POST "$KEYCLOAK_URL/admin/realms/employee-realm/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "employee-api-client",
    "name": "Employee API Client",
    "enabled": true,
    "protocol": "openid-connect",
    "publicClient": false,
    "standardFlowEnabled": true,
    "directAccessGrantsEnabled": true,
    "serviceAccountsEnabled": true,
    "authorizationServicesEnabled": false,
    "secret": "employee-api-client-secret",
    "redirectUris": ["http://localhost:8088/*"],
    "webOrigins": ["http://localhost:8088"]
  }' > /dev/null

echo "✅ Client created"

# Create realm roles
echo "Creating realm roles..."
for role in "ADMIN" "MANAGER" "HR" "EMPLOYEE"; do
  curl -s -X POST "$KEYCLOAK_URL/admin/realms/employee-realm/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"$role\", \"description\": \"$role role for employees\"}" > /dev/null
done

# Create client roles
echo "Creating client roles..."
CLIENT_UUID=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/employee-realm/clients?clientId=employee-api-client" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

for role in "READ_EMPLOYEES" "CREATE_EMPLOYEES" "UPDATE_EMPLOYEES" "DELETE_EMPLOYEES" "FULL_ACCESS" "CLIENT_READ" "CLIENT_WRITE"; do
  curl -s -X POST "$KEYCLOAK_URL/admin/realms/employee-realm/clients/$CLIENT_UUID/roles" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"$role\", \"description\": \"$role client role\"}" > /dev/null
done

echo "✅ Roles created"

# Create test users
echo "Creating test users..."

# Test Admin User
curl -s -X POST "$KEYCLOAK_URL/admin/realms/employee-realm/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@company.com",
    "email": "admin@company.com",
    "firstName": "Admin",
    "lastName": "User",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{"type": "password", "value": "admin123", "temporary": false}]
  }' > /dev/null

# Test Employee User
curl -s -X POST "$KEYCLOAK_URL/admin/realms/employee-realm/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user@company.com",
    "email": "user@company.com",
    "firstName": "Test",
    "lastName": "User",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{"type": "password", "value": "user123", "temporary": false}]
  }' > /dev/null

echo "✅ Users created"

echo ""
echo "🎉 Keycloak setup completed!"
echo ""
echo "📋 Test Users Created:"
echo "   Admin: admin@company.com / admin123"
echo "   User:  user@company.com / user123"
echo ""
echo "🔗 URLs:"
echo "   Keycloak Admin: http://localhost:8080/admin"
echo "   Employee Realm: http://localhost:8080/realms/employee-realm"
echo ""
echo "🔑 Client Details:"
echo "   Client ID: employee-api-client"
echo "   Client Secret: employee-api-client-secret"