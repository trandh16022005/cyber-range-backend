#!/bin/bash
set -euo pipefail

install -d /run/sshd /var/log/lab /run/ssh-probe
touch /var/log/lab/auth.log
chmod 0644 /var/log/lab/auth.log

if ! id lab-user >/dev/null 2>&1; then
    useradd -m -s /bin/bash -p "$(openssl rand -hex 32 | openssl passwd -6 -stdin)" lab-user
fi

if [[ ! -s /run/lab-client/key ]]; then
    ssh-keygen -q -t ed25519 -N '' -f /run/lab-client/key
fi
if [[ ! -s /run/ssh-probe/key ]]; then
    ssh-keygen -q -t ed25519 -N '' -f /run/ssh-probe/key
fi
chown 10001:10001 /run/lab-client/key
install -d -m 0700 -o lab-user -g lab-user /home/lab-user/.ssh
cat /run/lab-client/key.pub /run/ssh-probe/key.pub > /home/lab-user/.ssh/authorized_keys
chown lab-user:lab-user /home/lab-user/.ssh/authorized_keys
chmod 0600 /home/lab-user/.ssh/authorized_keys

ssh-keygen -A
printf 'ubuntu-target %s\n' "$(cat /etc/ssh/ssh_host_ed25519_key.pub)" > /run/lab-client/known_hosts
printf '127.0.0.1 %s\n' "$(cat /etc/ssh/ssh_host_ed25519_key.pub)" > /run/ssh-probe/known_hosts

/usr/sbin/rsyslogd -f /etc/rsyslog-lab.conf
exec /usr/sbin/sshd -D -o PasswordAuthentication=no -o KbdInteractiveAuthentication=no \
    -o PermitRootLogin=no -o AllowUsers=lab-user -o DisableForwarding=yes \
    -o PermitTTY=no -o ForceCommand=/usr/bin/true -o LogLevel=VERBOSE
