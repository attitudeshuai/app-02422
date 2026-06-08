# 项目结构调整记录

## 最近更改

### 2024 - 项目结构优化

#### 1. 验证脚本整理
**目的**: 简化项目结构，将验证脚本集中到 backend 目录

**变更内容**:
- ✅ 将验证脚本移动到 `backend/` 目录
- ✅ 删除根目录的 verify.sh 和 verify.bat
- ✅ 更新脚本中的路径引用

**使用方法**:
```bash
cd backend
./verify.sh      # Linux/Mac
verify.bat       # Windows
```

#### 2. 文档目录整理
**目的**: 将所有技术文档集中到 backend/docs 目录，保持根目录简洁

**变更内容**:
- ✅ 将 `docs/` 目录移动到 `backend/docs/`
- ✅ 将根目录的所有 .md 文件（除 README.md）移动到 `backend/docs/`
- ✅ 创建 `backend/docs/README.md` 作为文档索引

**移动的文件**:
- `QUICKSTART.md` → `backend/docs/QUICKSTART.md`
- `PROJECT_SUMMARY.md` → `backend/docs/PROJECT_SUMMARY.md`
- `COMPLETION_REPORT.md` → `backend/docs/COMPLETION_REPORT.md`
- `label-02422.md` → `backend/docs/label-02422.md`

**保留在根目录的文件**:
- `README.md` - 项目主文档
- `docker-compose.yml` - Docker 编排配置
- `.gitignore` - Git 忽略规则
- `verify.sh` / `verify.bat` - 环境验证脚本

#### 2. Docker 镜像优化
**目的**: 提升跨平台兼容性，特别是 Mac M1/M2/M3 支持

**变更内容**:
- ✅ Redis 镜像从 `redis:7-alpine` 改为 `redis:7`
- ✅ 支持多架构：linux/amd64, linux/arm64, linux/arm/v7

**原因**:
- `redis:7` 官方镜像完美支持 Apple Silicon (M1/M2/M3)
- 更好的跨平台兼容性和稳定性
- 虽然镜像稍大（~120MB vs ~30MB），但兼容性更重要

## 当前项目结构

```
personal-blog/
├── backend/
│   ├── docs/                        # 📚 所有技术文档
│   │   ├── README.md               # 文档索引
│   │   ├── QUICKSTART.md           # 快速启动
│   │   ├── PROJECT_SUMMARY.md      # 项目总结
│   │   ├── project_design.md       # 设计文档
│   │   ├── API_TEST.md             # API 测试
│   │   ├── DEPLOYMENT.md           # 部署指南
│   │   ├── DELIVERY_CHECKLIST.md   # 交付清单
│   │   ├── COMPLETION_REPORT.md    # 完成报告
│   │   └── label-02422.md          # 对话记录
│   ├── src/                        # 源代码
│   ├── Dockerfile                  # Docker 构建
│   └── pom.xml                     # Maven 配置
├── README.md                        # 📖 项目主文档
├── docker-compose.yml               # 🐳 Docker 编排
├── .gitignore                       # Git 配置
├── verify.sh                        # 验证脚本 (Linux/Mac)
└── verify.bat                       # 验证脚本 (Windows)
```

## 优势

### 1. 目录结构更清晰
- ✅ 根目录简洁，只保留核心配置文件
- ✅ 所有文档集中管理，便于查找
- ✅ 文档有独立的索引页面

### 2. 更好的跨平台支持
- ✅ Windows (x86_64)
- ✅ Linux (x86_64)
- ✅ Mac Intel (x86_64)
- ✅ Mac M1/M2/M3 (ARM64)

### 3. 符合最佳实践
- ✅ 文档与代码放在一起
- ✅ 根目录保持简洁
- ✅ 便于项目维护和扩展

## 如何使用

### 验证项目
```bash
# 在 backend 目录
cd backend
./verify.sh      # Linux/Mac
verify.bat       # Windows
```

### 查看文档
```bash
# 查看文档索引
cat backend/docs/README.md

# 或在浏览器中打开
open backend/docs/README.md  # Mac
start backend/docs/README.md # Windows
```

### 快速启动
```bash
# 参考快速启动文档
cat backend/docs/QUICKSTART.md

# 或直接启动
docker-compose up --build -d
```

### 查看 API 文档
```bash
cat backend/docs/API_TEST.md
```

## 注意事项

1. 所有文档链接已更新，指向新的路径
2. README.md 中的文档索引已更新
3. Docker 镜像已优化，支持所有主流平台
4. 项目功能和代码没有任何变化，只是文件组织优化

---

**变更日期**: 2024  
**变更人**: Kiro AI  
**影响范围**: 文档结构和 Docker 配置  
**代码影响**: 无
