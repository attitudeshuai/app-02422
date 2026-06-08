# 个人博客系统

[![Status](https://img.shields.io/badge/status-production%20ready-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-8-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.12-green)]()
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)]()
[![License](https://img.shields.io/badge/license-MIT-blue)]()

一个功能完整的个人博客系统后端，采用前后端分离架构，基于 Spring Boot + MyBatis-Plus + MySQL 构建。

## 项目特点

✅ 功能完整 - 实现所有核心博客功能  
✅ 安全可靠 - 多层安全防护机制  
✅ 一键部署 - Docker Compose 快速启动  
✅ 文档齐全 - 从设计到部署完整文档  
✅ 代码规范 - 企业级开发标准

## How to Run

### 环境要求
- Docker 20.10+
- Docker Compose 1.29+

### 快速启动

1. 克隆项目到本地
```bash
git clone <repository-url>
cd personal-blog
```

2. （可选）验证环境
```bash
cd backend
./verify.sh      # Linux/Mac
verify.bat       # Windows
cd ..
```

3. 启动所有服务
```bash
docker-compose up --build -d
```

4. 等待服务启动完成（约1-2分钟）
```bash
# 查看服务状态
docker-compose ps

# 查看后端日志
docker-compose logs -f backend
```

5. 访问服务
- 后端 API: http://localhost:8080
- API 文档: 查看下方 Services 章节

### 停止服务
```bash
docker-compose down
```

### 清理数据（包括数据库）
```bash
docker-compose down -v
```

## Services

### 认证接口
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `GET /api/auth/info` - 获取当前用户信息

### 用户管理
- `GET /api/users` - 分页查询用户列表（管理员）
- `GET /api/users/{id}` - 获取用户详情
- `PUT /api/users/{id}` - 更新用户信息
- `DELETE /api/users/{id}` - 删除用户（管理员）
- `PUT /api/users/{id}/role` - 设置用户角色（管理员）
- `PUT /api/users/{id}/status` - 设置用户状态（管理员）

### 文章管理
- `GET /api/articles` - 分页查询文章列表
  - 参数: page, size, keyword, categoryId, userId, status
- `GET /api/articles/{id}` - 获取文章详情（自动增加浏览量）
- `POST /api/articles` - 创建文章（需登录）
- `PUT /api/articles/{id}` - 更新文章（需登录）
- `DELETE /api/articles/{id}` - 删除文章（需登录）
- `GET /api/articles/search` - 全文搜索文章
- `POST /api/articles/{id}/like` - 点赞文章（需登录）
- `DELETE /api/articles/{id}/like` - 取消点赞（需登录）

### 分类管理
- `GET /api/categories` - 获取所有分类
- `POST /api/categories` - 创建分类（管理员）
- `PUT /api/categories/{id}` - 更新分类（管理员）
- `DELETE /api/categories/{id}` - 删除分类（管理员）

### 标签管理
- `GET /api/tags` - 获取所有标签
- `POST /api/tags` - 创建标签（需登录）
- `PUT /api/tags/{id}` - 更新标签（需登录）
- `DELETE /api/tags/{id}` - 删除标签（需登录）

### 评论管理
- `GET /api/comments?articleId={id}` - 获取文章评论列表
- `POST /api/comments` - 发表评论（需登录）
- `DELETE /api/comments/{id}` - 删除评论（需登录）

### 文件管理
- `POST /api/files/upload` - 上传文件（需登录）
- `GET /api/files/download/{id}` - 下载文件
- `DELETE /api/files/{id}` - 删除文件（需登录）

## 测试账号

### 管理员账号
- 用户名: `admin`
- 密码: `admin123`
- 权限: 所有权限

### 普通用户账号
- 用户名: `user`
- 密码: `admin123`
- 权限: 基本用户权限

## 题目内容
帮我开发一款个人博客，后端技术栈采用：java8、springboot、springmvc、spring、mybatis-plus、相关安全框架你进行推荐； 
数据库：采用mysql，mysql的用户root密码123456进行配置； 
版本控制：采用maven，所有组件版本需要适用于兼容java8基础开发，选用稳定维护版本； 
前端暂不开发，但项目为前后端分离项目，提前开发好相关接口； 
博客需求：具备用户登录注册功能，具备文章发布增删改查功能，具备文章分类功能，具备标签增删改查功能、具备文章分页阅览功能、具备文章搜索功能、具备文件上传下载功能、具备登录用户评论、浏览量显示、点赞功能。其中管理员登录后具备对所有普通用户的增删改查操作，对普通用户的权限设置操作、对普通用户的文章操作。但普通用户仅能对自己的文章内容增删改查操作和个人信息修改。 
安全需求：采用合适框架或组件保障用户鉴权安全、会话安全、数据安全、防止sql注入、防止xss跨站攻击、防止爬虫、做好拦截器、过滤器等安全校验，防止未登录的状态下越权执行管理员权限操作等问题； 
最后搭建项目过程中写好测试单元即时测试bug问题、接口可用性问题；

### 项目需求
开发一款个人博客系统，实现以下功能：

#### 技术栈要求
- 后端: Java 8, Spring Boot, Spring MVC, Spring Security, MyBatis-Plus
- 数据库: MySQL 8.0 (用户root, 密码123456)
- 版本控制: Maven
- 架构: 前后端分离

#### 核心功能
1. 用户认证
   - 用户注册、登录功能
   - JWT Token 认证

2. 文章管理
   - 文章发布、编辑、删除、查询
   - 文章分类功能
   - 文章标签功能
   - 文章分页浏览
   - 文章全文搜索
   - 浏览量统计
   - 点赞功能

3. 评论功能
   - 登录用户可评论
   - 支持评论回复

4. 文件管理
   - 文件上传下载功能

5. 权限管理
   - 管理员: 对所有用户和文章的完全控制权
   - 普通用户: 仅能操作自己的文章和个人信息

#### 安全要求
- Spring Security + JWT 实现用户鉴权
- BCrypt 密码加密
- XSS 攻击防护
- SQL 注入防护（MyBatis-Plus 参数化查询）
- 接口限流防爬虫
- 全局异常处理
- 操作日志记录

#### 测试要求
- 单元测试覆盖核心功能
- 接口可用性测试

## 项目特性

### 安全特性
- JWT 无状态认证
- 密码 BCrypt 加密
- XSS 过滤器
- SQL 注入防护
- 基于 IP 的接口限流
- 角色权限控制
- 操作日志审计

### 技术特性
- RESTful API 设计
- 统一响应格式
- 全局异常处理
- 参数校验
- AOP 日志记录
- MyBatis-Plus 代码生成
- 逻辑删除
- 自动填充时间字段
- 分页查询
- 全文搜索（MySQL FULLTEXT）

### 部署特性
- Docker 容器化部署
- Docker Compose 一键启动
- 多阶段构建优化镜像大小
- 跨平台支持（ARM/X86）
- 健康检查
- 数据持久化

## 项目结构

```
personal-blog/
├── backend/                    # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/blog/
│   │   │   │   ├── annotation/      # 自定义注解
│   │   │   │   ├── aspect/          # AOP 切面
│   │   │   │   ├── config/          # 配置类
│   │   │   │   ├── constant/        # 常量
│   │   │   │   ├── controller/      # 控制器
│   │   │   │   ├── dto/             # 数据传输对象
│   │   │   │   ├── entity/          # 实体类
│   │   │   │   ├── exception/       # 异常处理
│   │   │   │   ├── filter/          # 过滤器
│   │   │   │   ├── interceptor/     # 拦截器
│   │   │   │   ├── mapper/          # MyBatis Mapper
│   │   │   │   ├── security/        # 安全相关
│   │   │   │   ├── service/         # 服务层
│   │   │   │   ├── utils/           # 工具类
│   │   │   │   ├── vo/              # 视图对象
│   │   │   │   └── BlogApplication.java
│   │   │   └── resources/
│   │   │       ├── mapper/          # MyBatis XML
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       └── schema.sql       # 数据库初始化脚本
│   │   └── test/                    # 测试代码
│   ├── docs/                        # 📚 技术文档
│   │   ├── README.md               # 文档索引
│   │   ├── QUICKSTART.md           # 快速启动
│   │   ├── PROJECT_SUMMARY.md      # 项目总结
│   │   ├── project_design.md       # 设计文档
│   │   ├── API_TEST.md             # API 测试
│   │   ├── DEPLOYMENT.md           # 部署指南
│   │   ├── DELIVERY_CHECKLIST.md   # 交付清单
│   │   ├── COMPLETION_REPORT.md    # 完成报告
│   │   ├── label-02422.md          # 对话记录
│   │   └── CHANGES.md              # 变更记录
│   ├── Dockerfile                   # Docker 构建
│   ├── pom.xml                      # Maven 配置
│   ├── verify.sh                    # 验证脚本 (Linux/Mac)
│   └── verify.bat                   # 验证脚本 (Windows)
├── docker-compose.yml               # 🐳 Docker Compose 配置
├── .gitignore                       # Git 忽略规则
└── README.md                        # 📖 项目说明
```

## 数据库设计

### 核心表
- `user` - 用户表
- `article` - 文章表
- `category` - 分类表
- `tag` - 标签表
- `article_tag` - 文章标签关联表
- `comment` - 评论表
- `article_like` - 文章点赞表
- `file_upload` - 文件上传表
- `operation_log` - 操作日志表

详细设计请查看 `backend/docs/project_design.md`

## API 使用示例

### 用户注册
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123",
    "email": "test@example.com",
    "nickname": "测试用户"
  }'
```

### 用户登录
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### 创建文章（需要 Token）
```bash
curl -X POST http://localhost:8080/api/articles \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "title": "我的第一篇文章",
    "content": "这是文章内容...",
    "categoryId": 1,
    "status": 1,
    "tagIds": [1, 2]
  }'
