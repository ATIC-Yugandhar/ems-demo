#!/bin/bash

echo "🔐 Assigning client roles to service account..."

# Get admin token
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
  echo "❌ Failed to get admin token"
  exit 1
fi

echo "✅ Admin token obtained"

# Get client UUID
CLIENT_UUID=$(curl -s -X GET "http://localhost:8080/admin/realms/employee-realm/clients?clientId=employee-api-client" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

echo "✅ Client UUID: $CLIENT_UUID"

# Get service account user ID  
SERVICE_ACCOUNT_ID=$(curl -s -X GET "http://localhost:8080/admin/realms/employee-realm/users?username=service-account-employee-api-client" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

echo "✅ Service Account ID: $SERVICE_ACCOUNT_ID"

# Get client roles that need to be assigned
echo "Getting client roles..."
CLIENT_ROLES=$(curl -s -X GET "http://localhost:8080/admin/realms/employee-realm/clients/$CLIENT_UUID/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN")

echo "✅ Available client roles:"
echo "$CLIENT_ROLES" | jq -r '.[].name'

# Assign all client roles to the service account
for role in "CLIENT_READ" "CLIENT_WRITE" "FULL_ACCESS" "READ_EMPLOYEES" "CREATE_EMPLOYEES" "UPDATE_EMPLOYEES" "DELETE_EMPLOYEES"; do
  echo "Assigning role: $role"
  
  # Get the role details
  ROLE_DATA=$(echo "$CLIENT_ROLES" | jq ".[] | select(.name == \"$role\")")
  
  if [ "$ROLE_DATA" != "" ] && [ "$ROLE_DATA" != "null" ]; then
    # Assign the role to service account
    curl -s -X POST "http://localhost:8080/admin/realms/employee-realm/users/$SERVICE_ACCOUNT_ID/role-mappings/clients/$CLIENT_UUID" \
      -H "Authorization: Bearer $ADMIN_TOKEN" \
      -H "Content-Type: application/json" \
      -d "[$ROLE_DATA]" > /dev/null
    echo "✅ Assigned $role"
  else
    echo "⚠️  Role $role not found"
  fi
done

echo ""
echo "🎉 Client role assignment completed!"
echo ""
echo "✅ The service account now has all necessary client roles for API access"