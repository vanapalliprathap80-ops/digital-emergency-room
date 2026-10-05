import urllib.request
req=urllib.request.Request('http://localhost:8080/api/metrics')
print(urllib.request.urlopen(req).read().decode())
