# 快速启动指南

## 一键启动

```bash
# 1. 进入项目目录
cd personal-blog

# 2. 启动所有服务
docker-compose up --build -d

# 3. 等待服务启动（约1-2分钟）
# 查看启动日志
docker-compose logs -f backend
```

## 验证服务

```bash
# 测试登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 预期返回：包含token的JSON响应
```

## 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（清空数据库）
docker-compose down -v
```

## 常见问题

### 1. 端口被占用

如果8080、3306或6379端口被占用，修改`docker-compose.yml`中的端口映射：

```yaml
services:
  backend:
    ports:
      - "8080:8081"  # 改为其他端口，如 "8082:8081"
```

### 2. 服务启动失败

查看具体服务的日志：

```bash
# 查看后端日志
docker-compose logs backend

# 查看MySQL日志
docker-compose logs mysql

# 查看Redis日志
docker-compose logs redis
```

### 3. 数据库初始化失败

重新初始化数据库：

```bash
docker-compose down -v
docker-compose up --build -d
```

## 访问地址

- 后端 API: http://localhost:8080
- MySQL: localhost:3306
- Redis: localhost:6379

## 测试账号

### 管理员
- 用户名: `admin`
- 密码: `admin123`

### 普通用户
- 用户名: `user`
- 密码: `admin123`

## 下一步

- 查看 [API测试文档](API_TEST.md) 了解接口使用
- 查看 [部署指南](DEPLOYMENT.md) 了解生产环境部署
- 查看 [项目设计文档](project_design.md) 了解系统架构
