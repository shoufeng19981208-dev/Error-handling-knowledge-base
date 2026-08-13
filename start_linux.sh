#!/bin/bash
# ============================================
# 报错处理知识库 - Linux/x86_64 离线部署脚本
# 支持: start | stop | restart | status
# ============================================

set -e

# ---- 配置区域（按需修改） ----
APP_NAME="error-knowledge-base"
# 部署目录（本脚本所在目录）
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_FILE="$DEPLOY_DIR/error-knowledge-base-1.0.0-SNAPSHOT.jar"
LOG_FILE="$DEPLOY_DIR/app.log"
PID_FILE="$DEPLOY_DIR/app.pid"
DATA_DIR="$DEPLOY_DIR/data"
UPLOAD_DIR="$DEPLOY_DIR/uploads"
JAVA_OPTS="-Xms256m -Xmx512m -Duser.dir=$DEPLOY_DIR -Dfile.encoding=UTF-8"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ---- 检查 Java 环境 ----
check_java() {
    if ! command -v java &> /dev/null; then
        if [ -d "$DEPLOY_DIR/jdk" ]; then
            export JAVA_HOME="$DEPLOY_DIR/jdk"
            export PATH="$JAVA_HOME/bin:$PATH"
        fi
    fi
    if ! command -v java &> /dev/null; then
        echo -e "${RED}错误: 未找到 Java 运行时，请安装 JDK 1.8+ 或在 jdk/ 目录放置 JRE${NC}"
        exit 1
    fi
}

# ---- 创建必要目录 ----
ensure_dirs() {
    mkdir -p "$DATA_DIR"
    mkdir -p "$UPLOAD_DIR"
    # 兼容部署目录属主变更的场景：日志目录已存在但属主不同时，尽量补齐写权限
    mkdir -p "$DEPLOY_DIR/logs"
    chmod u+rwx,g+rwx "$DEPLOY_DIR/logs" 2>/dev/null || true
}

# ---- 检查 JAR 文件 ----
check_jar() {
    if [ ! -f "$JAR_FILE" ]; then
        echo -e "${RED}错误: 找不到 $JAR_FILE${NC}"
        exit 1
    fi
}

# ---- 检查是否已运行 ----
is_running() {
    if [ -f "$PID_FILE" ]; then
        local pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            return 0
        fi
    fi
    return 1
}

# ---- 启动 ----
start() {
    if is_running; then
        echo -e "${YELLOW}${APP_NAME} 已在运行中 (PID: $(cat $PID_FILE))${NC}"
        return 0
    fi

    check_java
    check_jar
    ensure_dirs

    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}   报错处理知识库 - 启动中...${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo -e "  部署目录: ${DEPLOY_DIR}"
    echo -e "  数据目录: ${DATA_DIR}"
    echo -e "  上传目录: ${UPLOAD_DIR}"
    echo ""

    nohup java $JAVA_OPTS -jar "$JAR_FILE" > "$LOG_FILE" 2>&1 &
    local pid=$!
    echo $pid > "$PID_FILE"

    echo -n "等待应用启动"
    for i in $(seq 1 30); do
        if curl -s http://localhost:8080/api/error-record/search?page=0\&size=1 > /dev/null 2>&1; then
            echo ""
            echo -e "${GREEN}应用已启动 (PID: $pid)${NC}"
            echo ""
            echo -e "  前端页面:    ${GREEN}http://localhost:8080${NC}"
            echo -e "  API 接口:    ${GREEN}http://localhost:8080/api/error-record${NC}"
            echo -e "  数据库:      ${GREEN}MySQL (errorkb)${NC}"
            echo ""
            echo -e "  日志文件:    ${LOG_FILE}"
            echo -e "${YELLOW}  使用 ./start_linux.sh stop 停止服务${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
    done

    echo ""
    echo -e "${RED}启动超时，请检查日志: tail -f $LOG_FILE${NC}"
    echo -e "${YELLOW}最后 20 行日志:${NC}"
    tail -20 "$LOG_FILE"
    return 1
}

# ---- 停止 ----
stop() {
    if ! is_running; then
        echo -e "${YELLOW}${APP_NAME} 未在运行${NC}"
        rm -f "$PID_FILE"
        return 0
    fi

    local pid=$(cat "$PID_FILE")
    echo -e "${YELLOW}正在停止 ${APP_NAME} (PID: $pid)...${NC}"
    kill "$pid" 2>/dev/null

    # 等待进程退出
    for i in $(seq 1 15); do
        if ! kill -0 "$pid" 2>/dev/null; then
            echo -e "${GREEN}${APP_NAME} 已停止${NC}"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1
    done

    # 强制终止
    echo -e "${RED}强制终止进程...${NC}"
    kill -9 "$pid" 2>/dev/null
    rm -f "$PID_FILE"
    echo -e "${GREEN}${APP_NAME} 已强制停止${NC}"
}

# ---- 重启 ----
restart() {
    stop
    sleep 2
    start
}

# ---- 状态 ----
status() {
    if is_running; then
        local pid=$(cat "$PID_FILE")
        echo -e "${GREEN}${APP_NAME} 运行中 (PID: $pid)${NC}"
        echo -e "  日志: ${LOG_FILE}"
    else
        echo -e "${RED}${APP_NAME} 未运行${NC}"
    fi
}

# ---- 主入口 ----
case "${1:-start}" in
    start)   start ;;
    stop)    stop ;;
    restart) restart ;;
    status)  status ;;
    *)
        echo "用法: $0 {start|stop|restart|status}"
        echo ""
        echo "  start   - 启动服务"
        echo "  stop    - 停止服务"
        echo "  restart - 重启服务"
        echo "  status  - 查看状态"
        exit 1
        ;;
esac
