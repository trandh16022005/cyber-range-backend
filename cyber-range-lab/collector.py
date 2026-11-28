import hashlib
import json
import os
from pathlib import Path
import re
import sqlite3
import sys
import time
from datetime import datetime, timezone

LOG = Path("/var/log/lab/auth.log")
QUEUE = "/var/lib/ssh-collector/queue.db"
AUTH = re.compile(
    r"^(?P<timestamp>\S+) (?P<host>\S+) sshd(?:-session)?(?:\[\d+\])?: "
    r"(?P<result>Accepted|Failed) (?:publickey|password|keyboard-interactive(?:/pam)?) "
    r"for (?:invalid user )?(?P<user>[A-Za-z0-9_@.$-]{1,100}) from "
    r"(?P<ip>[A-Fa-f0-9.:]{2,45}) port \d+ ssh2(?:[: ].*)?$"
)
BACKLOG_LIMIT = 10_000


def normalize(line, record_key):
    text = line.decode("utf-8", errors="replace").rstrip("\r\n")
    match = AUTH.fullmatch(text)
    if not match or match["host"] != "ubuntu-target" or match["ip"] in {"127.0.0.1", "::1"}:
        return None
    try:
        observed = datetime.fromisoformat(match["timestamp"])
        if observed.tzinfo is None:
            return None
    except ValueError:
        return None
    return {
        "recordKey": record_key,
        "timestamp": observed.astimezone(timezone.utc).isoformat(),
        "sourceIp": match["ip"],
        "username": match["user"],
        "outcome": "SUCCESS" if match["result"] == "Accepted" else "FAILURE",
        "targetHost": match["host"],
        "log": text,
    }


def open_queue():
    db = sqlite3.connect(QUEUE, timeout=5)
    db.execute("PRAGMA journal_mode=WAL")
    db.execute("CREATE TABLE IF NOT EXISTS checkpoint (stream TEXT PRIMARY KEY, offset INTEGER NOT NULL)")
    db.execute("CREATE TABLE IF NOT EXISTS outbox (key TEXT PRIMARY KEY, payload TEXT NOT NULL, delivered INTEGER NOT NULL DEFAULT 0)")
    db.commit()
    return db


def collect(db, path):
    if not path.is_file():
        return
    with path.open("rb") as source:
        first = source.readline(4097)
        if not first or not first.endswith(b"\n"):
            return
        file_stat = os.fstat(source.fileno())
        stream = f"{file_stat.st_dev}:{file_stat.st_ino}:{hashlib.sha256(first).hexdigest()[:24]}"
        previous = db.execute("SELECT offset FROM checkpoint WHERE stream = ?", (stream,)).fetchone()
        offset = previous[0] if previous else 0
        if file_stat.st_size < offset:
            print(f"Unexpected truncation of {path}; use rename rotation", file=sys.stderr, flush=True)
            return
        source.seek(offset)
        for _ in range(256):
            start = source.tell()
            line = source.readline(4097)
            if not line:
                break
            oversized = len(line) > 4096
            while oversized and line and not line.endswith(b"\n"):
                line = source.readline(4097)
            if not line.endswith(b"\n"):
                break
            if oversized:
                print(f"Skipped oversized log line at {start}", file=sys.stderr, flush=True)
                continue
            key = f"{stream}:{start}"
            event = normalize(line, key)
            if event:
                if db.execute("SELECT count(*) FROM outbox WHERE delivered = 0").fetchone()[0] >= BACKLOG_LIMIT:
                    source.seek(start)
                    break
                db.execute("INSERT OR IGNORE INTO outbox (key, payload) VALUES (?, ?)", (key, json.dumps(event)))
        db.execute("INSERT INTO checkpoint VALUES (?, ?) ON CONFLICT(stream) DO UPDATE SET offset=excluded.offset",
                   (stream, source.tell() if line.endswith(b"\n") or not line else start))
        db.commit()


def main():
    action = sys.argv[1]
    if action == "run":
        with open_queue() as db:
            print("SSH auth log collector started", flush=True)
            while True:
                collect(db, LOG.with_suffix(".log.1"))
                collect(db, LOG)
                Path("/tmp/ssh-collector-heartbeat").touch()
                time.sleep(1)
    elif action == "health":
        if time.time() - Path("/tmp/ssh-collector-heartbeat").stat().st_mtime > 8:
            return 1
        with open_queue() as db:
            count = db.execute("SELECT count(*) FROM outbox WHERE delivered=0").fetchone()[0]
            return int(count >= BACKLOG_LIMIT)
    elif action == "read":
        with open_queue() as db:
            pending = db.execute("SELECT payload FROM outbox WHERE delivered=0 ORDER BY rowid LIMIT 32")
            print(json.dumps([json.loads(payload) for (payload,) in pending]))
    elif action == "ack":
        keys = json.load(sys.stdin)
        with open_queue() as db:
            db.executemany("DELETE FROM outbox WHERE key=?", [(key,) for key in keys])
    return 0


if __name__ == "__main__":
    sys.exit(main())
