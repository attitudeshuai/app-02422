# 多阶段构建 - 编译阶段
FROM --platform=$BUILDPLATFORM maven:3.8.6-openjdk-8 AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并编译
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段 - 使用跨平台基础镜像
FROM eclipse-temurin:8-jre

WORKDIR /app

# 创建上传目录
RUN mkdir -p /app/uploads

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/blog-backend.jar /app/app.jar

# 暴露端口
EXPOSE 8081

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
