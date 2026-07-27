#!/bin/bash
# ============================================
# 报错处理知识库 - 离线一键部署 (Linux x86_64 glibc)
# 无需联网 / Docker / Java / Maven / Node，内置 JRE 直接运行。
# 用法: ./deploy.sh
# ============================================
set -e
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$BASE_DIR"

# 修复解压可能丢失的执行权限
chmod +x "$BASE_DIR"/*.sh 2>/dev/null || true
chmod +x "$BASE_DIR"/jre/bin/* 2>/dev/null || true

source "$BASE_DIR/env.sh"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   报错处理知识库 - 离线一键部署${NC}"
echo -e "${GREEN}============================================${NC}"

# 初始化目录（已存在则保留，数据不受影响）
mkdir -p "$BASE_DIR/data" "$BASE_DIR/uploads" "$BASE_DIR/logs"

# 已在运行则先停止（升级/重复部署场景）
PID=$(app_pid)
if [ -n "$PID" ]; then
    echo -e "${YELLOW}检测到服务已在运行 (PID: $PID)，先停止...${NC}"
    "$BASE_DIR/stop.sh"
fi

"$BASE_DIR/start.sh"

echo ""
echo -e "${GREEN}部署完成。常用命令：${NC}"
echo "  ./start.sh    启动    |  ./stop.sh    停止"
echo "  ./restart.sh  重启    |  ./status.sh  状态"
echo "  修改端口: 编辑 config/application.yml 后 ./restart.sh"
echo "  详细说明见: 部署文档.md"
