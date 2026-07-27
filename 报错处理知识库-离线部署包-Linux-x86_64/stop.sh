#!/bin/bash
# ============================================
# 报错处理知识库 - 一键停止
# 用法: ./stop.sh
# ============================================
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/env.sh"

PID=$(app_pid)
if [ -z "$PID" ]; then
    echo -e "${YELLOW}服务未在运行${NC}"
    rm -f "$PID_FILE"
    exit 0
fi

echo -e "停止服务 (PID: $PID)..."
kill "$PID" 2>/dev/null || true
for i in $(seq 1 10); do
    if ! kill -0 "$PID" 2>/dev/null; then
        echo -e "${GREEN}已停止${NC}"
        rm -f "$PID_FILE"
        exit 0
    fi
    sleep 1
done

echo -e "${YELLOW}优雅停止超时，强制结束...${NC}"
kill -9 "$PID" 2>/dev/null || true
rm -f "$PID_FILE"
echo -e "${GREEN}已停止${NC}"
