#!/usr/bin/env bash
# ──────────────────────────────────────────────
# AgentForge — Status Check
# ──────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "AgentForge 服务状态"
echo "════════════════════════════════════════════"

# ── Backend ──
SERVER_PID_FILE="$PROJECT_DIR/.server.pid"
SERVER_PORT="${AGENTFORGE_PORT:-8080}"

if [ -f "$SERVER_PID_FILE" ] && kill -0 "$(cat "$SERVER_PID_FILE")" 2>/dev/null; then
    PID=$(cat "$SERVER_PID_FILE")
    echo "  Backend (Server):  ✓ 运行中 (PID: $PID, Port: $SERVER_PORT)"
else
    echo "  Backend (Server):  ✗ 未运行"
    [ -f "$SERVER_PID_FILE" ] && rm -f "$SERVER_PID_FILE"
fi

# ── Frontend ──
STUDIO_PID_FILE="$PROJECT_DIR/.studio.pid"
STUDIO_PORT="${STUDIO_PORT:-3000}"

if [ -f "$STUDIO_PID_FILE" ] && kill -0 "$(cat "$STUDIO_PID_FILE")" 2>/dev/null; then
    PID=$(cat "$STUDIO_PID_FILE")
    echo "  Frontend (Studio): ✓ 运行中 (PID: $PID, Port: $STUDIO_PORT)"
else
    echo "  Frontend (Studio): ✗ 未运行"
    [ -f "$STUDIO_PID_FILE" ] && rm -f "$STUDIO_PID_FILE"
fi

echo "════════════════════════════════════════════"
