# CI/CD 完整流程说明

## 总览

本项目演示一套完整的容器安全测试 CI/CD 流程：

```
编写 Python 靶标应用 → 本地测试验证 → Jenkins 自动构建镜像 → 推送到 Harbor 仓库
```

```
┌─────────────┐    ┌─────────────┐    ┌──────────────────┐    ┌─────────────┐
│  编写代码    │ →  │  本地测试    │ →  │  Jenkins 构建镜像 │ →  │  推送 Harbor │
│  (Python)   │    │ (虚拟环境)   │    │  (Docker build)  │    │  (Registry)  │
└─────────────┘    └─────────────┘    └──────────────────┘    └─────────────┘
```

> ⚠️ 靶标镜像故意包含多种安全隐患（webshell、挖矿、反弹shell、硬编码密码等），
> 用于验证容器安全平台的检测能力。**仅限测试环境使用！**

---

## 第一阶段：编写代码

所有 Jenkins CI/CD 相关代码位于 `cicd/jenkins-pipeline/` 目录。

### 靶标应用（`cicd_test_app.py`）

一个 FastAPI Web 应用，故意包含多种安全漏洞：

| 端点 | 漏洞类型 | 说明 |
|------|---------|------|
| `/` | - | 首页，列出所有端点 |
| `/health` | - | 健康检查 |
| `/info` | 信息泄露 | 暴露系统信息 + 硬编码密码 |
| `/vuln/cmd?cmd=id` | 命令注入 | 直接执行用户输入的命令 |
| `/vuln/file?path=/etc/passwd` | 任意文件读取 | 无路径校验 |
| `/vuln/ssrf?url=http://...` | SSRF | 服务端请求伪造 |
| `/vuln/pickle?data=base64` | 不安全反序列化 | 直接加载 pickle 数据 |
| `/vuln/env` | 环境变量泄露 | 暴露全部环境变量 |
| `/vuln/process` | 进程信息泄露 | 暴露内核/CPU/内存/挂载信息 |

### 恶意样本文件（`malicious-samples/`）

构建到镜像中的测试样本，覆盖常见攻击场景：

| 目录 | 内容 |
|------|------|
| `webshells/` | PHP/JSP/Python/ASPX 四种 Web 后门 |
| `backdoors/` | 反弹 Shell、Bind Shell、Rootkit、SSH 公钥后门 |
| `miners/` | 挖矿配置 + 启动脚本 |
| `scripts/` | 提权、数据窃取、容器逃逸脚本 |
| `configs/` | 泄露的凭据、不安全的 SSH/Nginx 配置 |
| `crontabs/` | 恶意定时任务（持久化+挖矿+反弹shell） |

### Dockerfile

镜像构建时会：
- 以 root 用户运行
- 安装 nmap、netcat、gcc 等危险工具
- 将 webshell 散布到 `/var/www/html/`
- 将后门脚本放到 `/tmp/.hidden/`
- 将挖矿文件放到 `/tmp/.miner/`
- 硬编码 AWS Key、DB 密码到环境变量
- 植入恶意 SSH 公钥和 Cron 任务

---

## 第二阶段：本地测试

### 环境准备

```bash
# 激活虚拟环境
cd /root/python-sdk-main
source venv/bin/activate
```

虚拟环境已安装的关键依赖：
- `fastapi` - Web 框架
- `uvicorn` - ASGI 服务器
- `python-jenkins` - Jenkins 客户端库

### 启动应用

```bash
cd cicd/jenkins-pipeline

# 前台运行（看日志）
python -m uvicorn cicd_test_app:app --host 0.0.0.0 --port 8000

# 或后台运行
nohup python -m uvicorn cicd_test_app:app --host 0.0.0.0 --port 8000 > /tmp/vuln-app.log 2>&1 &
```

### 验证测试

浏览器访问 `http://服务器IP:8000`，或用 curl 测试：

```bash
# 首页
curl http://localhost:8000/

# 命令注入
curl "http://localhost:8000/vuln/cmd?cmd=whoami"
curl "http://localhost:8000/vuln/cmd?cmd=id"

# 文件读取
curl "http://localhost:8000/vuln/file?path=/etc/passwd"

# 环境变量泄露（含硬编码密码）
curl http://localhost:8000/vuln/env

# 信息泄露
curl http://localhost:8000/info
```

确认所有端点正常后，进入下一阶段。

---

## 第三阶段：Jenkins 构建镜像

### 3.1 启动 Jenkins

```bash
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /root/python-sdk-main:/var/jenkins_home/workspace/python-sdk-main \
  jenkins/jenkins:lts
```

访问 `http://服务器IP:8080`，登录 Jenkins（账号：`admin/admin`）。

### 3.2 在 Jenkins 中配置 Harbor 凭据

1. **管理 Jenkins → 凭据管理 → 系统 → 全局凭据**
2. 点击 **添加凭据**：
   - 类型：Username with password
   - 用户名：Harbor 用户名（如 `admin`）
   - 密码：Harbor 密码
   - **ID：`harbor-credentials`**（必须和 Jenkinsfile 一致）

