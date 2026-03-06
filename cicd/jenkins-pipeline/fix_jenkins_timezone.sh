#!/bin/bash
# Jenkins 容器时区修复脚本（持久化方案）

set -e

CONTAINER_NAME="jenkins"
TIMEZONE="Asia/Shanghai"

echo "=========================================="
echo "Jenkins 容器时区修复脚本"
echo "=========================================="

# 检查容器是否存在
if ! docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "错误: 容器 ${CONTAINER_NAME} 不存在"
    exit 1
fi

# 方案1: 如果容器正在运行，直接修复（临时方案）
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "容器正在运行，执行临时修复..."
    docker exec -u root ${CONTAINER_NAME} bash -c "
        ln -sf /usr/share/zoneinfo/${TIMEZONE} /etc/localtime
        echo '${TIMEZONE}' > /etc/timezone
        dpkg-reconfigure -f noninteractive tzdata >/dev/null 2>&1 || true
    "
    echo "✓ 时区已临时修复（容器重启后会丢失）"
    docker exec ${CONTAINER_NAME} date
fi

echo ""
echo "=========================================="
echo "持久化方案（推荐）"
echo "=========================================="
echo ""
echo "要永久修复时区，需要重新启动容器并挂载时区文件："
echo ""
echo "1. 停止容器："
echo "   docker stop ${CONTAINER_NAME}"
echo ""
echo "2. 重新启动容器（添加时区挂载）："
echo "   docker start ${CONTAINER_NAME}"
echo "   docker update --env-add TZ=${TIMEZONE} ${CONTAINER_NAME} || \\"
echo "   docker run -d \\"
echo "     --name ${CONTAINER_NAME} \\"
echo "     -p 8080:8080 \\"
echo "     -p 50000:50000 \\"
echo "     -v jenkins_home:/var/jenkins_home \\"
echo "     -v /var/run/docker.sock:/var/run/docker.sock \\"
echo "     -v /etc/localtime:/etc/localtime:ro \\"
echo "     -v /etc/timezone:/etc/timezone:ro \\"
echo "     -e TZ=${TIMEZONE} \\"
echo "     jenkins/jenkins:lts"
echo ""
echo "或者使用 docker-compose，在 docker-compose.yml 中添加："
echo "  environment:"
echo "    - TZ=${TIMEZONE}"
echo "  volumes:"
echo "    - /etc/localtime:/etc/localtime:ro"
echo "    - /etc/timezone:/etc/timezone:ro"
echo ""
echo "=========================================="
