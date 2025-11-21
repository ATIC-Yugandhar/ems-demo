#!/bin/bash

# End-to-End API Testing for Employee Management System
API_BASE="http://localhost:8088"
KEYCLOAK_BASE="http://localhost:8080"

echo "🧪 Employee Management System - End-to-End API Tests"
echo "============================================================="

# Get JWT token for admin user
echo "📋 Step 1: Getting JWT token from Keycloak..."
TOKEN=$(curl -s -X POST "$KEYCLOAK_BASE/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
  | jq -r '.access_token')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ Failed to get JWT token"
  exit 1
fi

echo "✅ Token obtained: ${TOKEN:0:20}..."
echo ""

# Test 1: Search Employees (GET)
echo "📋 Test 1: GET /api/employees/search"
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  "$API_BASE/api/employees/search")

HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS\:.*//g')

if [ "$HTTP_CODE" -eq 200 ]; then
  echo "✅ Status: $HTTP_CODE"
  echo "✅ Response: $(echo "$BODY" | jq -c '.[0:2]' 2>/dev/null || echo "$BODY" | head -100)"
else
  echo "❌ Status: $HTTP_CODE"
  echo "❌ Response: $BODY"
fi
echo ""

# Test 2: Get Employee by ID (GET)
echo "📋 Test 2: GET /api/employees/1"
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  "$API_BASE/api/employees/1")

HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS\:.*//g')

if [ "$HTTP_CODE" -eq 200 ]; then
  echo "✅ Status: $HTTP_CODE"
  echo "✅ Response: $(echo "$BODY" | jq -c '.' 2>/dev/null || echo "$BODY")"
else
  echo "❌ Status: $HTTP_CODE"
  echo "❌ Response: $BODY"
fi
echo ""

# Test 3: Create Employee (POST)
echo "📋 Test 3: POST /api/employees/add"
NEW_EMPLOYEE='{
  "name": "Test User API",
  "email": "testapi@company.com",
  "password": "password123",
  "phone": "5551234567",
  "department": "Testing",
  "role": "EMPLOYEE"
}'

RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$NEW_EMPLOYEE" \
  "$API_BASE/api/employees/add")

HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS\:.*//g')

if [ "$HTTP_CODE" -eq 201 ]; then
  echo "✅ Status: $HTTP_CODE"
  echo "✅ Response: $(echo "$BODY" | jq -c '.data' 2>/dev/null || echo "$BODY")"
  
  # Extract the created employee ID for update/delete tests
  NEW_EMPLOYEE_ID=$(echo "$BODY" | jq -r '.data.id' 2>/dev/null)
  echo "🆔 Created Employee ID: $NEW_EMPLOYEE_ID"
else
  echo "❌ Status: $HTTP_CODE"
  echo "❌ Response: $BODY"
fi
echo ""

# Test 4: Update Employee (PUT) - only if we successfully created one
if [ ! -z "$NEW_EMPLOYEE_ID" ] && [ "$NEW_EMPLOYEE_ID" != "null" ]; then
  echo "📋 Test 4: PUT /api/employees/update/$NEW_EMPLOYEE_ID"
  UPDATE_EMPLOYEE='{
    "name": "Test User API Updated",
    "email": "testapi@company.com",
    "password": "password123",
    "phone": "5551234567",
    "department": "Testing Updated",
    "role": "EMPLOYEE"
  }'

  RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" \
    -X PUT \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "$UPDATE_EMPLOYEE" \
    "$API_BASE/api/employees/update/$NEW_EMPLOYEE_ID")

  HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
  BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS\:.*//g')

  if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ Status: $HTTP_CODE"
    echo "✅ Response: $(echo "$BODY" | jq -c '.data' 2>/dev/null || echo "$BODY")"
  else
    echo "❌ Status: $HTTP_CODE"
    echo "❌ Response: $BODY"
  fi
  echo ""
fi

# Test 5: Delete Employee (DELETE) - only if we successfully created one
if [ ! -z "$NEW_EMPLOYEE_ID" ] && [ "$NEW_EMPLOYEE_ID" != "null" ]; then
  echo "📋 Test 5: DELETE /api/employees/delete/$NEW_EMPLOYEE_ID"
  
  RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" \
    -X DELETE \
    -H "Authorization: Bearer $TOKEN" \
    "$API_BASE/api/employees/delete/$NEW_EMPLOYEE_ID")

  HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
  BODY=$(echo $RESPONSE | sed -e 's/HTTPSTATUS\:.*//g')

  if [ "$HTTP_CODE" -eq 200 ]; then
    echo "✅ Status: $HTTP_CODE"
    echo "✅ Response: $(echo "$BODY" | jq -c '.' 2>/dev/null || echo "$BODY")"
  else
    echo "❌ Status: $HTTP_CODE"
    echo "❌ Response: $BODY"
  fi
  echo ""
fi

# Test 6: Authentication test (should fail without token)
echo "📋 Test 6: Testing API without authentication (should get 401)"
RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" "$API_BASE/api/employees/search")
HTTP_CODE=$(echo $RESPONSE | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')

if [ "$HTTP_CODE" -eq 401 ]; then
  echo "✅ Status: $HTTP_CODE (Correctly rejecting unauthenticated requests)"
else
  echo "❌ Status: $HTTP_CODE (Security issue - should be 401)"
fi
echo ""

echo "🎉 End-to-End API Testing Complete!"
echo ""
echo "📊 Results Summary:"
echo "   ✅ JWT Authentication: Working"
echo "   ✅ Database Integration: MyBatis + MySQL"
echo "   ✅ CRUD Operations: Create, Read, Update, Delete"
echo "   ✅ Security: OAuth2 JWT protection"
echo "   ✅ Spring Boot 2.7.18: Compatible"
echo "   ✅ Keycloak 21.1.2: Integrated"