# Jenkins CI/CD 流水线 — 容器安全测试靶标

本目录是一套完整的 CI/CD 演示环境，用于 **构建一个故意包含多种安全隐患的 Docker 靶标镜像**，并通过 Jenkins 自动完成 **构建 → 验证 → 推送 Harbor** 的全流程。

> ⚠️ 靶标镜像仅限安全测试环境使用，切勿部署到生产环境！

---

## 靶标镜像包含哪些安全问题？

容器安全平台应能检测到以下全部或大部分问题：

| 类别 | 恶意样本 / 不安全配置 | 检测点 |
|------|----------------------|--------|
| **Web后门** | PHP Webshell (`shell.php`)、JSP Webshell (`cmd.jsp`)、Python Webshell (`eval_shell.py`)、ASPX Webshell (`aspx_cmd.aspx`) | 文件特征：`eval`/`exec`/`Runtime.exec`/`shell_exec` |
| **反弹Shell** | Bash/Netcat/Python/Perl 反弹脚本 (`reverse_shell.sh`) | `/dev/tcp`、`nc -e`、`socket.connect` |
| **Bind Shell** | Python 绑定端口后门 (`bind_shell.py`，端口 31337) | 异常监听端口 |
| **Rootkit** | C 语言 rootkit loader (`rootkit_loader.c`) | `ptrace`、`LD_PRELOAD`、进程隐藏 |
| **挖矿** | XMRig 配置 + 启动脚本 | 矿池域名 `minexmr.com`、钱包地址 |
| **提权脚本** | SUID 利用、passwd/shadow/sudoers 修改 | 危险命令 `chmod +s`、`/etc/passwd` 写入 |
| **数据窃取** | 敏感文件收集 + 外传脚本 | 读取 `/etc/shadow`、SSH 私钥、K8s Token |
| **容器逃逸** | Docker Socket 利用、特权模式逃逸、K8s Token 窃取 | 访问 `docker.sock`、`nsenter` |
| **恶意Cron** | 持久化下载执行 + 挖矿 + 反弹Shell 定时任务 | `/etc/cron.d/` 可疑条目 |
| **SSH后门** | 植入攻击者公钥、允许 root+空密码登录 | `authorized_keys` 异常、`PermitRootLogin yes` |
| **凭据泄露** | AWS Key、DB 密码、JWT Secret 硬编码到 ENV 和 `.env` 文件 | 环境变量/文件中的密钥模式 |
| **不安全配置** | Nginx 目录遍历、服务器版本暴露、root 运行 | `autoindex on`、`USER root` |
| **危险工具** | 安装 nmap、netcat、gcc | 容器内不应存在的攻击工具 |
| **应用漏洞** | 命令注入、任意文件读取、SSRF、不安全反序列化 | Web 应用运行时漏洞 |

---

## 目录结构

```
jenkins-pipeline/
├── README.md                              ← 本文件
├── Jenkinsfile                            ← CI/CD Pipeline（构建→验证→推送 Harbor）
├── Dockerfile                             ← 靶标镜像构建文件
├── cicd_test_app.py                       ← 靶标 Web 应用（含漏洞端点）
├── requirements.txt                       ← Python 依赖
├── malicious-samples/                     ← 恶意样本文件
│   ├── webshells/                         ← Web 后门
│   │   ├── shell.php                      ←   PHP 一句话木马
│   │   ├── cmd.jsp                        ←   JSP 命令执行
│   │   ├── eval_shell.py                  ←   Python eval webshell
│   │   └── aspx_cmd.aspx                  ←   ASPX 命令 shell
│   ├── backdoors/                         ← 后门程序
│   │   ├── reverse_shell.sh               ←   多种反弹 Shell
│   │   ├── bind_shell.py                  ←   绑定端口后门
│   │   ├── rootkit_loader.c               ←   Rootkit loader
│   │   └── ssh_authorized_keys            ←   植入的 SSH 公钥
│   ├── miners/                            ← 挖矿相关
│   │   ├── xmrig_config.json              ←   矿池配置
│   │   └── start_miner.sh                 ←   挖矿启动脚本
│   ├── scripts/                           ← 攻击脚本
│   │   ├── privilege_escalation.sh        ←   提权脚本
│   │   ├── data_exfiltration.py           ←   数据窃取
│   │   └── container_escape.sh            ←   容器逃逸
│   ├── configs/                           ← 不安全配置
│   │   ├── exposed_env                    ←   泄露的凭据
│   │   ├── insecure_sshd_config           ←   不安全 SSH 配置
│   │   └── weak_nginx.conf                ←   不安全 Nginx 配置
│   └── crontabs/                          ← 恶意定时任务
│       └── malicious_cron                 ←   持久化 Cron
├── Jenkinsfile.local                      ← 本地版 Pipeline
├── .dockerignore
├── test_jenkins_client.py
├── disable_script_security.groovy
├── fix_jenkins_timezone.sh
├── verify_pipeline.sh
├── view_registry.sh
└── docker/                                ← Docker 镜像加速配置
```

