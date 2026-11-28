FROM ubuntu:24.04 AS tooling

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends openssh-client python3 ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --uid 10001 --no-create-home --shell /usr/sbin/nologin lab-runtime \
    && install -d -o 10001 -g 10001 /var/lib/ssh-collector

COPY collector.py /opt/lab/collector.py

FROM tooling AS target
RUN apt-get update \
    && apt-get install -y --no-install-recommends openssh-server rsyslog openssl \
    && rm -f /etc/ssh/ssh_host_*_key /etc/ssh/ssh_host_*_key.pub \
    && rm -rf /var/lib/apt/lists/*

COPY --chmod=0755 start.sh probe.sh /usr/local/bin/
COPY rsyslog-lab.conf /etc/rsyslog-lab.conf

EXPOSE 22
CMD ["/usr/local/bin/start.sh"]
