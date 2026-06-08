@echo off
chcp 65001 >nul
echo ==========================================
echo 个人博客系统 - 后端项目验证脚本
echo ==========================================
echo.
echo 注意：请在 backend 目录中运行此脚本
echo.

REM 检查 Docker
echo 1. 检查 Docker...
docker --version >nul 2>&1
if %errorlevel% equ 0 (
    docker --version
    echo ✅ Docker 已安装
) else (
    echo ❌ Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

echo.
REM 检查 Docker Compose
echo 2. 检查 Docker Compose...
docker-compose --version >nul 2>&1
if %errorlevel% equ 0 (
    docker-compose --version
    echo ✅ Docker Compose 已安装
) else (
    echo ❌ Docker Compose 未安装
    pause
    exit /b 1
)

echo.
REM 检查端口占用
echo 3. 检查端口占用...
netstat -ano | findstr ":8081" >nul 2>&1
if %errorlevel% equ 0 (
    echo ❌ 端口 8081 已被占用
) else (
    echo ✅ 端口 8081 可用
)

netstat -ano | findstr ":3306" >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  端口 3306 已被占用（可能是本地 MySQL）
) else (
    echo ✅ 端口 3306 可用
)

netstat -ano | findstr ":6379" >nul 2>&1
if %errorlevel% equ 0 (
    echo ⚠️  端口 6379 已被占用（可能是本地 Redis）
) else (
    echo ✅ 端口 6379 可用
)

echo.
REM 检查项目文件
echo 4. 检查项目文件...
set all_files_exist=1

if exist "docker-compose.yml" (
    echo ✅ docker-compose.yml
) else (
    echo ❌ docker-compose.yml 不存在
    set all_files_exist=0
)

if exist "Dockerfile" (
    echo ✅ Dockerfile
) else (
    echo ❌ Dockerfile 不存在
    set all_files_exist=0
)

if exist "pom.xml" (
    echo ✅ pom.xml
) else (
    echo ❌ pom.xml 不存在
    set all_files_exist=0
)

if exist "src\main\resources\application.yml" (
    echo ✅ src\main\resources\application.yml
) else (
    echo ❌ src\main\resources\application.yml 不存在
    set all_files_exist=0
)

if exist "src\main\resources\schema.sql" (
    echo ✅ src\main\resources\schema.sql
) else (
    echo ❌ src\main\resources\schema.sql 不存在
    set all_files_exist=0
)

if exist "..\README.md" (
    echo ✅ README.md
) else (
    echo ❌ README.md 不存在
    set all_files_exist=0
)

if %all_files_exist% equ 0 (
    echo.
    echo ❌ 部分文件缺失，请检查项目完整性
    pause
    exit /b 1
)

echo.
echo ==========================================
echo ✅ 所有检查通过！
echo ==========================================
echo.
echo 现在可以启动项目（在项目根目录执行）：
echo   cd ..
echo   docker-compose up --build -d
echo.
echo 查看日志：
echo   docker-compose logs -f
echo.
echo 测试登录：
echo   curl -X POST http://localhost:8081/api/auth/login ^
echo     -H "Content-Type: application/json" ^
echo     -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
echo.
pause
