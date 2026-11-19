
#!/bin/bash

set -Eeuo pipefail

# ==============================
# 1. 基础配置
# ==============================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SERVER_USER="treasure"
SERVER_HOST="124.222.46.122"
REMOTE="${SERVER_USER}@${SERVER_HOST}"

REMOTE_DIR="/opt/restart-file"
REMOTE_JAR="${REMOTE_DIR}/restart-file-service.jar"

SERVICE_NAME="restart-file-service"
SERVICE_FILE="/etc/systemd/system/restart-file-service.service"

SERVER_ENV_FILE="/etc/restart-server/restart-server.env"

cd "$PROJECT_DIR"

echo "================================"
echo "开始发布 restart-file-service"
echo "================================"

# ==============================
# 2. Maven 打包
# ==============================

echo "开始 Maven 打包..."

if [[ -x "$PROJECT_DIR/mvnw" ]]; then
    "$PROJECT_DIR/mvnw" clean package -DskipTests
else
    mvn clean package -DskipTests
fi

# 定位 JAR
shopt -s nullglob
JARS=( "$PROJECT_DIR"/target/restart-file-service*.jar )

if [[ ${#JARS[@]} -ne 1 ]]; then
    echo "错误：没有找到唯一的 restart-file-service JAR"
    printf '%s\n' "${JARS[@]}"
    exit 1
fi

JAR_FILE="${JARS[0]}"

echo "打包成功：$JAR_FILE"

# ==============================
# 3. 检查服务器环境变量文件
# ==============================

echo "检查服务器环境变量文件..."

ssh "$REMOTE" "
    if [ ! -f '$SERVER_ENV_FILE' ]; then
        echo '错误：环境变量文件不存在：$SERVER_ENV_FILE'
        exit 1
    fi
"

echo "环境变量文件检查通过"

# ==============================
# 4. 创建服务器部署目录
# ==============================

echo "准备服务器部署目录..."

ssh "$REMOTE" "
    sudo mkdir -p '$REMOTE_DIR'
    sudo mkdir -p '$REMOTE_DIR/uploads'
    sudo chown '$SERVER_USER:$SERVER_USER' '$REMOTE_DIR'
    sudo chown '$SERVER_USER:$SERVER_USER' '$REMOTE_DIR/uploads'
"

# ==============================
# 5. 上传 JAR
# ==============================

echo "正在上传 JAR..."

scp "$JAR_FILE" "$REMOTE:$REMOTE_JAR"

echo "JAR 上传成功"

# ==============================
# 6. 创建 systemd 服务
# ==============================

echo "配置 systemd 服务..."

ssh "$REMOTE" 'sudo tee /etc/systemd/system/restart-file-service.service > /dev/null << "EOF"
[Unit]
Description=Restart File Service
After=network.target

[Service]
Type=simple
User=treasure
WorkingDirectory=/opt/restart-file

EnvironmentFile=/etc/restart-server/restart-server.env

ExecStart=/usr/bin/java -jar /opt/restart-file/restart-file-service.jar

Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable restart-file-service
'

# ==============================
# 7. 重启服务
# ==============================

echo "正在重启 restart-file-service..."

if ! ssh "$REMOTE" "sudo systemctl restart $SERVICE_NAME"; then
    echo "服务重启命令执行失败"

    ssh "$REMOTE" "
        sudo journalctl -u $SERVICE_NAME -n 80 --no-pager
    "

    exit 1
fi

# ==============================
# 8. 检查服务状态
# ==============================

echo "检查服务状态..."

sleep 3

if ! ssh "$REMOTE" "sudo systemctl is-active --quiet $SERVICE_NAME"; then

    echo "================================"
    echo "restart-file-service 启动失败"
    echo "================================"

    ssh "$REMOTE" "
        sudo systemctl status $SERVICE_NAME --no-pager
        sudo journalctl -u $SERVICE_NAME -n 80 --no-pager
    "

    exit 1
fi

# ==============================
# 9. 发布成功
# ==============================

echo "================================"
echo "restart-file-service 发布成功"
echo "================================"

ssh "$REMOTE" "
    sudo systemctl status $SERVICE_NAME --no-pager
#
#    echo '========== 最近启动日志 =========='
#
#    sudo journalctl -u $SERVICE_NAME -n 30 --no-pager
"
