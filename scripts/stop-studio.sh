#!/usr/bin/env bash
# ──────────────────────────────────────────────
# AgentForge Studio (Frontend) — Stop
# ──────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
PID_FILE="$PROJECT_DIR/.studio.pid"
TIMEOUT="${STOP_TIMEOUT:-10}"

if [ ! -f "$PID_FILE" ]; then
    echo "⚠  PID 文件不存在，Studio 可能未运行"
    # 尝试通过端口查找
    PORT="${STUDIO_PORT:-3000}"
    if command -v lsof &>/dev/null; then
        PID=$(lsof -ti:"$PORT" 2>/dev/null || true)
        if [ -n "$PID" ]; then
            echo "  发现端口 $PORT 上的进程 (PID: $PID)，正在停止..."
            kill "$PID" 2>/dev/null || true
            sleep 1
            kill -0 "$PID" 2>/dev/null && kill -9 "$PID" 2>/dev/null || true
            echo "✓ Studio 已停止"
            exit 0
        fi
    fi
    echo "  Studio 未在运行"
    exit 0
fi

PID=$(cat "$PID_FILE")

if ! kill -0 "$PID" 2>/dev/null; then
    echo "⚠  进程 $PID 已不存在"
    rm -f "$PID_FILE"
    exit 0
fi

echo "▶ 正在停止 AgentForge Studio (PID: $PID)..."

# 优雅关闭
kill "$PID" 2>/dev/null

# 同时终止子进程（pnpm dev 会启动子进程）
pkill -P "$PID" 2>/dev/null || true

WAITED=0
while kill -0 "$PID" 2>/dev/null && [ "$WAITED" -lt "$TIMEOUT" ]; do
    sleep 1
    WAITED=$((WAITED + 1))
    printf "  等待关闭... %ds\r" "$WAITED"
done

if kill -0 "$PID" 2>/dev/null; then
    echo ""
    echo "  优雅关闭超时，强制终止..."
    kill -9 "$PID" 2>/dev/null || true
    sleep 1
fi

rm -f "$PID_FILE"
echo "✓ AgentForge Studio 已停止"
