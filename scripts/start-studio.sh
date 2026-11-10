#!/usr/bin/env bash
# ──────────────────────────────────────────────
# AgentForge Studio (Frontend) — Start
# ──────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
STUDIO_DIR="$PROJECT_DIR/agentforge-studio"
PID_FILE="$PROJECT_DIR/.studio.pid"
LOG_FILE="$PROJECT_DIR/logs/studio.log"

PORT="${STUDIO_PORT:-3000}"

# ── 检查是否已在运行 ──
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo "⚠  AgentForge Studio 已在运行 (PID: $OLD_PID, Port: $PORT)"
        echo "   使用 ./scripts/stop-studio.sh 先停止"
        exit 1
    else
        rm -f "$PID_FILE"
    fi
fi

# ── 检查依赖 ──
if ! command -v pnpm &>/dev/null; then
    echo "✗ 需要 pnpm，请先安装: npm install -g pnpm"
    exit 1
fi

if ! command -v node &>/dev/null; then
    echo "✗ 需要 Node.js >= 18"
    exit 1
fi

cd "$STUDIO_DIR"

# ── 安装依赖（如需要）──
if [ ! -d "node_modules" ]; then
    echo "▶ 安装前端依赖..."
    pnpm install --frozen-lockfile
fi

# ── 创建日志目录 ──
mkdir -p "$PROJECT_DIR/logs"

# ── 启动 ──
echo "▶ 启动 AgentForge Studio (Port: $PORT)..."

STUDIO_PORT="$PORT" nohup pnpm dev > "$LOG_FILE" 2>&1 &

STUDIO_PID=$!
echo "$STUDIO_PID" > "$PID_FILE"

# ── 等待启动确认 ──
sleep 3

if kill -0 "$STUDIO_PID" 2>/dev/null; then
    echo ""
    echo "╔══════════════════════════════════════════╗"
    echo "║  ✓ AgentForge Studio 已启动              ║"
    echo "╠══════════════════════════════════════════╣"
    echo "║  PID:  $(printf '%-6s' "$STUDIO_PID")                         ║"
    echo "║  Port: $(printf '%-6s' "$PORT")                              ║"
    echo "║  Log:  logs/studio.log                    ║"
    echo "║  Stop: ./scripts/stop-studio.sh           ║"
    echo "╚══════════════════════════════════════════╝"
    echo ""
    echo "  Studio:  http://localhost:$PORT"
else
    echo "✗ 启动失败，请检查日志: $LOG_FILE"
    rm -f "$PID_FILE"
    tail -20 "$LOG_FILE"
    exit 1
fi
