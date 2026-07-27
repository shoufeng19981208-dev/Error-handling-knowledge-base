#!/bin/bash
# ============================================
# 报错处理知识库 - 公共环境定义
# 被 deploy.sh / start.sh / stop.sh / restart.sh / status.sh 引用，无需单独执行
# ============================================

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_JAR="$(ls "$BASE_DIR"/error-knowledge-base-*.jar 2>/dev/null | head -1)"
PID_FILE="$BASE_DIR/.app.pid"
LOG_DIR="$BASE_DIR/logs"
LOG_FILE="$LOG_DIR/app.log"

# 端口：读取 config/application.yml 中的 port 配置，读不到时用 8080
PORT=$(grep -E '^[[:space:]]*port:' "$BASE_DIR/config/application.yml" 2>/dev/null | head -1 | tr -cd '0-9')
[ -n "$PORT" ] || PORT=8080

# JVM 参数：可通过环境变量 JAVA_OPTS 覆盖（如加大内存）
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 选择 Java：优先内置 JRE（Zulu 8 / Linux x86_64 glibc），异常时回退系统 java
resolve_java() {
    chmod +x "$BASE_DIR/jre/bin/java" 2>/dev/null || true
    if [ -x "$BASE_DIR/jre/bin/java" ] && "$BASE_DIR/jre/bin/java" -version >/dev/null 2>&1; then
        JAVA_BIN="$BASE_DIR/jre/bin/java"
        echo -e "${GREEN}使用内置 JRE: $("$JAVA_BIN" -version 2>&1 | head -1)${NC}"
    elif command -v java >/dev/null 2>&1; then
        JAVA_BIN="java"
        echo -e "${YELLOW}提示: 内置 JRE 不可用（可能与本机架构不符），回退系统 Java: $(java -version 2>&1 | head -1)${NC}"
    else
        echo -e "${RED}错误: 内置 JRE 不可用且系统未安装 Java，无法启动${NC}"
        exit 1
    fi
}

# 当前运行的服务 PID（pid 文件优先，缺失时按 jar 名兜底查找）
app_pid() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE" 2>/dev/null)
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            echo "$pid"
            return
        fi
    fi
    pgrep -f "error-knowledge-base-.*\.jar" 2>/dev/null | head -1
}

# 端口是否已监听（纯 bash 实现，不依赖 curl/wget/nc）
port_up() {
    (echo > "/dev/tcp/127.0.0.1/$PORT") 2>/dev/null
}
