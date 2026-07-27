#!/bin/bash
# ============================================
# 报错处理知识库 - 运行状态
# 用法: ./status.sh
# ============================================
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/env.sh"

PID=$(app_pid)
if [ -z "$PID" ]; then
    echo -e "${YELLOW}状态: 未运行${NC}"
    exit 1
fi

echo -e "${GREEN}状态: 运行中${NC}"
echo "  PID:  $PID"
echo "  端口: $PORT $(port_up && echo '(已监听)' || echo '(未监听，可能仍在启动)')"
ps -o pid,etime,rss,args -p "$PID" 2>/dev/null | tail -1 | awk '{printf "  运行时长: %s, 内存: %.0fMB\n", $2, $3/1024}'
echo "  日志: $LOG_FILE"
