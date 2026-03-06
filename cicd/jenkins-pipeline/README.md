# Jenkins CI/CD 流水线

本目录包含一套完整的 Jenkins CI/CD 演示环境，可独立运行，与 `rasp-demo` 无关。

流程概要：**用 Docker 启动 Jenkins → 在 Jenkins Web 页面创建 Pipeline 任务 → 自动构建/测试/推送镜像**。

---

## 快速回忆：你之前做了什么

1. 拉取并启动了 Jenkins Docker 镜像（`jenkins/jenkins:lts`，端口 8080）
2. 用初始密码完成 Jenkins 首次配置，账号 `admin/admin`
3. 在 Jenkins Web 界面创建了 Pipeline 任务，指向 `Jenkinsfile`
4. Pipeline 自动：构建 `cicd-test-app` Docker 镜像 → 运行容器验证健康检查 → 推送到本地 Registry

---

## 目录结构

```
jenkins-pipeline/
├── README.md                          ← 本文件
├── Jenkinsfile                        ← 主 Pipeline 定义（完整流程）
├── Jenkinsfile.local                  ← 本地版 Pipeline（使用本地挂载代码）
├── Dockerfile                         ← cicd-test-app 镜像构建（Python + FastAPI）
├── .dockerignore                      ← Docker 构建排除规则
├── cicd_test_app.py                   ← 测试应用（FastAPI，/health、/info）
├── requirements.txt                   ← Python 依赖
├── test_jenkins_client.py             ← Jenkins 客户端连接测试
├── disable_script_security.groovy     ← Jenkins Script Security 自动批准
├── fix_jenkins_timezone.sh            ← Jenkins 容器时区修复
├── verify_pipeline.sh                 ← 验证 Pipeline 环境和结果
├── view_registry.sh                   ← 查看本地 Registry 镜像
└── docker/                            ← Docker 国内镜像加速配置
    ├── README.md
    ├── daemon.json.example
    └── apply-daemon-mirrors.sh
```

---

## 从零开始：完整使用流程

### 第一步：启动 Jenkins

```bash
# 启动 Jenkins 容器（挂载 Docker socket，让 Jenkins 能构建镜像）
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts

# 查看初始管理员密码
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 第二步：首次配置 Jenkins（Web 界面）

1. 浏览器打开 **http://localhost:8080**（或 `http://服务器IP:8080`）
2. 输入上一步拿到的初始密码
3. 选择「安装推荐的插件」，等待安装完成
4. 创建管理员用户（如 `admin/admin`）
5. 确认 Jenkins URL

### 第三步：将代码挂载到 Jenkins 工作空间

Pipeline 默认从 `/var/jenkins_home/workspace/python-sdk-main/cicd` 读取代码：

```bash
# 把本地代码复制到 Jenkins 容器
docker cp /root/python-sdk-main/cicd/jenkins-pipeline/. \
  jenkins:/var/jenkins_home/workspace/python-sdk-main/cicd/
```

或在启动容器时直接挂载：

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

### 第四步：在 Jenkins Web 界面创建 Pipeline 任务

1. 登录 Jenkins → 点击 **「新建任务」**
2. 输入任务名（如 `cicd-test-pipeline`）→ 选择 **「流水线」(Pipeline)** → 确定
3. 在配置页面的 **「流水线」** 部分：

**方式 A：直接粘贴（快速测试）**
- 「定义」选 **Pipeline script**
- 把 `Jenkinsfile` 的内容粘贴进去
- 保存

**方式 B：从 SCM 获取（推荐，代码有变会自动更新）**
- 「定义」选 **Pipeline script from SCM**
- SCM 选 **Git**
- Repository URL：`https://github.com/xingyaa/Product-Learning.git`
- Script Path：`cicd/jenkins-pipeline/Jenkinsfile`
- 保存

4. 回到任务页面 → 点击 **「立即构建」**

### 第五步：查看构建结果

1. 在「构建历史」中点击构建号（如 `#1`）
2. 点击 **「控制台输出」** 查看完整日志
3. Pipeline 会依次执行：

| 阶段 | 做了什么 |
|------|---------|
| 准备代码 | 从工作空间复制 cicd 代码 |
| 构建环境检查 | 检查 docker 版本和状态 |
| 测试 | 运行 `test_jenkins_client.py` |
| 构建镜像 | `docker build -t cicd-test-app:${BUILD_NUMBER}` |
| 验证镜像 | 启动容器，访问 `/health` 健康检查 |
| 推送镜像 | 打标签推送到 `localhost:5000`（本地 Registry） |
| 清理 | 删除临时容器 |

---

## 常用操作

### 查看 Jenkins 是否在运行

```bash
docker ps | grep jenkins
```

### 重启 Jenkins

```bash
docker restart jenkins
```

### 查看本地 Registry 中的镜像

```bash
./view_registry.sh
```

### 验证 Pipeline 环境

```bash
./verify_pipeline.sh
```

### 修复 Jenkins 容器时区

```bash
./fix_jenkins_timezone.sh
```

### 遇到 Script Security 报错

在 Jenkins Web 界面 → 管理 Jenkins → Script Console，粘贴 `disable_script_security.groovy` 的内容并执行。

### Docker 拉取镜像慢 / 报 429

```bash
cd docker/
./apply-daemon-mirrors.sh
```

---

## 常见问题

| 问题 | 解决 |
|------|------|
| 无法访问 `http://localhost:8080` | 检查容器是否在跑：`docker ps \| grep jenkins` |
| Pipeline 找不到 Jenkinsfile | 检查 Script Path 是否正确 |
| Docker 命令执行失败 | 确保启动时挂载了 `-v /var/run/docker.sock:/var/run/docker.sock` |
| Script Security 报 `UnapprovedUsageException` | 在 Script Console 执行 `disable_script_security.groovy` |
| Git 拉取失败 | 在 Jenkins → 凭据管理 → 添加 GitHub 凭据 |

---

## 备注

- Jenkins 镜像：`jenkins/jenkins:lts`（约 500MB）
- 之前使用的账号：`admin/admin`
- 之前的初始密码：`3429920629ae4c91b3743a6f4a3e6a06`
- 测试应用端口：容器内 8000（健康检查 18080）
- 本地 Registry 端口：5000
