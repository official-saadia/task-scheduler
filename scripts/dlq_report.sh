#!/usr/bin/env bash
#
# Downloads a Dead Letter Queue report from Task Scheduler's own export API.
#
# Uses GET /api/v1/task-dlq/export rather than querying MySQL directly, so the
# file produced here is byte-for-byte what the DLQ page's Download button gives
# you. The export's date-range and status semantics live in one place; a second
# hand-written SQL copy would drift from it silently.
#
# Exits non-zero on any failure so Task Scheduler marks the execution FAILED,
# retries, and finally routes it to the DLQ.
set -uo pipefail

API_BASE="${API_BASE:-http://localhost:8080/api/v1}"
API_USER="${API_USER:-admin}"
API_PASS="${API_PASS:-admin123}"
OUTPUT_FILE="${OUTPUT_FILE:-/home/saadia/development/java/task-scheduler/reports/dlq_report.xlsx}"

# Preset window: TODAY | YESTERDAY | PAST_7_DAYS | PAST_30_DAYS
DATE_RANGE="${DATE_RANGE:-PAST_7_DAYS}"

# Custom window (yyyy-MM-dd). Set BOTH to override DATE_RANGE.
FROM_DATE="${FROM_DATE:-}"
TO_DATE="${TO_DATE:-}"

# NEW | IN_PROGRESS | ANALYSED | FIXED. Blank = every status.
DLQ_STATUS="${DLQ_STATUS:-}"

CURL_TIMEOUT="${CURL_TIMEOUT:-60}"

TMP_FILE="${OUTPUT_FILE}.tmp.$$"
trap 'rm -f "$TMP_FILE"' EXIT

mkdir -p "$(dirname "$OUTPUT_FILE")" || exit 1

# ---------------------------------------------------------------- range mode
# The endpoint rejects "both" and "neither" with a 400. Failing here instead
# turns that into a message naming the offending variables.
if [[ -n "$FROM_DATE" || -n "$TO_DATE" ]]; then
  if [[ -z "$FROM_DATE" || -z "$TO_DATE" ]]; then
    echo "[dlq_report] a custom range needs both FROM_DATE and TO_DATE (got FROM_DATE='${FROM_DATE}', TO_DATE='${TO_DATE}')" >&2
    exit 1
  fi
  QUERY="from=${FROM_DATE}&to=${TO_DATE}"
else
  if [[ -z "$DATE_RANGE" ]]; then
    echo "[dlq_report] set DATE_RANGE, or both FROM_DATE and TO_DATE" >&2
    exit 1
  fi
  QUERY="dateRange=${DATE_RANGE}"
fi

if [[ -n "$DLQ_STATUS" ]]; then
  QUERY="${QUERY}&status=${DLQ_STATUS}"
fi

# ------------------------------------------------------------ authentication
# Every endpoint except /auth/** is behind JWT, so the report has to log in the
# same as any other client.
LOGIN_RAW=$(curl -sS --max-time "$CURL_TIMEOUT" -w '\n%{http_code}' \
  -H 'Content-Type: application/json' \
  -d "$(printf '{"username":"%s","password":"%s"}' "$API_USER" "$API_PASS")" \
  "${API_BASE}/auth/login" 2>&1)

if [[ $? -ne 0 ]]; then
  echo "[dlq_report] could not reach ${API_BASE}/auth/login: ${LOGIN_RAW}" >&2
  exit 1
fi

LOGIN_CODE=$(printf '%s' "$LOGIN_RAW" | tail -n1)
LOGIN_BODY=$(printf '%s' "$LOGIN_RAW" | sed '$d')

if [[ "$LOGIN_CODE" != "200" ]]; then
  echo "[dlq_report] login as '${API_USER}' failed with HTTP ${LOGIN_CODE}: ${LOGIN_BODY}" >&2
  exit 1
fi

TOKEN=$(printf '%s' "$LOGIN_BODY" | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')

if [[ -z "$TOKEN" ]]; then
  echo "[dlq_report] login succeeded but no token found in response: ${LOGIN_BODY}" >&2
  exit 1
fi

# -------------------------------------------------------------------- export
URL="${API_BASE}/task-dlq/export?${QUERY}"

# Body goes to TMP_FILE, never straight to OUTPUT_FILE. On an error the body is
# a JSON message, and writing that to the real path would leave a file that
# ReportGenerationTaskHandler counts as "produced" — the email task would then
# cheerfully attach an error blob named .xlsx.
HTTP_CODE=$(curl -sS --max-time "$CURL_TIMEOUT" \
  -H "Authorization: Bearer ${TOKEN}" \
  -o "$TMP_FILE" -w '%{http_code}' "$URL")

if [[ $? -ne 0 ]]; then
  echo "[dlq_report] export request to ${URL} failed" >&2
  exit 1
fi

if [[ "$HTTP_CODE" != "200" ]]; then
  echo "[dlq_report] export returned HTTP ${HTTP_CODE}: $(head -c 500 "$TMP_FILE")" >&2
  exit 1
fi

if [[ ! -s "$TMP_FILE" ]]; then
  echo "[dlq_report] export returned HTTP 200 but an empty body" >&2
  exit 1
fi

# XLSX is a zip, so it must start with the zip magic bytes. Catches a 200 that
# is really HTML from a proxy, or JSON from a handler that swallowed its status.
if [[ "$(head -c 2 "$TMP_FILE")" != "PK" ]]; then
  echo "[dlq_report] export returned HTTP 200 but the body is not an XLSX: $(head -c 200 "$TMP_FILE")" >&2
  exit 1
fi

mv "$TMP_FILE" "$OUTPUT_FILE" || exit 1

echo "[dlq_report] wrote $(wc -c < "$OUTPUT_FILE") bytes to ${OUTPUT_FILE} (${QUERY})"
exit 0
