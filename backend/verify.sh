#!/bin/bash

echo "=========================================="
echo "个人博客系统 - 后端项目验证脚本"
echo "=========================================="
echo ""
echo "注意：请在 backend 目录中运行此脚本"
echo ""

# 检查 Docker
echo "1. 检查 Docker..."
if command -v docker &> /dev/null; then
    echo "✅ Docker 已安装: $(docker --version)"
else
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 Docker Compose
echo ""
echo "2. 检查 Docker Compose..."
if command -v docker-compose &> /dev/null; then
    echo "✅ Docker Compose 已安装: $(docker-compose --version)"
else
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

# 检查端口占用
echo ""
echo "3. 检查端口占用..."
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        echo "❌ 端口 $1 已被占用"
        return 1
    else
        echo "✅ 端口 $1 可用"
        return 0
    fi
}

check_port 8081
check_port 3306
check_port 6379

# 检查项目文件
echo ""
echo "4. 检查项目文件..."
files=(
    "docker-compose.yml"
    "Dockerfile"
    "pom.xml"
    "src/main/resources/application.yml"
    "src/main/resources/schema.sql"
    "../README.md"
)

all_files_exist=true
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file 不存在"
        all_files_exist=false
    fi
done

if [ "$all_files_exist" = false ]; then
    echo ""
    echo "❌ 部分文件缺失，请检查项目完整性"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ 所有检查通过！"
echo "=========================================="
echo ""
echo "现在可以启动项目（在项目根目录执行）："
echo "  cd .."
echo "  docker-compose up --build -d"
echo ""
echo "查看日志："
echo "  docker-compose logs -f"
echo ""
echo "测试登录："
echo "  curl -X POST http://localhost:8081/api/auth/login \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"username\":\"admin\",\"password\":\"admin123\"}'"
echo ""
