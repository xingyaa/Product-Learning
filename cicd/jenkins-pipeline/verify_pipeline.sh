#!/bin/bash
# Jenkins Pipeline 验证脚本

echo "=========================================="
echo "Jenkins Pipeline 验证脚本"
echo "=========================================="

# 1. 检查 Jenkins 容器状态
echo -e "\n[1] 检查 Jenkins 容器状态"
docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E '^(NAMES|jenkins)'

# 2. 检查本地 Registry 状态
echo -e "\n[2] 检查本地 Registry 状态"
docker ps --format 'table {{.Names}}\t{{.Status}}' | grep -E '^(NAMES|local-registry)'

# 3. 检查 Registry 中的镜像
echo -e "\n[3] Registry 中的镜像列表"
curl -s http://localhost:5000/v2/_catalog | python3 -m json.tool 2>/dev/null || echo "Registry 无响应或为空"

# 4. 检查本地构建的 Docker 镜像
echo -e "\n[4] 本地 Docker 镜像（cicd-test-app）"
docker images | grep cicd-test-app || echo "未找到 cicd-test-app 镜像"

# 5. 检查挂载的代码目录
echo -e "\n[5] Jenkins 容器内的代码目录"
docker exec jenkins ls -la /var/jenkins_home/workspace/python-sdk-main/cicd/ 2>/dev/null | head -n 10 || echo "代码目录不存在"

# 6. 检查 Jenkins 工作空间
echo -e "\n[6] Jenkins 工作空间"
docker exec jenkins ls -la /var/jenkins_home/workspace/ 2>/dev/null | head -n 10 || echo "工作空间不存在"

# 7. 验证镜像是否可以运行
echo -e "\n[7] 测试镜像运行（如果存在）"
if docker images | grep -q "cicd-test-app.*latest"; then
    echo "尝试运行镜像..."
    docker run --rm -d --name test-verify -p 18081:8000 cicd-test-app:latest 2>/dev/null
    sleep 3
    if curl -fsS http://localhost:18081/health >/dev/null 2>&1; then
        echo "✓ 镜像运行成功，健康检查通过"
    else
        echo "✗ 镜像运行失败或健康检查失败"
    fi
    docker stop test-verify >/dev/null 2>&1
else
    echo "镜像不存在，跳过测试"
fi

echo -e "\n=========================================="
echo "验证完成"
echo "=========================================="
