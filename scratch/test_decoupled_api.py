import requests
import json
import sys

BASE_URL = "http://localhost:8080"

def test_flow():
    print("=== 1. Test Static Frontend Assets ===")
    r = requests.get(f"{BASE_URL}/frontend/index.html")
    assert r.status_code == 200, f"index.html failed: {r.status_code}"
    print("  [OK] /frontend/index.html (200)")

    r = requests.get(f"{BASE_URL}/frontend/login.html")
    assert r.status_code == 200, f"login.html failed: {r.status_code}"
    print("  [OK] /frontend/login.html (200)")

    r = requests.get(f"{BASE_URL}/frontend/patient/dashboard.html")
    assert r.status_code == 200, f"patient dashboard failed: {r.status_code}"
    print("  [OK] /frontend/patient/dashboard.html (200)")

    print("\n=== 2. Test Unauthenticated Access to API ===")
    r = requests.get(f"{BASE_URL}/api/auth/me", headers={"Accept": "application/json"})
    assert r.status_code == 401, f"Expected 401, got {r.status_code}"
    print("  [OK] /api/auth/me returns 401 JSON when unauthenticated:", r.json())

    r = requests.get(f"{BASE_URL}/api/admin/dashboard", headers={"Accept": "application/json"})
    assert r.status_code == 401, f"Expected 401, got {r.status_code}"
    print("  [OK] /api/admin/dashboard returns 401 JSON when unauthenticated")

    print("\n=== 3. Test Admin Login & Endpoints ===")
    s_admin = requests.Session()
    login_data = {"username": "admin@hospital.com", "password": "admin123"}
    r = s_admin.post(f"{BASE_URL}/auth/login", data=login_data, headers={"Accept": "application/json"})
    assert r.status_code == 200, f"Admin login failed: {r.status_code} {r.text}"
    admin_info = r.json()
    print("  [OK] Admin login returned JSON:", admin_info)
    assert admin_info["role"] == "ROLE_ADMIN"

    r = s_admin.get(f"{BASE_URL}/api/auth/me")
    assert r.status_code == 200 and r.json()["role"] == "ROLE_ADMIN"
    print("  [OK] /api/auth/me verified for Admin:", r.json())

    r = s_admin.get(f"{BASE_URL}/api/admin/dashboard")
    assert r.status_code == 200
    print("  [OK] /api/admin/dashboard stats:", r.json())

    r = s_admin.get(f"{BASE_URL}/api/admin/doctors")
    assert r.status_code == 200
    docs = r.json()
    print(f"  [OK] /api/admin/doctors returned {len(docs)} doctors")

    r = s_admin.get(f"{BASE_URL}/api/admin/departments")
    assert r.status_code == 200
    depts = r.json()
    print(f"  [OK] /api/admin/departments returned {len(depts)} departments")

    print("\n=== 4. Test Doctor Login & Endpoints ===")
    s_doc = requests.Session()
    login_data = {"username": "dr.kumar@hospital.com", "password": "doctor123"}
    r = s_doc.post(f"{BASE_URL}/auth/login", data=login_data, headers={"Accept": "application/json"})
    assert r.status_code == 200, f"Doctor login failed: {r.status_code} {r.text}"
    doc_info = r.json()
    print("  [OK] Doctor login returned JSON:", doc_info)
    assert doc_info["role"] == "ROLE_DOCTOR"

    r = s_doc.get(f"{BASE_URL}/api/doctor/dashboard")
    assert r.status_code == 200
    print("  [OK] /api/doctor/dashboard:", r.json()["doctorName"], "Total today:", r.json()["todayTotal"])

    r = s_doc.get(f"{BASE_URL}/api/doctor/availability")
    assert r.status_code == 200
    print("  [OK] /api/doctor/availability returned schedule:", len(r.json()["availabilities"]), "slots")

    print("\n=== 5. Test Patient Registration, Login, Booking ===")
    s_patient = requests.Session()
    reg_data = {
        "fullName": "Decoupled Tester",
        "email": "tester_decoupled@test.com",
        "password": "password123",
        "phone": "9876543210",
        "dateOfBirth": "1995-05-15",
        "gender": "MALE",
        "address": "123 Rest API Street"
    }
    r = requests.post(f"{BASE_URL}/api/auth/register", json=reg_data)
    print("  Patient Registration response:", r.status_code, r.json())
    assert r.status_code == 200 or "already exists" in r.text

    # Login as patient
    login_data = {"username": "tester_decoupled@test.com", "password": "password123"}
    r = s_patient.post(f"{BASE_URL}/auth/login", data=login_data, headers={"Accept": "application/json"})
    assert r.status_code == 200, f"Patient login failed: {r.status_code} {r.text}"
    patient_info = r.json()
    print("  [OK] Patient login returned JSON:", patient_info)
    assert patient_info["role"] == "ROLE_PATIENT"

    r = s_patient.get(f"{BASE_URL}/api/patient/dashboard")
    assert r.status_code == 200
    print("  [OK] /api/patient/dashboard:", r.json())

    r = s_patient.get(f"{BASE_URL}/api/patient/doctors")
    assert r.status_code == 200
    doctors = r.json()
    print(f"  [OK] /api/patient/doctors returned {len(doctors)} doctors")

    # Check slots
    first_doc_id = doctors[0]["id"]
    r = s_patient.get(f"{BASE_URL}/api/patient/doctors/{first_doc_id}/slots?date=2026-09-21")
    assert r.status_code == 200
    print(f"  [OK] Doctor {first_doc_id} slots on 2026-09-21:", r.json())

    print("\n=== ALL TESTS PASSED! Decoupled REST API + Static Frontend fully functional ===")

if __name__ == "__main__":
    try:
        test_flow()
    except Exception as e:
        print("TEST FAILED:", e)
        sys.exit(1)
