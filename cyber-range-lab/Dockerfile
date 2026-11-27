FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        openssh-server \
        rsyslog \
        ca-certificates \
    && mkdir -p /run/sshd /var/log/ssh \
    && touch /var/log/auth.log \
    && rm -rf /var/lib/apt/lists/*

COPY start.sh /usr/local/bin/start.sh

RUN chmod +x /usr/local/bin/start.sh

EXPOSE 22

CMD ["/usr/local/bin/start.sh"]