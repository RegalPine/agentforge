#!/usr/bin/env bash
# ──────────────────────────────────────────────
# AgentForge — Start All (Backend + Frontend)
# ──────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "╔══════════════════════════════════════════╗"
echo "║     AgentForge — Starting All            ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# ── 启动后端 ──
echo "━━━ Backend (Server) ━━━"
bash "$SCRIPT_DIR/start-server.sh"
echo ""

# ── 启动前端 ──
echo "━━━ Frontend (Studio) ━━━"
bash "$SCRIPT_DIR/start-studio.sh"
echo ""

echo "════════════════════════════════════════════"
echo "  ✓ AgentForge 全部启动完成"
echo ""
echo "  Backend:  http://localhost:${AGENTFORGE_PORT:-8080}"
echo "  Frontend: http://localhost:${STUDIO_PORT:-3000}"
echo ""
echo "  停止: ./scripts/stop-all.sh"
echo "════════════════════════════════════════════"
