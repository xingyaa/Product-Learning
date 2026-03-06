#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
简单的 CI/CD 测试应用
这是一个用于测试 CI/CD 流程的简单 Web 应用
"""

from fastapi import FastAPI
from datetime import datetime
import os

app = FastAPI(
    title="CI/CD 测试应用",
    description="用于测试 CI/CD 流程的简单应用",
    version="1.0.0"
)


@app.get("/")
def read_root():
    """根路径，返回欢迎信息"""
    return {
        "message": "欢迎使用 CI/CD 测试应用！",
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0",
        "environment": os.getenv("ENVIRONMENT", "development")
    }


@app.get("/health")
def health_check():
    """健康检查端点"""
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat()
    }


@app.get("/info")
def get_info():
    """获取应用信息"""
    return {
        "app_name": "CI/CD 测试应用",
        "version": "1.0.0",
        "python_version": os.sys.version,
        "environment": os.getenv("ENVIRONMENT", "development"),
        "hostname": os.getenv("HOSTNAME", "unknown")
    }


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
