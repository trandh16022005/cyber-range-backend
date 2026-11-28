"""Host-side transport for the fixed SSH Lab Compose workflow."""

import json
import os
from pathlib import Path
import subprocess
import tempfile
import time
from urllib.error import HTTPError
from urllib.request import build_opener, ProxyHandler, Request
from uuid import UUID

COMPOSE = Path(__file__).resolve().with_name("compose.yaml")
BACKEND_PORT = int(os.environ.get("SSH_LAB_BACKEND_PORT", "8080"))
BASE = f"http://127.0.0.1:{BACKEND_PORT}/api/v1/lab-agent"
HTTP = build_opener(ProxyHandler({}))
RUNNER_TOKEN = os.environ.get("SSH_LAB_RUNNER_TOKEN", "")
COLLECTOR_TOKEN = os.environ.get("SSH_LAB_COLLECTOR_TOKEN", "")
DOCKER_ENV = {key: value for key, value in os.environ.items()
              if key.upper() not in {"SSH_LAB_RUNNER_TOKEN", "SSH_LAB_COLLECTOR_TOKEN", "DATABASE_PASSWORD",
                                     "SPRING_DATASOURCE_PASSWORD", "JWT_SECRET", "SPRING_APPLICATION_JSON"}}


def backend(path, token, body=None):
    data = json.dumps(body).encode() if body is not None else None
    request = Request(BASE + path, data, {"X-Lab-Credential": token, "Content-Type": "application/json"})
    with HTTP.open(request, timeout=8) as response:
        result = json.load(response)
        if result.get("success") is not True:
            raise RuntimeError("Backend did not return a successful response")
        return result.get("data")


def compose(session, *args, data=None, timeout=20, check=True):
    command = ["docker", "compose", "-f", str(COMPOSE), "-p", f"cyber-range-ssh-{session}", *args]
    return subprocess.run(command, input=data, capture_output=True, text=True, timeout=timeout, check=check,
                          env=DOCKER_ENV)


def services(session):
    output = compose(session, "ps", "--all", "--format", "json").stdout.strip()
    if output.startswith("["):
        rows = json.loads(output)
    else:
        rows = [json.loads(line) for line in output.splitlines()]
    return {row["Service"]: row for row in rows}


def collector(session, action, data=None):
    args = ("exec", "-T", "event-collector", "python3", "/opt/lab/collector.py", action)
    return compose(session, *args, data=data).stdout


def deliver(session):
    records = json.loads(collector(session, "read"))
    if not records:
        backend(f"/{session}/collector-health", COLLECTOR_TOKEN)
        return

    try:
        receipts = backend(f"/{session}/ssh-events", COLLECTOR_TOKEN, records)
    except HTTPError as error:
        if error.code != 400:
            raise
        # Isolate malformed entries. Stop on service errors and keep the remaining queue.
        receipts = []
        for record in records:
            try:
                receipts.extend(backend(f"/{session}/ssh-events", COLLECTOR_TOKEN, [record]))
            except HTTPError as item_error:
                if item_error.code != 400:
                    raise
                print(f"Collector rejected record {record['recordKey']}", flush=True)
                receipts.append({"recordKey": record["recordKey"]})
    collector(session, "ack", json.dumps([receipt["recordKey"] for receipt in receipts]))


def heartbeat(session, status, **values):
    health = dict.fromkeys(("target", "client", "collector", "ssh", "logging", "collectorBackend"), False)
    health.update(values)
    backend(f"/{session}/heartbeat", RUNNER_TOKEN, {"status": status, "health": health})


def probe(session, action):
    return compose(session, "exec", "-T", "ubuntu-target", "/usr/local/bin/probe.sh", action, check=False).returncode == 0


def client_network(session, client_name):
    network = f"cyber-range-ssh-{session}_lab"
    output = subprocess.run(["docker", "network", "inspect", network], capture_output=True,
                            text=True, timeout=10, check=True, env=DOCKER_ENV).stdout
    members = (json.loads(output)[0].get("Containers") or {}).values()
    return network, next((member["IPv4Address"].split("/")[0] for member in members
                          if member["Name"] == client_name), None)


def process_containment(session, client_name):
    pending = backend(f"/{session}/containment", RUNNER_TOKEN)
    network, address = client_network(session, client_name)
    if not pending:
        return address
    if address == pending["sourceIp"]:
        subprocess.run(["docker", "network", "disconnect", network, client_name], check=True,
                       capture_output=True, timeout=15, env=DOCKER_ENV)
        _, address = client_network(session, client_name)
    isolated = address is None
    recovered = isolated and probe(session, "recovery")
    backend(f"/{session}/containment/{pending['alertId']}", RUNNER_TOKEN,
            {"isolated": isolated, "recovered": recovered})
    return address


