#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
测试 Jenkins 客户端连接
用于验证 python-jenkins 库是否正确安装
"""

import jenkins
import os

def test_jenkins_connection():
    """测试 Jenkins 连接（如果有配置的话）"""
    print("=" * 50)
    print("Jenkins 客户端测试")
    print("=" * 50)
    
    # 检查 Jenkins 客户端是否可用
    try:
        server = jenkins.Jenkins('http://localhost:8080',
                                 username=os.getenv('JENKINS_USER', ''),
                                 password=os.getenv('JENKINS_TOKEN', ''))
        
        # 获取 Jenkins 版本信息
        version = server.get_version()
        print(f"✓ Jenkins 客户端连接成功！")
        print(f"  Jenkins 版本: {version}")
        
        # 获取用户信息
        user = server.get_whoami()
        print(f"  当前用户: {user.get('id', 'unknown')}")
        
        # 获取作业列表
        jobs = server.get_jobs()
        print(f"  作业数量: {len(jobs)}")
        
    except Exception as e:
        print(f"⚠ Jenkins 连接测试失败（这是正常的，如果 Jenkins 服务器未运行）:")
        print(f"  {type(e).__name__}: {str(e)}")
        print("\n提示: 要测试 Jenkins 连接，需要:")
        print("  1. 运行 Jenkins 服务器")
        print("  2. 设置环境变量 JENKINS_USER 和 JENKINS_TOKEN")
    
    print("\n" + "=" * 50)
    print("python-jenkins 库已正确安装！")
    print("=" * 50)

if __name__ == "__main__":
    test_jenkins_connection()