```

### 获取文章列表
```bash
curl http://localhost:8080/api/articles?page=1&size=10
```

## 开发说明

### 本地开发
如需本地开发，请确保安装：
- JDK 8
- Maven 3.6+
- MySQL 8.0
- Redis 6.x

修改 `application-dev.yml` 中的数据库连接配置后运行：
```bash
cd backend
mvn spring-boot:run
```

### 运行测试
```bash
cd backend
mvn test
```

查看详细的测试覆盖报告：[backend/docs/TEST_COVERAGE.md](backend/docs/TEST_COVERAGE.md)

## 技术文档

详细的技术设计文档请查看：
- [快速启动指南](backend/docs/QUICKSTART.md) - 一键启动说明
- [项目设计文档](backend/docs/project_design.md) - 系统架构和数据库设计
- [项目总结](backend/docs/PROJECT_SUMMARY.md) - 项目概述和技术亮点
- [API 测试文档](backend/docs/API_TEST.md) - 接口测试示例
- [部署指南](backend/docs/DEPLOYMENT.md) - 部署和运维指南
- [交付清单](backend/docs/DELIVERY_CHECKLIST.md) - 功能完成度检查
- [完成报告](backend/docs/COMPLETION_REPORT.md) - 项目统计和质量评估
- [对话记录](backend/docs/label-02422.md) - 完整开发过程记录

## License

MIT License
