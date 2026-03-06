# ============================================================
# 容器安全测试靶标镜像 (Vulnerable Container Target)
# ⚠️ 仅限安全测试环境使用！
#
# 故意包含以下安全问题，用于验证容器安全平台检测能力：
#   - 以 root 用户运行
#   - 安装危险工具 (nmap, netcat, curl, wget)
#   - 植入 Webshell (PHP/JSP/Python/ASPX)
#   - 植入反弹 Shell / Bind Shell 后门
#   - 植入挖矿脚本和配置
#   - 植入提权脚本
#   - 植入数据窃取脚本
#   - 植入容器逃逸脚本
#   - 植入恶意 Crontab
#   - 植入不安全的 SSH / Nginx 配置
#   - 硬编码敏感凭据
#   - 植入伪造的 SSH 公钥后门
#   - 安装有已知 CVE 的旧版本包
# ============================================================

FROM python:3.9-slim

# ---------- 不安全实践：以 root 运行 ----------
USER root

WORKDIR /app

# ---------- 安装危险工具（安全平台应告警） ----------
RUN apt-get update && apt-get install -y --no-install-recommends \
    nmap \
    netcat-openbsd \
    curl \
    wget \
    procps \
    net-tools \
    iproute2 \
    openssh-server \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# ---------- 安装 Python 依赖 ----------
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ---------- 复制应用代码 ----------
COPY cicd_test_app.py .

# ---------- 植入恶意样本文件 ----------
COPY malicious-samples/ /opt/malicious-samples/

# Webshell 散落在 Web 目录中（模拟真实入侵场景）
RUN mkdir -p /var/www/html /usr/share/nginx/html && \
    cp /opt/malicious-samples/webshells/shell.php /var/www/html/ && \
    cp /opt/malicious-samples/webshells/cmd.jsp /var/www/html/ && \
    cp /opt/malicious-samples/webshells/eval_shell.py /var/www/html/ && \
    cp /opt/malicious-samples/webshells/aspx_cmd.aspx /var/www/html/

# 后门脚本放到隐藏目录
RUN mkdir -p /tmp/.hidden /root/.cache/.sys && \
    cp /opt/malicious-samples/backdoors/reverse_shell.sh /tmp/.hidden/ && \
    cp /opt/malicious-samples/backdoors/bind_shell.py /tmp/.hidden/ && \
    cp /opt/malicious-samples/backdoors/rootkit_loader.c /root/.cache/.sys/ && \
    chmod +x /tmp/.hidden/*.sh

# 挖矿相关文件
RUN mkdir -p /tmp/.miner && \
    cp /opt/malicious-samples/miners/xmrig_config.json /tmp/.miner/config.json && \
    cp /opt/malicious-samples/miners/start_miner.sh /tmp/.miner/ && \
    chmod +x /tmp/.miner/start_miner.sh

# 容器逃逸 / 提权 / 数据窃取脚本
RUN cp /opt/malicious-samples/scripts/container_escape.sh /usr/local/bin/.escape.sh && \
    cp /opt/malicious-samples/scripts/privilege_escalation.sh /usr/local/bin/.privesc.sh && \
    cp /opt/malicious-samples/scripts/data_exfiltration.py /usr/local/bin/.exfil.py && \
    chmod +x /usr/local/bin/.escape.sh /usr/local/bin/.privesc.sh

# ---------- 不安全配置 ----------

# 植入恶意 SSH 公钥
RUN mkdir -p /root/.ssh && \
    cp /opt/malicious-samples/backdoors/ssh_authorized_keys /root/.ssh/authorized_keys && \
    chmod 600 /root/.ssh/authorized_keys

# 不安全的 SSH 配置
RUN mkdir -p /etc/ssh && \
    cp /opt/malicious-samples/configs/insecure_sshd_config /etc/ssh/sshd_config

# 不安全的 Nginx 配置
RUN mkdir -p /etc/nginx/conf.d && \
    cp /opt/malicious-samples/configs/weak_nginx.conf /etc/nginx/conf.d/default.conf

# 泄露的凭据文件
RUN cp /opt/malicious-samples/configs/exposed_env /app/.env

# 恶意定时任务
RUN cp /opt/malicious-samples/crontabs/malicious_cron /etc/cron.d/malicious && \
    chmod 644 /etc/cron.d/malicious

# ---------- 硬编码密码 / 环境变量 ----------
ENV ADMIN_PASSWORD=admin123 \
    DB_HOST=10.0.0.5 \
    DB_PASSWORD=P@ssw0rd! \
    REDIS_PASSWORD=redis_secret \
    SECRET_KEY=hardcoded-jwt-secret \
    AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE \
    AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

# ---------- 危险端口 ----------
EXPOSE 8000 22 31337

# ---------- 无健康检查（不安全实践） ----------

# ---------- 以 root 直接运行（不安全实践） ----------
CMD ["python", "-m", "uvicorn", "cicd_test_app:app", "--host", "0.0.0.0", "--port", "8000"]
