#!/bin/bash
# 查看本地 Docker Registry 的脚本

REGISTRY_URL="http://localhost:5000"

echo "=========================================="
echo "本地 Docker Registry 查看工具"
echo "Registry 地址: $REGISTRY_URL"
echo "=========================================="

# 1. 检查 Registry 是否运行
echo -e "\n[1] 检查 Registry 状态"
if curl -fsS "$REGISTRY_URL/v2/" >/dev/null 2>&1; then
    echo "✓ Registry 正在运行"
else
    echo "✗ Registry 未运行或无法访问"
    exit 1
fi

# 2. 查看所有镜像列表
echo -e "\n[2] 镜像列表"
CATALOG=$(curl -s "$REGISTRY_URL/v2/_catalog")
REPOS=$(echo "$CATALOG" | python3 -c "import sys, json; data=json.load(sys.stdin); print('\n'.join(data.get('repositories', [])))" 2>/dev/null)

if [ -z "$REPOS" ]; then
    echo "  (Registry 为空，暂无镜像)"
else
    echo "$REPOS" | while read -r repo; do
        echo "  - $repo"
        
        # 3. 查看每个镜像的标签（版本）
        echo "    标签:"
        TAGS=$(curl -s "$REGISTRY_URL/v2/$repo/tags/list" 2>/dev/null | python3 -c "import sys, json; data=json.load(sys.stdin); print('\n'.join(data.get('tags', [])))" 2>/dev/null)
        if [ -n "$TAGS" ]; then
            echo "$TAGS" | sed 's/^/      - /'
        else
            echo "      (无标签)"
        fi
    done
fi

# 4. 显示完整的 JSON 格式（如果需要）
echo -e "\n[3] 完整 JSON 格式"
echo "$CATALOG" | python3 -m json.tool 2>/dev/null || echo "$CATALOG"

# 5. 使用说明
echo -e "\n[4] 常用命令"
echo "  查看镜像列表: curl -s $REGISTRY_URL/v2/_catalog | python3 -m json.tool"
echo "  查看镜像标签: curl -s $REGISTRY_URL/v2/<镜像名>/tags/list | python3 -m json.tool"
echo "  拉取镜像: docker pull localhost:5000/<镜像名>:<标签>"
echo ""
echo "=========================================="
