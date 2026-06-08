# 部署指南

## 快速部署

### 前置要求
- Docker 20.10+
- Docker Compose 1.29+
- 至少 2GB 可用内存
- 至少 5GB 可用磁盘空间

### 部署步骤

1. 克隆项目
```bash
git clone <repository-url>
cd personal-blog
```

2. 启动服务
```bash
docker-compose up --build -d
```

3. 查看服务状态
```bash
docker-compose ps
```

4. 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f mysql
docker-compose logs -f redis
```

## 验证部署

### 1. 检查服务健康状态

```bash
# 检查容器状态
docker-compose ps

# 预期输出：所有服务状态为 "Up" 且显示 "healthy"
```

### 2. 测试API接口

```bash
# 测试后端健康状态
curl http://localhost:8080/api/auth/login

# 测试数据库连接
docker-compose exec mysql mysql -uroot -p123456 -e "SHOW DATABASES;"

# 测试Redis连接
docker-compose exec redis redis-cli ping
```

### 3. 测试登录功能

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

预期返回包含token的JSON响应。

## 生产环境部署

### 1. 环境变量配置

创建 `.env` 文件：

```env
# MySQL配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=personal_blog

# JWT配置
JWT_SECRET=your_jwt_secret_key_at_least_32_characters_long

# 文件上传路径
FILE_UPLOAD_PATH=/app/uploads

# Redis配置（如需密码）
REDIS_PASSWORD=your_redis_password
```

### 2. 修改docker-compose.yml

```yaml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
  
  backend:
    environment:
      JWT_SECRET: ${JWT_SECRET}
      FILE_UPLOAD_PATH: ${FILE_UPLOAD_PATH}
```

### 3. 使用Nginx反向代理

创建 `nginx.conf`:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 4. 配置HTTPS（推荐）

使用Let's Encrypt获取免费SSL证书：

```bash
# 安装certbot
sudo apt-get install certbot python3-certbot-nginx

# 获取证书
sudo certbot --nginx -d your-domain.com
```

## 数据备份

### 备份MySQL数据

```bash
# 备份数据库
docker-compose exec mysql mysqldump -uroot -p123456 personal_blog > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker-compose exec -T mysql mysql -uroot -p123456 personal_blog < backup_20240214.sql
```

### 备份文件上传目录

```bash
# 备份上传文件
docker run --rm -v blog-backend-uploads:/data -v $(pwd):/backup alpine tar czf /backup/uploads_backup_$(date +%Y%m%d).tar.gz -C /data .

# 恢复上传文件
docker run --rm -v blog-backend-uploads:/data -v $(pwd):/backup alpine tar xzf /backup/uploads_backup_20240214.tar.gz -C /data
```

## 监控和日志

### 查看日志

```bash
# 实时查看日志
docker-compose logs -f backend

# 查看最近100行日志
docker-compose logs --tail=100 backend

# 查看特定时间的日志
docker-compose logs --since 2024-02-14T10:00:00 backend
```

### 监控资源使用

```bash
# 查看容器资源使用情况
docker stats

# 查看特定容器
docker stats blog-backend blog-mysql blog-redis
```

## 故障排查

### 1. 后端无法连接数据库

检查MySQL是否正常运行：
```bash
docker-compose logs mysql
docker-compose exec mysql mysql -uroot -p123456 -e "SELECT 1;"
```

### 2. 后端无法连接Redis

检查Redis是否正常运行：
```bash
docker-compose logs redis
docker-compose exec redis redis-cli ping
```

### 3. 端口冲突

修改docker-compose.yml中的端口映射：
```yaml
services:
  backend:
    ports:
      - "8082:8081"  # 改为其他可用端口
```

### 4. 内存不足

增加Docker内存限制或升级服务器配置。

## 更新部署

### 更新代码

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 重新构建并启动
docker-compose up --build -d

# 3. 查看启动日志
docker-compose logs -f backend
```

### 滚动更新（零停机）

```bash
# 1. 构建新镜像
docker-compose build backend

# 2. 启动新容器
docker-compose up -d --no-deps --scale backend=2 backend

# 3. 等待新容器健康检查通过

# 4. 停止旧容器
docker-compose up -d --no-deps --scale backend=1 backend
```

## 性能优化

### 1. 数据库优化

```sql
-- 添加索引
CREATE INDEX idx_article_user_id ON article(user_id);
CREATE INDEX idx_article_category_id ON article(category_id);
CREATE INDEX idx_article_create_time ON article(create_time);

-- 优化查询
ANALYZE TABLE article;
OPTIMIZE TABLE article;
```

### 2. Redis缓存

配置Redis持久化：
```yaml
redis:
  command: redis-server --appendonly yes
```

### 3. 应用层优化

- 启用数据库连接池
- 配置合适的JVM参数
- 使用CDN加速静态资源

## 安全加固

### 1. 修改默认密码

```bash
# 修改MySQL root密码
docker-compose exec mysql mysql -uroot -p123456 -e "ALTER USER 'root'@'%' IDENTIFIED BY 'new_secure_password';"

# 修改应用配置
vim backend/src/main/resources/application-dev.yml
```

### 2. 限制网络访问

```yaml
services:
  mysql:
    networks:
      - backend-network
    # 不暴露端口到主机
    # ports:
    #   - "3306:3306"
```

### 3. 定期更新

```bash
# 更新Docker镜像
docker-compose pull
docker-compose up -d
```

## 卸载

```bash
# 停止并删除所有容器、网络、数据卷
docker-compose down -v

# 删除镜像
docker rmi $(docker images -q personal-blog*)
```

## 技术支持

如遇到问题，请查看：
- [项目文档](README.md)
- [API测试文档](API_TEST.md)
- [常见问题](QUICKSTART.md#常见问题)