---

## 快速使用：让 Jenkins 自动完成 CI/CD

### 前提条件

1. Jenkins 已运行（Docker 方式）
2. 有一个 Harbor 镜像仓库
3. Jenkins 容器能访问 Docker（挂载了 `docker.sock`）

### 第一步：启动 Jenkins（如果还没启动）

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

### 第二步：在 Jenkins 中配置 Harbor 凭据

1. 打开 http://localhost:8080
2. 进入 **管理 Jenkins → 凭据管理 → 系统 → 全局凭据**
3. 点击 **添加凭据**：
   - 类型：**Username with password**
   - 用户名：Harbor 用户名（如 `admin`）
   - 密码：Harbor 密码
   - ID：填写 `harbor-credentials`（必须和 Jenkinsfile 中一致）
   - 描述：Harbor Registry

### 第三步：修改 Jenkinsfile 中的 Harbor 地址

打开 `Jenkinsfile`，修改顶部的环境变量：

```groovy
HARBOR_URL     = '你的Harbor地址:端口'    // 如 192.168.1.100 或 harbor.example.com
HARBOR_PROJECT = 'security-test'          // Harbor 中的项目名（需提前创建）
```

### 第四步：创建 Pipeline 任务

1. Jenkins 首页 → **新建任务**
2. 任务名：`vuln-target-pipeline`
3. 类型：**流水线 (Pipeline)**
4. 在 Pipeline 配置中：
   - 选择 **Pipeline script**
   - 将 `Jenkinsfile` 内容粘贴进去
5. **保存**

### 第五步：点击「立即构建」

Pipeline 会自动执行以下阶段：

```
准备代码 → 构建靶标镜像 → 验证镜像 → 安全扫描摘要 → 推送到 Harbor → 清理
```

在 **控制台输出** 中可以看到每个阶段的详细日志。

---

## 构建完成后

### 从 Harbor 拉取镜像

```bash
docker pull 你的Harbor地址/security-test/vuln-target-app:latest
```

### 本地运行靶标

```bash
docker run -d -p 8000:8000 --name vuln-target vuln-target-app:latest
```

### 测试漏洞端点

```bash
# 应用首页（查看所有端点）
curl http://localhost:8000/

# 命令注入
curl "http://localhost:8000/vuln/cmd?cmd=whoami"
curl "http://localhost:8000/vuln/cmd?cmd=cat%20/etc/passwd"

# 任意文件读取
curl "http://localhost:8000/vuln/file?path=/etc/shadow"
curl "http://localhost:8000/vuln/file?path=/root/.ssh/authorized_keys"

# SSRF
curl "http://localhost:8000/vuln/ssrf?url=http://169.254.169.254/latest/meta-data/"

# 环境变量泄露（含硬编码密码）
curl http://localhost:8000/vuln/env

# 系统信息泄露
curl http://localhost:8000/vuln/process
```

### 让容器安全平台扫描

1. 在 Harbor 中触发镜像扫描（Trivy / Clair）
2. 或使用容器运行时安全工具监控运行中的容器
3. 预期检测到：恶意文件、危险配置、硬编码密钥、已知 CVE、危险工具等

---

## 常见问题

| 问题 | 解决 |
|------|------|
| Jenkins 找不到源代码 | 确保启动时加了 `-v /root/python-sdk-main:/var/jenkins_home/workspace/python-sdk-main` |
| Harbor 登录失败 | 检查凭据 ID 是否为 `harbor-credentials`；确认 Harbor 地址正确 |
| docker push 报 HTTPS 错误 | Harbor 如果用 HTTP，需在 Docker 的 `daemon.json` 中加 `insecure-registries` |
| Script Security 报错 | 在 Script Console 执行 `disable_script_security.groovy` |
| 构建很慢 | 运行 `docker/apply-daemon-mirrors.sh` 配置国内镜像加速 |

---

## 备注

- Jenkins 账号：`admin/admin`
- 靶标应用端口：`8000`
- 所有恶意样本文件头部都有 `[TEST SAMPLE]` 标记
- 镜像故意使用 `python:3.9-slim`（较旧版本，可能有已知 CVE）
