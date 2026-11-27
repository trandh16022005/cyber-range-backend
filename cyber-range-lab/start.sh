#!/bin/bash
set -e

mkdir -p /run/sshd
touch /var/log/auth.log

/usr/sbin/rsyslogd

sleep 1

if ! pgrep -x rsyslogd > /dev/null; then
    echo "ERROR: rsyslog failed to start"
    exit 1
fi

exec /usr/sbin/sshd -D -e
