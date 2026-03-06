#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
容器安全测试靶标应用

故意包含多种安全漏洞，用于验证容器安全平台的检测能力。
⚠️ 仅限安全测试环境使用，切勿部署到生产环境！

漏洞清单：
- 命令注入 (OS Command Injection)
- 任意文件读取 (Arbitrary File Read)
- SSRF (Server-Side Request Forgery)
- 信息泄露 (Information Disclosure)
- 不安全的反序列化 (Insecure Deserialization)
- 弱密码硬编码 (Hardcoded Credentials)
"""

from fastapi import FastAPI, Request, Query
from fastapi.responses import HTMLResponse, PlainTextResponse
from datetime import datetime
import os
import subprocess
import pickle
import base64
import urllib.request
import socket
import json

# ⚠️ 硬编码凭据 - 安全平台应检测到
ADMIN_PASSWORD = "admin123"
DB_PASSWORD = "P@ssw0rd!"
SECRET_KEY = "super-secret-jwt-key-hardcoded"
API_TOKEN = "ghp_faketoken1234567890abcdefghijklmn"

app = FastAPI(
    title="容器安全测试靶标",
    description="包含多种安全漏洞的测试应用，用于验证容器安全平台检测能力",
    version="2.0.0"
)


@app.get("/")
def index():
    return {
        "app": "容器安全测试靶标 (Vulnerable Target App)",
        "version": "2.0.0",
        "purpose": "容器安全平台检测能力验证",
        "timestamp": datetime.now().isoformat(),
        "endpoints": {
            "/health": "健康检查",
            "/info": "应用信息（含敏感信息泄露）",
            "/vuln/cmd?cmd=id": "命令注入",
            "/vuln/file?path=/etc/passwd": "任意文件读取",
            "/vuln/ssrf?url=http://169.254.169.254": "SSRF",
            "/vuln/pickle?data=base64": "不安全反序列化",
            "/vuln/env": "环境变量泄露",
            "/vuln/process": "进程信息泄露",
        }
    }


@app.get("/health")
def health_check():
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}


@app.get("/info")
def app_info():
    """信息泄露：暴露过多系统信息"""
    return {
        "app_name": "容器安全测试靶标",
        "version": "2.0.0",
        "python_version": os.sys.version,
        "hostname": socket.gethostname(),
        "ip_address": socket.gethostbyname(socket.gethostname()),
        "user": os.getenv("USER", "unknown"),
        "home": os.getenv("HOME", "unknown"),
        "path": os.getenv("PATH", "unknown"),
        "working_dir": os.getcwd(),
        "pid": os.getpid(),
        "uid": os.getuid(),
        "environment": os.getenv("ENVIRONMENT", "development"),
        "db_password": DB_PASSWORD,
        "secret_key": SECRET_KEY,
    }


@app.get("/vuln/cmd")
def command_injection(cmd: str = Query("id", description="要执行的命令")):
    """OS 命令注入：直接执行用户输入的命令"""
    try:
        result = subprocess.check_output(
            cmd, shell=True, stderr=subprocess.STDOUT, timeout=10
        )
        return PlainTextResponse(result.decode(errors="replace"))
    except subprocess.TimeoutExpired:
        return PlainTextResponse("Command timed out")
    except Exception as e:
        return PlainTextResponse(f"Error: {e}")


@app.get("/vuln/file")
def file_read(path: str = Query("/etc/passwd", description="要读取的文件路径")):
    """任意文件读取：无路径校验"""
    try:
        with open(path, "r") as f:
            content = f.read(65536)
        return PlainTextResponse(content)
    except Exception as e:
        return PlainTextResponse(f"Error: {e}")


@app.get("/vuln/ssrf")
def ssrf(url: str = Query(
    "http://169.254.169.254/latest/meta-data/",
    description="要请求的URL"
)):
    """SSRF：服务端请求伪造，无 URL 校验"""
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        resp = urllib.request.urlopen(req, timeout=5)
        return PlainTextResponse(resp.read().decode(errors="replace"))
    except Exception as e:
        return PlainTextResponse(f"Error: {e}")


@app.get("/vuln/pickle")
def insecure_deserialize(data: str = Query("", description="Base64编码的pickle数据")):
    """不安全的反序列化：直接加载用户提供的 pickle 数据"""
    if not data:
        demo = base64.b64encode(pickle.dumps({"test": "hello"})).decode()
        return {"message": "提供 base64 编码的 pickle 数据", "example": demo}
    try:
        obj = pickle.loads(base64.b64decode(data))
        return {"deserialized": str(obj)}
    except Exception as e:
        return {"error": str(e)}


@app.get("/vuln/env")
def env_disclosure():
    """环境变量全量泄露"""
    return dict(os.environ)


@app.get("/vuln/process")
def process_info():
    """进程和系统信息泄露"""
    info = {}
    for proc_file in ["/proc/version", "/proc/cpuinfo", "/proc/meminfo",
                      "/proc/self/cgroup", "/proc/self/mountinfo"]:
        try:
            with open(proc_file) as f:
                info[proc_file] = f.read(4096)
        except Exception:
            pass
    return info


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
