#!/usr/bin/env bash
# ──────────────────────────────────────────────
# AgentForge Server — Start
# ──────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
PID_FILE="$PROJECT_DIR/.server.pid"
LOG_FILE="$PROJECT_DIR/logs/server.log"

PORT="${AGENTFORGE_PORT:-8080}"
JAVA_OPTS="${AGENTFORGE_JAVA_OPTS:-}"

# ── 检查是否已在运行 ──
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if kill -0 "$OLD_PID" 2>/dev/null; then
        echo "⚠  AgentForge Server 已在运行 (PID: $OLD_PID, Port: $PORT)"
        echo "   使用 ./scripts/stop-server.sh 先停止"
        exit 1
    else
        rm -f "$PID_FILE"
    fi
fi

# ── 检查端口占用 ──
if command -v lsof &>/dev/null; then
    EXISTING=$(lsof -ti:"$PORT" 2>/dev/null || true)
    if [ -n "$EXISTING" ]; then
        echo "⚠  端口 $PORT 已被占用 (PID: $EXISTING)"
        exit 1
    fi
fi

# ── 构建 classpath ──
echo "▶ 构建 AgentForge Server..."
cd "$PROJECT_DIR"

# 优先使用系统 mvn，回退到 mvnw
MVN_CMD="mvn"
if ! command -v mvn &>/dev/null; then
    MVN_CMD="./mvnw"
fi

$MVN_CMD compile -pl agentforge-server -am -B -q 2>&1

# 收集所有模块的 target/classes
CLASSPATH=""
for dir in \
    agentforge-server \
    agentforge-kernel \
    agentforge-model \
    agentforge-graph \
    agentforge-graph-ir \
    agentforge-compiler \
    agentforge-runtime \
    agentforge-security \
    agentforge-capability \
    agentforge-observability \
    agentforge-persistence \
    agentforge-event \
    agentforge-agent \
    agentforge-provider-spi \
    agentforge-provider-mock; do
    if [ -d "$dir/target/classes" ]; then
        CLASSPATH="$CLASSPATH:$dir/target/classes"
    fi
done
CLASSPATH="${CLASSPATH:1}"  # 去掉前导冒号

# ── 创建日志目录 ──
mkdir -p "$PROJECT_DIR/logs"

# ── 确定 Java 路径 ──
JAVA_CMD="java"
if [ -n "${JAVA_HOME:-}" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif [ -x "/opt/homebrew/Cellar/openjdk/21.0.2/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
    JAVA_CMD="/opt/homebrew/Cellar/openjdk/21.0.2/libexec/openjdk.jdk/Contents/Home/bin/java"
fi

# ── 启动 ──
echo "▶ 启动 AgentForge Server (Port: $PORT)..."

nohup "$JAVA_CMD" \
    $JAVA_OPTS \
    -cp "$CLASSPATH" \
    -Dagentforge.port="$PORT" \
    io.github.regalpine.agentforge.server.ServerMain \
    > "$LOG_FILE" 2>&1 &

SERVER_PID=$!
echo "$SERVER_PID" > "$PID_FILE"

# ── 等待启动确认 ──
sleep 2

if kill -0 "$SERVER_PID" 2>/dev/null; then
    echo ""
    echo "╔══════════════════════════════════════════╗"
    echo "║  ✓ AgentForge Server 已启动              ║"
    echo "╠══════════════════════════════════════════╣"
    echo "║  PID:  $(printf '%-6s' "$SERVER_PID")                         ║"
    echo "║  Port: $(printf '%-6s' "$PORT")                              ║"
    echo "║  Log:  logs/server.log                    ║"
    echo "║  Stop: ./scripts/stop-server.sh           ║"
    echo "╚══════════════════════════════════════════╝"
    echo ""
    echo "  Health:  http://localhost:$PORT/health/live"
    echo "  API:     http://localhost:$PORT/api/v1/"
else
    echo "✗ 启动失败，请检查日志: $LOG_FILE"
    rm -f "$PID_FILE"
    tail -20 "$LOG_FILE"
    exit 1
fi
