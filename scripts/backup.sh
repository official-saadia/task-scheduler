#!/usr/bin/env bash
#
# Example backup script for Task Scheduler (DATABASE_BACKUP task type).
#
# Task Scheduler runs this via `sh -c`, waits for it to finish, and records
# the exit code plus combined stdout/stderr. Exit 0 = success; anything else
# is a failure and feeds the normal retry -> DLQ path.
#
# Everything this script prints ends up in backup_executions.output
# (truncated to 8000 chars), so print enough to debug a failure.

set -euo pipefail

# ---- config (override via env) ----------------------------------
DB_NAME="${DB_NAME:-task_scheduler}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
BACKUP_DIR="${BACKUP_DIR:-/home/saadia/development/java/task-scheduler/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
# -----------------------------------------------------------------

timestamp="$(date +%Y%m%d_%H%M%S)"
outfile="${BACKUP_DIR}/${DB_NAME}_${timestamp}.sql.gz"

echo "[backup] starting at $(date -Is)"
echo "[backup] target: ${DB_USER}@${DB_HOST}:${DB_PORT}/${DB_NAME}"
echo "[backup] output: ${outfile}"

mkdir -p "${BACKUP_DIR}"

# --single-transaction keeps InnoDB consistent without locking writers.
# Password via env so it isn't visible in `ps`.
MYSQL_PWD="${DB_PASS}" mysqldump \
    --host="${DB_HOST}" \
    --port="${DB_PORT}" \
    --user="${DB_USER}" \
    --single-transaction \
    --quick \
    --routines \
    --triggers \
    "${DB_NAME}" \
  | gzip -c > "${outfile}"

# mysqldump's exit code, not gzip's. Without this, a dump failure that still
# produces a valid (empty) gzip stream would look like success.
dump_status="${PIPESTATUS[0]}"
if [ "${dump_status}" -ne 0 ]; then
    echo "[backup] ERROR: mysqldump exited ${dump_status}" >&2
    rm -f "${outfile}"
    exit "${dump_status}"
fi

if [ ! -s "${outfile}" ]; then
    echo "[backup] ERROR: dump file is empty" >&2
    rm -f "${outfile}"
    exit 1
fi

size="$(du -h "${outfile}" | cut -f1)"
echo "[backup] wrote ${size}"

deleted="$(find "${BACKUP_DIR}" -name "${DB_NAME}_*.sql.gz" -mtime "+${RETENTION_DAYS}" -print -delete | wc -l)"
echo "[backup] pruned ${deleted} backup(s) older than ${RETENTION_DAYS} days"

echo "[backup] done at $(date -Is)"
exit 0
