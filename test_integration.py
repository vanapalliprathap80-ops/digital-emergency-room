import urllib.request
import json
import time

BASE_URL = "http://localhost:8080/api"

def reset_active_incident():
    req = urllib.request.Request(f"{BASE_URL}/incidents/system-state")
    try:
        response = urllib.request.urlopen(req)
        state = json.loads(response.read().decode())
        if state.get("activeIncidentId"):
            active_id = state["activeIncidentId"]
            print(f"Resetting existing active incident: {active_id}")
            reset_req = urllib.request.Request(f"{BASE_URL}/incidents/{active_id}/reset", method="POST")
            urllib.request.urlopen(reset_req)
    except Exception as e:
        print(f"Failed to reset active incident: {e}")

def create_incident():
    reset_active_incident()
    print("Creating manual incident: DATABASE_QUERY_LATENCY")
    req = urllib.request.Request(f"{BASE_URL}/incidents/manual", method="POST")
    req.add_header("Content-Type", "application/json")
    data = json.dumps({"failureType": "DATABASE_QUERY_LATENCY", "severity": "HIGH", "service": "DATABASE"}).encode("utf-8")
    try:
        response = urllib.request.urlopen(req, data=data)
        res_data = json.loads(response.read().decode())
        print(f"Created incident: {res_data['incidentId']}")
        return res_data['incidentId']
    except Exception as e:
        print(f"Failed to create incident: {e}")
        return None

def start_investigation(incident_id):
    print(f"Starting investigation for incident: {incident_id}")
    req = urllib.request.Request(f"{BASE_URL}/investigations/{incident_id}/start", method="POST")
    try:
        response = urllib.request.urlopen(req, data=b'')
        res_data = json.loads(response.read().decode())
        print(f"Started investigation: {res_data['id']}")
        return res_data['id']
    except Exception as e:
        print(f"Failed to start investigation: {e}")
        return None

def wait_for_investigation(investigation_id):
    print("Waiting for investigation to complete...")
    for _ in range(60): # 60 seconds
        req = urllib.request.Request(f"{BASE_URL}/investigations/{investigation_id}")
        try:
            response = urllib.request.urlopen(req)
            data = json.loads(response.read().decode())
            status = data['investigation']['status']
            print(f"Status: {status}")
            if status in ["COMPLETED", "FAILED", "INSUFFICIENT_EVIDENCE"]:
                return data['investigation']
            time.sleep(2)
        except Exception as e:
            print(f"Failed to fetch status: {e}")
            return None
    print("Investigation timed out")
    return None

def run_test():
    incident_id = create_incident()
    if not incident_id: return
    
    time.sleep(2) # Give it a second to reflect in telemetry
    
    investigation_id = start_investigation(incident_id)
    if not investigation_id: return
    
    result = wait_for_investigation(incident_id)
    if not result: return
    
    print("\n--- Investigation Result ---")
    print(json.dumps(result, indent=2))
    
    if result['status'] != "COMPLETED":
        print(f"TEST FAILED: Expected status COMPLETED, got {result['status']}")
        return
        
    if not result.get('diagnosis'):
        print("TEST FAILED: Missing diagnosis")
        return
        
    print("Investigation completed successfully!")
    
    print("\nChecking if remediation was proposed...")
    req = urllib.request.Request(f"{BASE_URL}/remediations?incidentId={incident_id}")
    try:
        response = urllib.request.urlopen(req)
        rems = json.loads(response.read().decode())
        print(json.dumps(rems, indent=2))
        
        if rems:
            for r in rems:
                if r['status'] == 'APPROVED':
                    print(f"Remediation {r['id']} was APPROVED.")
                else:
                    print(f"Remediation {r['id']} status: {r['status']}")
        else:
            print("No remediations found.")
    except Exception as e:
        print(f"Failed to fetch remediations: {e}")

if __name__ == "__main__":
    run_test()
