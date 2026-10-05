import urllib.request
req=urllib.request.Request('http://localhost:8080/api/traffic/generate', data=b'{"count": 10}', headers={'Content-Type': 'application/json'})
print(urllib.request.urlopen(req).read().decode())