### 3.3 修改 Jenkinsfile 中的 Harbor 地址

打开 `cicd/jenkins-pipeline/Jenkinsfile`，修改顶部环境变量：

```groovy
HARBOR_URL     = '你的Harbor地址'         // 如 192.168.1.100
HARBOR_PROJECT = 'security-test'          // Harbor 中的项目名（需提前创建）
```

### 3.4 创建 Pipeline 任务

1. Jenkins 首页 → **新建任务**
2. 任务名：`vuln-target-pipeline` → 类型：**流水线 (Pipeline)** → 确定
3. Pipeline 配置 → **Pipeline script** → 粘贴 `Jenkinsfile` 内容
4. **保存**

### 3.5 点击「立即构建」

Pipeline 自动执行以下阶段：

```
┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌──────────────┐   ┌────────────┐   ┌──────┐
│ 准备代码  │ → │ 构建靶标镜像  │ → │ 验证镜像  │ → │ 安全扫描摘要  │ → │ 推送 Harbor │ → │ 清理  │
└──────────┘   └──────────────┘   └──────────┘   └──────────────┘   └────────────┘   └──────┘
```

| 阶段 | 做什么 |
|------|--------|
| 准备代码 | 从挂载目录复制 `jenkins-pipeline/` 到工作空间 |
| 构建靶标镜像 | `docker build` 构建含恶意样本的靶标镜像 |
| 验证镜像 | 启动容器，测试健康检查和漏洞端点 |
| 安全扫描摘要 | 输出镜像中的全部安全风险清单 |
| 推送 Harbor | 登录 Harbor → 打标签 → `docker push` |
| 清理 | 删除临时容器和工作空间 |

在 **构建历史 → 控制台输出** 中查看每个阶段的详细日志。

---

## 第四阶段：推送到 Harbor

Pipeline 自动完成推送，成功后可以：

```bash
# 从 Harbor 拉取镜像
docker pull Harbor地址/security-test/vuln-target-app:latest

# 运行靶标容器
docker run -d -p 8000:8000 --name vuln-target Harbor地址/security-test/vuln-target-app:latest
```

### 让容器安全平台扫描

1. **镜像扫描**：在 Harbor 中触发 Trivy/Clair 扫描，检测已知 CVE 和恶意文件
2. **运行时检测**：启动容器后，容器安全平台应检测到异常行为（挖矿连接、反弹shell、敏感文件访问等）
3. **配置审计**：检测 root 运行、危险端口暴露、硬编码密钥等不安全配置

---

## 文件清单

```
cicd/
├── CICD_README.md                         ← 本文件（整体流程说明）
├── jenkins-pipeline/                      ← Jenkins CI/CD 核心目录
│   ├── README.md                          ← Jenkins 详细使用说明
│   ├── Jenkinsfile                        ← Pipeline 定义（构建→验证→推送 Harbor）
│   ├── Dockerfile                         ← 靶标镜像构建文件
│   ├── cicd_test_app.py                   ← 靶标 Web 应用（含漏洞端点）
│   ├── requirements.txt                   ← Python 依赖
│   ├── malicious-samples/                 ← 恶意样本文件（webshell/后门/挖矿等）
│   ├── test_jenkins_client.py             ← Jenkins 连接测试
│   ├── disable_script_security.groovy     ← Script Security 自动批准
│   ├── fix_jenkins_timezone.sh            ← 时区修复
│   ├── verify_pipeline.sh                 ← 环境验证
│   ├── view_registry.sh                   ← 查看 Registry 镜像
│   └── docker/                            ← Docker 国内镜像加速
├── JENKINS_SETUP.md                       ← Jenkins Web 界面配置指南
├── DISABLE_SCRIPT_SECURITY.md             ← Script Security 禁用说明
└── java-app-test/                         ← OWASP 漏洞靶场（独立项目）
```

---

## 常见问题

| 问题 | 解决 |
|------|------|
| 本地测试 `Connection refused` | 确认用 `--host 0.0.0.0` 启动，不是 `127.0.0.1` |
| Jenkins 找不到代码 | 启动时必须加 `-v /root/python-sdk-main:/var/jenkins_home/workspace/python-sdk-main` |
| Harbor 登录失败 | 检查凭据 ID 是否为 `harbor-credentials`，用户名密码是否正确 |
| `docker push` 报 HTTPS 错误 | Harbor 用 HTTP 时，在 `/etc/docker/daemon.json` 加 `"insecure-registries": ["Harbor地址"]` |
| Script Security 报错 | 在 Script Console 执行 `disable_script_security.groovy` |
| Docker 拉取镜像慢 | 运行 `jenkins-pipeline/docker/apply-daemon-mirrors.sh` 配置国内镜像加速 |

---

## 备注

- Jenkins 镜像：`jenkins/jenkins:lts`（约 500MB）
- Jenkins 账号：`admin/admin`
- 初始密码：`3429920629ae4c91b3743a6f4a3e6a06`
- 虚拟环境路径：`/root/python-sdk-main/venv`
- 靶标应用端口：`8000`
- Jenkins 端口：`8080`
