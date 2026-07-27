#!/bin/bash
# ============================================
# 报错处理知识库 - 一键启动
# 用法: ./start.sh
# ============================================
set -e
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/env.sh"
cd "$BASE_DIR"

if [ -z "$APP_JAR" ]; then
    echo -e "${RED}错误: 未找到 error-knowledge-base-*.jar，请确认部署包完整${NC}"
    exit 1
fi

PID=$(app_pid)
if [ -n "$PID" ]; then
    echo -e "${YELLOW}服务已在运行 (PID: $PID)，如需重启请执行 ./restart.sh${NC}"
    exit 0
fi

resolve_java
mkdir -p "$LOG_DIR" "$BASE_DIR/data" "$BASE_DIR/uploads"

echo -e "${GREEN}启动报错处理知识库 (端口: $PORT)...${NC}"
nohup "$JAVA_BIN" $JAVA_OPTS -jar "$APP_JAR" --server.port="$PORT" > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

echo -n "等待服务就绪"
for i in $(seq 1 45); do
    if port_up; then
        IP=$(hostname -I 2>/dev/null | awk '{print $1}')
        [ -n "$IP" ] || IP=127.0.0.1
        echo ""
        echo -e "${GREEN}启动成功 (PID: $(cat "$PID_FILE"))${NC}"
        echo -e "  访问地址: ${GREEN}http://${IP}:${PORT}${NC}"
        echo -e "  日志文件: $LOG_FILE"
        exit 0
    fi
    if ! kill -0 "$(cat "$PID_FILE" 2>/dev/null)" 2>/dev/null; then
        break
    fi
    echo -n "."
    sleep 2
done

echo ""
echo -e "${RED}启动失败，最近 30 行日志：${NC}"
tail -30 "$LOG_FILE" 2>/dev/null
rm -f "$PID_FILE"
exit 1
