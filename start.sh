#!/bin/bash

echo "=== Agent-Scope Java 2.0 启动脚本 ==="
echo ""

# 检查MongoDB是否运行
echo "检查MongoDB连接..."
if ! mongosh --eval "db.version()" --quiet 2>/dev/null; then
    echo "[警告] MongoDB未运行或未安装"
    echo ""
    echo "请先启动MongoDB:"
    echo "  1. 安装MongoDB: https://www.mongodb.com/try/download/community"
    echo "  2. 启动MongoDB: mongod --dbpath /path/to/data"
    echo ""
    echo "或者使用Docker启动MongoDB:"
    echo "  docker run -d -p 27017:27017 --name mongodb mongo:latest"
    echo ""
fi

# 检查GLM API Key
echo "检查GLM API配置..."
if [ -z "$GLM_API_KEY" ]; then
    echo "[警告] GLM_API_KEY环境变量未设置"
    echo "请设置环境变量: export GLM_API_KEY=your-api-key"
    echo ""
fi

echo "启动应用..."
cd "$(dirname "$0")/backend"
mvn spring-boot:run
