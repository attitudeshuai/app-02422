# API 测试文档

## 测试环境
- 基础URL: http://localhost:8080

## 1. 认证接口测试

### 1.1 用户注册
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123456",
    "email": "test@example.com",
    "nickname": "测试用户"
  }'
```

预期响应:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 3,
      "username": "testuser",
      "email": "test@example.com",
      "nickname": "测试用户",
      "role": 1,
      "status": 1
    }
  }
}
```

### 1.2 用户登录
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 1.3 获取当前用户信息
```bash
curl -X GET http://localhost:8080/api/auth/info \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 2. 文章接口测试

### 2.1 获取文章列表（无需登录�?
```bash
curl -X GET "http://localhost:8080/api/articles?page=1&size=10"
```

### 2.2 创建文章（需要登录）
```bash
curl -X POST http://localhost:8080/api/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "title": "Spring Boot 入门教程",
    "content": "这是一篇关�?Spring Boot 的入门教�?..",
    "categoryId": 1,
    "status": 1,
    "tagIds": [1, 2]
  }'
```

### 2.3 获取文章详情
```bash
curl -X GET http://localhost:8080/api/articles/1
```

### 2.4 更新文章
```bash
curl -X PUT http://localhost:8080/api/articles/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "title": "Spring Boot 进阶教程",
    "content": "更新后的内容...",
    "categoryId": 1,
    "status": 1,
    "tagIds": [1, 2, 3]
  }'
```

### 2.5 删除文章
```bash
curl -X DELETE http://localhost:8080/api/articles/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 2.6 搜索文章
```bash
curl -X GET "http://localhost:8080/api/articles/search?keyword=Spring"
```

### 2.7 点赞文章
```bash
curl -X POST http://localhost:8080/api/articles/1/like \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 2.8 取消点赞
```bash
curl -X DELETE http://localhost:8080/api/articles/1/like \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 3. 分类接口测试

### 3.1 获取所有分�?
```bash
curl -X GET http://localhost:8080/api/categories
```

### 3.2 创建分类（管理员�?
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{
    "name": "编程语言",
    "description": "各种编程语言相关"
  }'
```

## 4. 标签接口测试

### 4.1 获取所有标�?
```bash
curl -X GET http://localhost:8080/api/tags
```

### 4.2 创建标签
```bash
curl -X POST http://localhost:8080/api/tags \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "Docker"
  }'
```

## 5. 评论接口测试

### 5.1 获取文章评论
```bash
curl -X GET "http://localhost:8080/api/comments?articleId=1"
```

### 5.2 发表评论
```bash
curl -X POST http://localhost:8080/api/comments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "articleId": 1,
    "content": "写得很好�?
  }'
```

### 5.3 回复评论
```bash
curl -X POST http://localhost:8080/api/comments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "articleId": 1,
    "parentId": 1,
    "content": "谢谢支持�?
  }'
```

## 6. 文件接口测试

### 6.1 上传文件
```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@/path/to/your/file.jpg"
```

### 6.2 下载文件
```bash
curl -X GET http://localhost:8080/api/files/download/1 \
  -o downloaded_file.jpg
```

## 7. 用户管理接口测试（管理员�?

### 7.1 获取用户列表
```bash
curl -X GET "http://localhost:8080/api/users?page=1&size=10" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### 7.2 更新用户角色
```bash
curl -X PUT "http://localhost:8080/api/users/2/role?role=0" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### 7.3 禁用用户
```bash
curl -X PUT "http://localhost:8080/api/users/2/status?status=0" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

## 测试流程建议

1. 首先使用管理员账号登录获�?Token
2. 创建分类和标�?
3. 创建文章
4. 测试文章的查询、更新、删�?
5. 测试评论功能
6. 测试点赞功能
7. 测试文件上传下载
8. 测试用户管理功能

## 常见错误�?

- 200: 成功
- 400: 参数错误
- 401: 未授权（未登录或 Token 失效�?
- 403: 权限不足
- 500: 服务器内部错�?
