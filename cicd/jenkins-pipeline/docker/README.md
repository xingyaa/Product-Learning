# Docker 国内镜像配置

拉取 Docker Hub 镜像超时或 429 时，可配置国内 registry 镜像加速。本项目 Dockerfile 使用**短镜像名**（如 `eclipse-temurin:8-jre-alpine`），由 daemon 的 `registry-mirrors` 决定从哪个源拉取。

## 配置步骤

1. **备份并合并配置**（若已有 `/etc/docker/daemon.json`，请保留其中的 `dns`、`insecure-registries` 等字段）：

   ```bash
   sudo cp /etc/docker/daemon.json /etc/docker/daemon.json.bak 2>/dev/null || true
   # 将本目录下的 daemon.json.example 中的 registry-mirrors 合并进 /etc/docker/daemon.json
   ```

2. **或直接使用示例配置**（会覆盖现有 daemon.json，请先备份）：

   ```bash
   sudo cp cicd/docker/daemon.json.example /etc/docker/daemon.json
   # 若需保留原有 insecure-registries（如 172.16.21.97:9090），请编辑 daemon.json 手动加上
   ```

3. **重启 Docker**：

   ```bash
   sudo systemctl daemon-reload
   sudo systemctl restart docker
   ```

## 镜像源说明（当前示例顺序）

| 地址 | 说明 |
|------|------|
| https://hub.rat.dev | 耗子/毫秒镜像，当前多份资料验证可用 |
| https://docker.m.daocloud.io | DaoCloud 镜像站 |
| https://docker.xuanyuan.me | 轩辕免费版（若遇 429 可调至后面或移除） |
| https://mirror.ccs.tencentyun.com | 腾讯云，仅腾讯云服务器内推荐 |

若某一源不可用，Docker 会按顺序尝试下一个。可根据本机网络情况调整顺序或增删。最新列表可参考：[腾讯云开发者 - Docker 国内镜像](https://cloud.tencent.com/developer/article/2485043)。
