#!/bin/bash
set -euo pipefail
case "$1" in
    ssh) /usr/sbin/sshd -t && ssh-keyscan -T 3 127.0.0.1 2>/dev/null | grep -q 'ssh-ed25519' ;;
    logging)
        marker="lab-probe-$(cat /proc/sys/kernel/random/uuid)"
        logger -p authpriv.info -t lab-health "$marker"
        for _ in 1 2 3 4 5; do
            if grep -qF "$marker" /var/log/lab/auth.log; then exit 0; fi
            sleep 0.2
        done
        exit 1
        ;;
    recovery)
        ssh -F /dev/null -o IdentitiesOnly=yes -o BatchMode=yes -o ConnectTimeout=3 \
            -o UserKnownHostsFile=/run/ssh-probe/known_hosts -i /run/ssh-probe/key lab-user@127.0.0.1 true
        ;;
    *) exit 2 ;;
esac
