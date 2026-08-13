#!/bin/bash
# ============================================
# 报错处理知识库 - 一键启动（开发模式：后端 + 前端）
# 用法: ./start.sh
# ============================================
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"
FRONTEND_DIR="$PROJECT_DIR/frontend"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

cleanup() {
    echo ""
    echo -e "${YELLOW}正在停止服务...${NC}"
    [ -n "$BACKEND_PID" ] && kill "$BACKEND_PID" 2>/dev/null && echo -e "${GREEN}后端已停止${NC}"
    [ -n "$FRONTEND_PID" ] && kill "$FRONTEND_PID" 2>/dev/null && echo -e "${GREEN}前端已停止${NC}"
    echo -e "${GREEN}所有服务已停止${NC}"
    exit 0
}
trap cleanup SIGINT SIGTERM

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   报错处理知识库 - 启动中...${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""

# ---- 1. 启动后端 ----
echo -e "${YELLOW}[1/2] 启动后端 (Spring Boot + MySQL)...${NC}"
cd "$BACKEND_DIR"

mkdir -p data uploads

# 查找 Maven
if command -v mvn &>/dev/null; then
    MVN_CMD="mvn"
elif [ -f "$HOME/apache-maven-3.9.9/bin/mvn" ]; then
    MVN_CMD="$HOME/apache-maven-3.9.9/bin/mvn"
else
    echo -e "${RED}错误: 找不到 Maven，请确认 Maven 已安装${NC}"
    exit 1
fi

"$MVN_CMD" spring-boot:run > /tmp/errorkb-backend.log 2>&1 &
BACKEND_PID=$!

echo -n "等待后端就绪"
for i in $(seq 1 45); do
    if curl -s http://localhost:8080/api/error-record/categories > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}后端已就绪 (PID: $BACKEND_PID)${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

if ! curl -s http://localhost:8080/api/error-record/categories > /dev/null 2>&1; then
    echo ""
    echo -e "${RED}后端启动超时，查看日志: tail -f /tmp/errorkb-backend.log${NC}"
    cleanup
    exit 1
fi

# ---- 2. 启动前端 ----
echo ""
echo -e "${YELLOW}[2/2] 启动前端 (Vue 2.6)...${NC}"
cd "$FRONTEND_DIR"

if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}首次运行，安装前端依赖...${NC}"
    npm install
fi

npm run serve > /tmp/errorkb-frontend.log 2>&1 &
FRONTEND_PID=$!

echo -n "等待前端就绪"
for i in $(seq 1 30); do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 2>/dev/null | grep -q 200; then
        echo ""
        echo -e "${GREEN}前端已就绪 (PID: $FRONTEND_PID)${NC}"
        break
    fi
    echo -n "."
    sleep 2
done

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   启动完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "  前端页面:    ${GREEN}http://localhost:3000${NC}"
echo -e "  后端 API:    ${GREEN}http://localhost:8080${NC}"
echo -e "  数据库:      ${GREEN}MySQL (errorkb)${NC}"
echo ""
echo -e "${YELLOW}按 Ctrl+C 停止所有服务${NC}"
echo ""

wait $BACKEND_PID $FRONTEND_PID 2>/dev/null
