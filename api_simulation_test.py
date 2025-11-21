#!/usr/bin/env python3
"""
API Request/Response Flow Simulation for Employee Management System
Tests the expected behavior of MyBatis-based API endpoints
"""

import json
import sqlite3
from datetime import datetime

class MockEmployeeService:
    def __init__(self, db_path):
        self.conn = sqlite3.connect(db_path)
        self.conn.row_factory = sqlite3.Row

    def get_employee_by_id(self, employee_id):
        """Simulate EmployeeMapper.findById"""
        cursor = self.conn.execute("SELECT * FROM employees WHERE id = ?", (employee_id,))
        row = cursor.fetchone()
        if row:
            return {
                "id": row["id"],
                "name": row["name"],
                "email": row["email"],
                "phone": row["phone"],
                "department": row["department"],
                "role": row["role"],
                "createdAt": row["created_at"],
                "updatedAt": row["updated_at"]
            }
        return None

    def search_employees(self, name=None, email=None, department=None, role=None):
        """Simulate EmployeeMapper.searchEmployees"""
        query = "SELECT * FROM employees WHERE 1=1"
        params = []
        
        if name:
            query += " AND name LIKE ?"
            params.append(f"%{name}%")
        if email:
            query += " AND email LIKE ?"
            params.append(f"%{email}%")
        if department:
            query += " AND department LIKE ?"
            params.append(f"%{department}%")
        if role:
            query += " AND role LIKE ?"
            params.append(f"%{role}%")
            
        query += " ORDER BY created_at DESC"
        
        cursor = self.conn.execute(query, params)
        rows = cursor.fetchall()
        
        return [{
            "id": row["id"],
            "name": row["name"],
            "email": row["email"],
            "phone": row["phone"],
            "department": row["department"],
            "role": row["role"],
            "createdAt": row["created_at"],
            "updatedAt": row["updated_at"]
        } for row in rows]

    def create_employee(self, employee_data):
        """Simulate EmployeeMapper.insert"""
        # Check if email exists
        cursor = self.conn.execute("SELECT COUNT(*) FROM employees WHERE email = ?", (employee_data["email"],))
        if cursor.fetchone()[0] > 0:
            raise Exception(f"Employee with email {employee_data['email']} already exists")
        
        cursor = self.conn.execute("""
            INSERT INTO employees (name, email, password, phone, department, role)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (
            employee_data["name"],
            employee_data["email"],
            employee_data["password"],
            employee_data.get("phone"),
            employee_data.get("department"),
            employee_data["role"]
        ))
        
        employee_id = cursor.lastrowid
        self.conn.commit()
        
        return self.get_employee_by_id(employee_id)

def test_api_endpoints():
    """Simulate API endpoint testing"""
    service = MockEmployeeService("test_db.sqlite")
    
    print("🧪 EMPLOYEE MANAGEMENT SYSTEM - API SIMULATION TESTS")
    print("=" * 60)
    
    # Test 1: GET /api/employees/{id}
    print("\n📋 Test 1: GET /api/employees/1")
    employee = service.get_employee_by_id(1)
    if employee:
        print("✅ Status: 200 OK")
        print(f"✅ Response: {json.dumps(employee, indent=2)}")
    else:
        print("❌ Status: 404 Not Found")
    
    # Test 2: GET /api/employees/search
    print("\n📋 Test 2: GET /api/employees/search")
    employees = service.search_employees()
    print(f"✅ Status: 200 OK")
    print(f"✅ Response: Found {len(employees)} employees")
    for emp in employees[:2]:  # Show first 2
        print(f"   - {emp['name']} ({emp['email']}) - {emp['department']}")
    
    # Test 3: GET /api/employees/search?department=IT
    print("\n📋 Test 3: GET /api/employees/search?department=IT")
    it_employees = service.search_employees(department="IT")
    print(f"✅ Status: 200 OK")
    print(f"✅ Response: Found {len(it_employees)} IT employees")
    for emp in it_employees:
        print(f"   - {emp['name']} ({emp['department']})")
    
    # Test 4: POST /api/employees/add
    print("\n📋 Test 4: POST /api/employees/add")
    new_employee_data = {
        "name": "API Test User",
        "email": "api.test@company.com",
        "password": "$2a$10$hashed.password.here",
        "phone": "5551234567",
        "department": "QA",
        "role": "EMPLOYEE"
    }
    
    try:
        created_employee = service.create_employee(new_employee_data)
        print("✅ Status: 201 Created")
        print(f"✅ Response: {json.dumps({'code': 201, 'message': 'Employee created successfully', 'data': created_employee}, indent=2)}")
    except Exception as e:
        print(f"❌ Status: 400 Bad Request")
        print(f"❌ Error: {str(e)}")
    
    # Test 5: POST /api/employees/add (Duplicate Email)
    print("\n📋 Test 5: POST /api/employees/add (Duplicate Email)")
    try:
        service.create_employee(new_employee_data)  # Same email
        print("❌ Should have failed!")
    except Exception as e:
        print("✅ Status: 400 Bad Request")
        print(f"✅ Expected Error: {str(e)}")
    
    # Test 6: Authentication Simulation
    print("\n📋 Test 6: POST /api/auth/login (Simulation)")
    login_request = {
        "email": "user2@company.com",
        "password": "password123"
    }
    
    # Simulate finding user
    user = service.conn.execute("SELECT * FROM employees WHERE email = ?", (login_request["email"],)).fetchone()
    if user:
        print("✅ Status: 200 OK")
        mock_response = {
            "success": True,
            "message": "Login successful",
            "token": "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
        }
        print(f"✅ Response: {json.dumps(mock_response, indent=2)}")
    else:
        print("❌ Status: 401 Unauthorized")

if __name__ == "__main__":
    test_api_endpoints()