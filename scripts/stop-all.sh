#!/usr/bin/env bash
# ──────────────────────────────────────────────
# AgentForge — Stop All (Backend + Frontend)
# ──────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "╔══════════════════════════════════════════╗"
echo "║     AgentForge — Stopping All            ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── 停止前端 ──
echo "━━━ Frontend (Studio) ━━━"
bash "$SCRIPT_DIR/stop-studio.sh"
echo ""

# ── 停止后端 ──
echo "━━━ Backend (Server) ━━━"
bash "$SCRIPT_DIR/stop-server.sh"
echo ""

echo "✓ AgentForge 已全部停止"
