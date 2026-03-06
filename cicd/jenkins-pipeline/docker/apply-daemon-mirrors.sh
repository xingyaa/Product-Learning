#!/bin/bash
# 将国内 registry-mirrors 合并进 /etc/docker/daemon.json，保留已有 dns、insecure-registries 等
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXAMPLE="${SCRIPT_DIR}/daemon.json.example"
TARGET="/etc/docker/daemon.json"
MIRRORS='["https://hub.rat.dev","https://docker.m.daocloud.io","https://docker.xuanyuan.me","https://mirror.ccs.tencentyun.com"]'

if [ "$(id -u)" -ne 0 ]; then
  echo "请使用 sudo 执行: sudo $0"
  exit 1
fi

mkdir -p /etc/docker
if [ -f "$TARGET" ]; then
  cp -a "$TARGET" "${TARGET}.bak"
  echo "已备份: ${TARGET}.bak"
fi

# 使用 jq 合并；若无 jq 则用 Python
if command -v jq &>/dev/null; then
  if [ -f "$TARGET" ]; then
    jq --argjson m "$MIRRORS" '. + {"registry-mirrors": $m}' "$TARGET" > "${TARGET}.tmp"
  else
    jq -n --argjson m "$MIRRORS" '{registry-mirrors: $m}' > "${TARGET}.tmp"
  fi
  mv "${TARGET}.tmp" "$TARGET"
else
  python3 << PY
import json, os
m = $MIRRORS
path = '$TARGET'
data = {}
if os.path.exists(path):
    with open(path) as f:
        data = json.load(f)
data['registry-mirrors'] = m
with open(path, 'w') as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
PY
fi

echo "已写入 registry-mirrors 到 $TARGET"
systemctl daemon-reload 2>/dev/null || true
systemctl restart docker 2>/dev/null || true
echo "已执行 daemon-reload 与 restart docker。"
