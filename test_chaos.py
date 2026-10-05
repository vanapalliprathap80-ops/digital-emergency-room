import urllib.request
import json
import time

# 1. Inject Database failure
req_chaos = urllib.request.Request('http://localhost:8080/api/incidents/manual', data=b'{"sessionId":"s1", "service": "DATABASE", "severity":"CRITICAL", "failureType":"DATABASE_CONNECTION_POOL_EXHAUSTION", "trafficPattern":"NORMAL"}', headers={'Content-Type': 'application/json'})
res_chaos = urllib.request.urlopen(req_chaos)
incident = json.loads(res_chaos.read().decode())
print("Incident injected:", incident['incidentId'])

# 2. Generate traffic
req_traffic = urllib.request.Request('http://localhost:8080/api/traffic/generate', data=b'{"count": 10}', headers={'Content-Type': 'application/json'})
print("Traffic result:", urllib.request.urlopen(req_traffic).read().decode())

# 3. Get metrics
req_metrics = urllib.request.Request('http://localhost:8080/api/metrics')
print("Metrics:", urllib.request.urlopen(req_metrics).read().decode())
