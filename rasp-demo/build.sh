#!/bin/sh
# 在宿主机用 JDK 编译，生成 .class；之后可用 Dockerfile.single 构建镜像（仅需 JRE，避免拉取 JDK 镜像）
set -e
cd "$(dirname "$0")"
javac -encoding UTF-8 -d out DiagnosticsServer.java
echo "Done. Class files in out/"
