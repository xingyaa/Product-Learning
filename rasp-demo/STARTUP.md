## RASP Demo 启动文档（cicd/rasp-demo）

本文件仅说明 **如何启动 `cicd/rasp-demo` 应用**，用于命令注入 / 内存马等 RASP 攻击演示。

---

### 1. 环境准备

- **必需软件**
  - Docker
  - Docker Compose（Docker CLI 自带的 `docker compose` 即可）

- **代码目录**
  - 进入项目根目录后：
    ```bash
    cd cicd/rasp-demo
    ```
  - 关键文件：
    - `DiagnosticsServer.java`：后端 Java 演示服务
    - `index.html`：前端页面
    - `Dockerfile`：多阶段构建（编译 + 运行）
    - `Dockerfile.single`：单阶段构建（仅运行，需提前本地编译）
    - `build.sh`：在宿主机用 JDK 编译 `.class` 的脚本
    - `docker-compose.yml`：一键启动编排

---

### 2. 推荐启动方式（使用 Dockerfile，多阶段构建）

1. 在项目根目录进入 demo 目录：

   ```bash
   cd cicd/rasp-demo
   ```

2. 构建并启动容器：

   ```bash
   docker compose up -d --build
   ```

   - `docker-compose.yml` 默认：
     - 使用当前目录作为构建上下文
     - 使用 `Dockerfile` 进行多阶段构建
     - 将容器内部 `8080` 端口映射为宿主机 `9093`

3. 查看容器状态（可选）：

   ```bash
   docker compose ps
   docker logs rasp-demo
   ```

4. 访问应用：

   - 浏览器打开：`http://<宿主机 IP>:9093/`

---

### 3. 镜像拉取受限时的单阶段构建（使用 Dockerfile.single）

在某些环境中，拉取 `eclipse-temurin:8-jdk-alpine`（构建阶段 JDK 镜像）可能会出现频率限制或 429，这时可以改为：

> 宿主机先编译 `.class` → Docker 镜像只携带 JRE + `.class`

具体步骤：

1. **在宿主机安装 JDK 8（或兼容版本）**

2. **本地编译 Java 源码**

   ```bash
   cd cicd/rasp-demo
   ./build.sh
   ```

   - 该脚本会：
     - 使用本机 `javac` 编译 `DiagnosticsServer.java`
     - 生成 `out/*.class`

3. **修改 `docker-compose.yml` 使用 `Dockerfile.single`**

   将原来的：

   ```yaml
   services:
     rasp-demo:
       build: .
   ```

   替换为（示例）：

   ```yaml
   services:
     rasp-demo:
       build:
         context: .
         dockerfile: Dockerfile.single
   ```

4. **重新构建并启动**

   ```bash
   cd cicd/rasp-demo
   docker compose up -d --build
   ```

5. 访问方式与端口映射不变：

   - 浏览器访问：`http://<宿主机 IP>:9093/`

---

### 4. 停止与清理

- 停止并移除容器：

  ```bash
  cd cicd/rasp-demo
  docker compose down
  ```

- 如需同时删除镜像，可以手动执行（需确认镜像名一致）：

  ```bash
  docker rmi rasp-demo:latest
  ```

---

### 5. 基本功能验证步骤

1. **基础连通性**
   - 浏览器打开 `http://<宿主机 IP>:9093/`，能看到 OA 风格的演示页面。

2. **正常诊断请求**
   - 在页面的「发件服务器连接测试」输入框中输入：
     - `127.0.0.1`
   - 点击「执行」，应看到 `ping` 输出结果。

3. **命令注入演示**
   - 在同一输入框中输入：
     - `127.0.0.1 && whoami` 或 `127.0.0.1 && id`
   - 再次点击「执行」，页面会展示：
     - 前半部分为 `ping` 结果
     - 末尾为注入命令输出（如 `root` 或 `uid=...`）

该行为为**刻意保留的漏洞**，用于挂载 RASP 后进行命令注入检测/阻断演示。

