#!/usr/bin/env bash
# Smoke test for a running allocator: seeds nodes and calls, checks every route, prints a report.
# Usage: BASE_URL=http://localhost:8080 scripts/seed-and-report.sh
#
# NodeStore is in-memory and survives between runs, so every id is prefixed with a per-run
# suffix to keep reruns against the same pod independent.
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
RUN="$(date +%s)-$$"
REGION="smoke-$RUN"
NODE_A="node-a-$RUN"
NODE_B="node-b-$RUN"

for tool in curl jq; do
    command -v "$tool" >/dev/null || { echo "missing dependency: $tool" >&2; exit 2; }
done

BODY_FILE="$(mktemp)"
trap 'rm -f "$BODY_FILE"' EXIT

PASSED=0
FAILED=0
STATUS=""
BODY=""

# req METHOD PATH [JSON] -> sets STATUS and BODY
req() {
    local method="$1" path="$2" data="${3:-}"
    local args=(-s -o "$BODY_FILE" -w '%{http_code}' -X "$method" "$BASE_URL$path")
    [[ -n "$data" ]] && args+=(-H 'Content-Type: application/json' -d "$data")
    STATUS="$(curl "${args[@]}")" || STATUS="000"
    BODY="$(cat "$BODY_FILE")"
}

# check NAME EXPECTED ACTUAL
check() {
    if [[ "$2" == "$3" ]]; then
        PASSED=$((PASSED + 1))
        printf '  \033[32mPASS\033[0m %s\n' "$1"
    else
        FAILED=$((FAILED + 1))
        printf '  \033[31mFAIL\033[0m %s (expected %s, got %s)\n' "$1" "$2" "$3"
        [[ -n "$BODY" ]] && printf '       body: %s\n' "$BODY"
    fi
}

json() { jq -r "$1" <<<"$BODY" 2>/dev/null; }

register_node() { req PUT /nodes "{\"id\":\"$1\",\"region\":\"$REGION\",\"capacity\":$2}"; }
allocate() { req POST /calls "{\"callId\":\"$1\",\"region\":\"${2:-$REGION}\"}"; }

echo "Smoke testing $BASE_URL (region $REGION)"

echo "Health"
req GET /health
check "GET /health" 200 "$STATUS"
if [[ "$STATUS" != 200 ]]; then
    echo "No healthy allocator at $BASE_URL; is the port-forward running?" >&2
    exit 1
fi

echo "Nodes"
register_node "$NODE_A" 2
check "register $NODE_A" 200 "$STATUS"
check "$NODE_A capacity" 2 "$(json .capacity)"
register_node "$NODE_B" 1
check "register $NODE_B" 200 "$STATUS"
req GET "/nodes/$REGION/$NODE_A"
check "GET node" 200 "$STATUS"
check "GET node current" 0 "$(json .current)"
req GET "/nodes/$REGION/missing-$RUN"
check "GET unknown node" 404 "$STATUS"
req PUT /nodes "{\"id\":\"bad-$RUN\",\"region\":\"$REGION\",\"capacity\":-1}"
check "register negative capacity" 400 "$STATUS"

echo "Calls"
for i in 1 2 3; do
    allocate "call-$i-$RUN"
    check "allocate call-$i" 200 "$STATUS"
done
allocate "call-4-$RUN"
check "allocate beyond capacity" 503 "$STATUS"

allocate "call-1-$RUN"
FIRST_NODE="$(json .nodeId)"
check "re-allocate running call" 200 "$STATUS"
req GET "/calls/call-1-$RUN"
check "GET call" 200 "$STATUS"
check "GET call status" RUNNING "$(json .status)"
check "GET call node matches allocation" "$FIRST_NODE" "$(json .nodeId)"

req DELETE "/calls/call-1-$RUN"
check "drop call" 204 "$STATUS"
req GET "/calls/call-1-$RUN"
check "dropped call status" FINISHED "$(json .status)"
req DELETE "/calls/call-1-$RUN"
check "drop finished call" 409 "$STATUS"
allocate "call-1-$RUN"
check "re-allocate finished call" 409 "$STATUS"

allocate "call-4-$RUN"
check "allocate into freed slot" 200 "$STATUS"
check "freed slot is on the dropped call's node" "$FIRST_NODE" "$(json .nodeId)"

echo "Errors"
req GET "/calls/missing-$RUN"
check "GET unknown call" 404 "$STATUS"
req DELETE "/calls/missing-$RUN"
check "drop unknown call" 404 "$STATUS"
allocate "call-x-$RUN" "nowhere-$RUN"
check "allocate in unknown region" 404 "$STATUS"
allocate ""
check "allocate blank callId" 400 "$STATUS"
req POST /calls '{"not":"json'
check "allocate malformed body" 400 "$STATUS"

echo
echo "Report"
printf '  %-28s %8s %8s\n' NODE CURRENT CAPACITY
for node in "$NODE_A" "$NODE_B"; do
    req GET "/nodes/$REGION/$node"
    printf '  %-28s %8s %8s\n' "$node" "$(json .current)" "$(json .capacity)"
done
printf '  %-28s %8s\n' CALL STATUS
for i in 1 2 3 4; do
    req GET "/calls/call-$i-$RUN"
    printf '  %-28s %8s  -> %s\n' "call-$i-$RUN" "$(json .status)" "$(json .nodeId)"
done

echo
echo "$PASSED passed, $FAILED failed"
[[ "$FAILED" -eq 0 ]]