def check_practice_connection(session):
    if os.environ.get("SSH_PRACTICE_ENABLED", "").lower() != "true":
        return
    path = f"/{session}/practice-connection"
    attempt = backend(path, RUNNER_TOKEN, {})
    if not attempt:
        return
    attempt = UUID(attempt)
    result = compose(session, "exec", "-T", "lab-client", "ssh", "-F", "/dev/null",
                     "-o", "BatchMode=yes", "-o", "IdentitiesOnly=yes", "-o", "ConnectTimeout=4",
                     "-o", "UserKnownHostsFile=/run/lab-client/known_hosts",
                     "-i", "/run/lab-client/key", "lab-user@ubuntu-target", "true",
                     timeout=8, check=False)
    backend(f"{path}/{attempt}", RUNNER_TOKEN, {"success": result.returncode == 0})


def tick(job):
    session = UUID(job["id"])
    if not job["desiredRunning"]:
        if services(session):
            compose(session, "stop", timeout=40)
        heartbeat(session, "STOPPED")
        return
    state = services(session)
    if not state or any(state.get(name, {}).get("State") in {None, "exited", "created", "dead"}
                        for name in ("ubuntu-target", "lab-client", "event-collector")):
        heartbeat(session, "STARTING")
        images = ["cyber-range-lab-target:local", "cyber-range-lab-tooling:local"]
        available = subprocess.run(["docker", "image", "inspect", *images], capture_output=True,
                                   timeout=15, check=False, env=DOCKER_ENV)
        if available.returncode:
            compose(session, "build", "ubuntu-target", "event-collector", timeout=240)
        compose(session, "up", "--detach", "--no-build", "--pull", "never", timeout=90)
        state = services(session)

    target = state.get("ubuntu-target", {}).get("State") == "running"
    client = state.get("lab-client", {}).get("Health") == "healthy"
    ready_collector = state.get("event-collector", {}).get("Health") == "healthy"
    address = process_containment(session, state["lab-client"]["Name"]) if client else None
    ssh = target and probe(session, "ssh")
    logging = target and probe(session, "logging")
    linked = False
    if ready_collector:
        try:
            deliver(session)
            linked = True
        except HTTPError as error:
            print(f"Collector could not deliver events (HTTP {error.code})", flush=True)
    ok = target and client and ready_collector and ssh and logging and linked
    initial = any(row.get("Health") == "starting" for row in state.values())
    status = "RUNNING" if ok else ("STARTING" if initial else "ERROR")
    heartbeat(session, status, target=target, client=client, collector=ready_collector,
              ssh=ssh, logging=logging, collectorBackend=linked, clientAddress=address,
              error=None if ok else "One or more required Lab checks did not pass")
    if ok and address:
        try:
            check_practice_connection(session)
        except (OSError, subprocess.SubprocessError, ValueError) as error:
            print(f"Practice connection check unavailable ({type(error).__name__})", flush=True)


def poll():
    interval = 3
    authenticated = False
    managed = os.environ.get("CYBER_RANGE_MANAGED_RUNNER") == "true"
    while True:
        job = None
        try:
            job = backend("/desired", RUNNER_TOKEN)
            if not authenticated:
                print("Runner authenticated with Backend", flush=True)
                authenticated = True
            if job:
                tick(job)
            interval = 3
        except (OSError, subprocess.SubprocessError, ValueError, RuntimeError) as error:
            reason = f"HTTP {error.code}" if isinstance(error, HTTPError) else type(error).__name__
            print(f"Lab agent check failed ({reason})", flush=True)
            if managed and not authenticated:
                raise SystemExit(1) from None
            if job:
                try:
                    heartbeat(UUID(job["id"]), "ERROR", error="Lab runner command or Backend connection failed")
                except (OSError, ValueError, RuntimeError):
                    pass
            interval = min(30, interval * 2)
        time.sleep(interval)


def main():
    if len(RUNNER_TOKEN) < 32 or len(COLLECTOR_TOKEN) < 32 or RUNNER_TOKEN == COLLECTOR_TOKEN:
        raise SystemExit("Configure two distinct SSH Lab service tokens of at least 32 characters")
    directory = Path(os.environ.get("LOCALAPPDATA") or tempfile.gettempdir()) / "CyberRange"
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    with (directory / f"runner-{BACKEND_PORT}.lock").open("a+b") as lock:
        try:
            lock.seek(0)
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError:
            raise SystemExit(f"Cannot acquire the Runner lock for Backend port {BACKEND_PORT}. Check for another Runner.") from None
        poll()


if __name__ == "__main__":
    main()
