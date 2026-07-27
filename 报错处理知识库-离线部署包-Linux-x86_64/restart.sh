#!/bin/bash
# ============================================
# 报错处理知识库 - 一键重启
# 用法: ./restart.sh
# ============================================
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$BASE_DIR/stop.sh"
"$BASE_DIR/start.sh"
