# NexusVoice Backend Dockerfile
# 基于OpenJDK 21 + Spring Boot 3.3.5
# 
# 构建命令：
# docker build -t nexusvoice/backend:latest .
#
# 运行命令：
# docker run -p 8080:8080 nexusvoice/backend:latest

# ===========================================
# 构建阶段 - 使用Maven构建应用
# ===========================================
FROM maven:3.9.6-openjdk-21-slim AS builder

# 设置工作目录
WORKDIR /app

# 设置Maven镜像源（加速依赖下载）
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' >> /root/.m2/settings.xml && \
    echo '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' >> /root/.m2/settings.xml && \
    echo '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0' >> /root/.m2/settings.xml && \
    echo '          http://maven.apache.org/xsd/settings-1.0.0.xsd">' >> /root/.m2/settings.xml && \
    echo '  <mirrors>' >> /root/.m2/settings.xml && \
    echo '    <mirror>' >> /root/.m2/settings.xml && \
    echo '      <id>aliyun</id>' >> /root/.m2/settings.xml && \
    echo '      <mirrorOf>central</mirrorOf>' >> /root/.m2/settings.xml && \
    echo '      <name>Aliyun Central</name>' >> /root/.m2/settings.xml && \
    echo '      <url>https://maven.aliyun.com/repository/public</url>' >> /root/.m2/settings.xml && \
    echo '    </mirror>' >> /root/.m2/settings.xml && \
    echo '  </mirrors>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

# 复制Maven配置文件，利用Docker层缓存
COPY pom.xml .

# 下载依赖（利用Docker缓存，只有pom.xml变化时才重新下载）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用（跳过测试以加速构建）
RUN mvn clean package -DskipTests -B

# ===========================================
# 运行阶段 - 使用轻量级JRE运行应用
# ===========================================
FROM openjdk:21-jdk-slim AS runtime

# 安装必要的工具
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    netcat-openbsd \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户（安全最佳实践）
RUN groupadd -r nexusvoice && \
    useradd -r -g nexusvoice -s /bin/false nexusvoice

# 设置工作目录
WORKDIR /app

# 设置时区为中国时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 创建日志和上传目录
RUN mkdir -p /app/logs /app/uploads && \
    chown -R nexusvoice:nexusvoice /app

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# JVM调试参数（仅在DEBUG=true时启用）
ENV JVM_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# 设置应用参数
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8080

# 暴露端口
EXPOSE 8080 5005

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 创建启动脚本
RUN echo '#!/bin/sh' > /app/start.sh && \
    echo 'set -e' >> /app/start.sh && \
    echo '' >> /app/start.sh && \
    echo '# 等待数据库服务启动' >> /app/start.sh && \
    echo 'echo "等待数据库连接..."' >> /app/start.sh && \
    echo 'while ! nc -z ${DB_HOST:-localhost} ${DB_PORT:-3306}; do' >> /app/start.sh && \
    echo '  echo "等待MySQL启动... (${DB_HOST:-localhost}:${DB_PORT:-3306})"' >> /app/start.sh && \
    echo '  sleep 2' >> /app/start.sh && \
    echo 'done' >> /app/start.sh && \
    echo 'echo "数据库连接成功!"' >> /app/start.sh && \
    echo '' >> /app/start.sh && \
    echo '# 等待Redis服务启动（如果配置了Redis）' >> /app/start.sh && \
    echo 'if [ ! -z "${REDIS_HOST}" ]; then' >> /app/start.sh && \
    echo '  echo "等待Redis连接..."' >> /app/start.sh && \
    echo '  while ! nc -z ${REDIS_HOST} ${REDIS_PORT:-6379}; do' >> /app/start.sh && \
    echo '    echo "等待Redis启动... (${REDIS_HOST}:${REDIS_PORT:-6379})"' >> /app/start.sh && \
    echo '    sleep 2' >> /app/start.sh && \
    echo '  done' >> /app/start.sh && \
    echo '  echo "Redis连接成功!"' >> /app/start.sh && \
    echo 'fi' >> /app/start.sh && \
    echo '' >> /app/start.sh && \
    echo '# 构建Java命令' >> /app/start.sh && \
    echo 'JAVA_CMD="java"' >> /app/start.sh && \
    echo '' >> /app/start.sh && \
    echo '# 添加基础JVM参数' >> /app/start.sh && \
    echo 'JAVA_CMD="$JAVA_CMD $JAVA_OPTS"' >> /app/start.sh && \
    echo '' >> /app/start.sh && \
    echo '# 如果启用调试模式，添加调试参数' >> /app/start.sh && \
    echo 'if [ "${JVM_DEBUG}" = "true" ]; then' >> /app/start.sh && \
    echo '  echo "启用JVM远程调试模式，端口: ${JVM_DEBUG_PORT:-5005}"' >> /app/start.sh && \
    echo '  DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=${JVM_DEBUG_SUSPEND:-n},address=*:${JVM_DEBUG_PORT:-5005}"' >> /app/start.sh && \
    echo '  JAVA_CMD="$JAVA_CMD $DEBUG_OPTS"' >> /app/start.sh && \
    echo 'fi' >> /app/start.sh && \
    echo '' >> /app/start.sh && \
    echo '# 添加应用JAR' >> /app/start.sh && \
    echo 'JAVA_CMD="$JAVA_CMD -jar /app/app.jar"' >> /app/start.sh && \
    echo '' >> /app/start.sh && \
    echo '# 打印启动信息' >> /app/start.sh && \
    echo 'echo "=========================================="' >> /app/start.sh && \
    echo 'echo "NexusVoice Backend 正在启动..."' >> /app/start.sh && \
    echo 'echo "Profile: ${SPRING_PROFILES_ACTIVE:-default}"' >> /app/start.sh && \
    echo 'echo "Port: ${SERVER_PORT:-8080}"' >> /app/start.sh && \
    echo 'echo "JVM Debug: ${JVM_DEBUG:-false}"' >> /app/start.sh && \
    echo 'if [ "${JVM_DEBUG}" = "true" ]; then' >> /app/start.sh && \
    echo '  echo "Debug Port: ${JVM_DEBUG_PORT:-5005}"' >> /app/start.sh && \
    echo 'fi' >> /app/start.sh && \
    echo 'echo "=========================================="' >> /app/start.sh && \
    echo '' >> /app/start.sh && \
    echo '# 启动应用' >> /app/start.sh && \
    echo 'exec $JAVA_CMD' >> /app/start.sh && \
    chmod +x /app/start.sh && \
    chown nexusvoice:nexusvoice /app/start.sh

# 切换到应用用户
USER nexusvoice

# 启动命令
CMD ["/app/start.sh"]

# 元数据标签
LABEL maintainer="NexusVoice Team" \
      version="1.0.0" \
      description="NexusVoice Backend API Server" \
      org.opencontainers.image.title="NexusVoice Backend" \
      org.opencontainers.image.description="AI-powered voice assistant backend service" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.created="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
      org.opencontainers.image.source="https://github.com/nexusvoice/nexusvoice-backend" \
      org.opencontainers.image.licenses="MIT"
