# 使用 Python 3.10 作为基础镜像
FROM python:3.10-slim

# 设置工作目录
WORKDIR /app

# 设置环境变量
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    ENVIRONMENT=production

# 复制 requirements 文件（使用本地的 requirements.txt）
COPY requirements.txt .

# 安装 Python 依赖
RUN pip install --no-cache-dir -r requirements.txt

# 安装额外的 CI/CD 测试应用依赖
RUN pip install --no-cache-dir python-jenkins

# 复制应用代码
COPY cicd_test_app.py .

# 暴露端口
EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1

# 运行应用
CMD ["python", "-m", "uvicorn", "cicd_test_app:app", "--host", "0.0.0.0", "--port", "8000"]
