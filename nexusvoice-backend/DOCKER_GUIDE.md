# NexusVoice Backend - Docker 部署指南

## 📋 概述

本文档提供NexusVoice后端服务的完整Docker容器化部署方案，包含以下服务：

- **NexusVoice Backend**: Spring Boot 3.3.5 + Java 21 应用服务
- **MySQL 8.0**: 主数据库服务
- **Redis 7.2**: 缓存和会话存储
- **phpMyAdmin**: MySQL数据库管理工具
- **Redis Commander**: Redis管理工具（可选）

## 🚀 快速启动

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少4GB可用内存
- 至少10GB可用磁盘空间

### 一键部署

```bash
# 1. 克隆项目（如果还没有）
git clone <your-repository-url>
cd nexusvoice-backend

# 2. 复制并配置环境变量
cp .env.example .env
# 编辑 .env 文件，配置必要的API密钥等

# 3. 一键启动所有服务
./docker-start.sh

# 或者重新构建镜像并启动
./docker-start.sh --build
```

### 手动部署

```bash
# 1. 准备环境变量文件
cp .env.example .env
vim .env  # 编辑配置

# 2. 创建必要目录
mkdir -p logs uploads docker/mysql/logs docker/mysql/backup

# 3. 启动服务
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看应用日志
docker-compose logs -f nexusvoice-backend
```

## ⚙️ 环境配置

### 必需配置

编辑 `.env` 文件，配置以下关键参数：

```bash
# 数据库配置
DB_NAME=nexusvoice_dev
DB_USERNAME=nexusvoice
DB_PASSWORD=nexusvoice123
MYSQL_ROOT_PASSWORD=root123

# JWT密钥（生产环境请修改）
JWT_SECRET=your-secure-jwt-secret-key

# OpenAI API（如果使用AI功能）
OPENAI_API_KEY=sk-your-openai-api-key

# 其他API密钥
TAVILY_API_KEY=tvly-your-tavily-api-key
SILICONFLOW_API_KEY=sk-your-siliconflow-api-key
```

### 可选配置

```bash
# 端口配置
APP_PORT=8080
MYSQL_PORT=3306
REDIS_PORT=6379
PHPMYADMIN_PORT=8082

# JVM配置
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC

# 调试模式
JVM_DEBUG=false
```

## 🌐 服务访问

服务启动后，可以通过以下地址访问：

| 服务 | 地址 | 描述 |
|------|------|------|
| **应用主服务** | http://localhost:8080 | NexusVoice Backend API |
| **API文档** | http://localhost:8080/swagger-ui.html | Swagger UI接口文档 |
| **健康检查** | http://localhost:8080/actuator/health | 应用健康状态 |
| **Druid监控** | http://localhost:8080/druid | 数据库连接池监控 |
| **phpMyAdmin** | http://localhost:8082 | MySQL数据库管理 |
| **Redis Commander** | http://localhost:8083 | Redis管理（需启用） |

### 默认登录信息

- **phpMyAdmin**: root / root123
- **Druid监控**: admin / admin123
- **Redis Commander**: admin / admin123

## 🛠️ 常用命令

### 脚本命令

```bash
# 启动所有服务
./docker-start.sh start

# 重新构建并启动
./docker-start.sh --build

# 停止所有服务
./docker-start.sh stop

# 重启服务
./docker-start.sh restart

# 查看服务状态
./docker-start.sh status

# 查看应用日志
./docker-start.sh logs

# 查看MySQL日志
./docker-start.sh logs mysql

# 进入应用容器
./docker-start.sh exec

# 清理容器和镜像
./docker-start.sh cleanup
```

### Docker Compose 命令

```bash
# 启动所有服务
docker-compose up -d

# 重新构建并启动
docker-compose up -d --build

# 停止所有服务
docker-compose down

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f [service_name]

# 进入容器
docker-compose exec nexusvoice-backend /bin/bash

# 重启单个服务
docker-compose restart nexusvoice-backend
```

## 🗄️ 数据持久化

### 数据卷说明

- **nexusvoice_mysql_data**: MySQL数据持久化
- **nexusvoice_redis_data**: Redis数据持久化
- **nexusvoice_phpmyadmin_sessions**: phpMyAdmin会话存储

### 备份和恢复

#### MySQL备份

```bash
# 备份数据库
docker-compose exec nexusvoice-mysql mysqldump -u root -proot123 nexusvoice_dev > backup_$(date +%Y%m%d_%H%M%S).sql

# 恢复数据库
docker-compose exec -T nexusvoice-mysql mysql -u root -proot123 nexusvoice_dev < backup.sql
```

#### Redis备份

```bash
# 备份Redis数据
docker-compose exec nexusvoice-redis redis-cli BGSAVE

# 复制RDB文件
docker cp nexusvoice-redis:/data/dump.rdb ./redis_backup_$(date +%Y%m%d_%H%M%S).rdb
```

## 🔧 故障排除

### 常见问题

#### 1. 端口占用

```bash
# 检查端口占用
lsof -i :8080
lsof -i :3306

# 修改 .env 文件中的端口配置
APP_PORT=8081
MYSQL_PORT=3307
```

#### 2. 内存不足

```bash
# 调整JVM内存配置
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC

# 调整MySQL内存配置
# 编辑 docker/mysql/conf.d/nexusvoice.cnf
innodb_buffer_pool_size = 128M
```

#### 3. 数据库连接失败

```bash
# 检查MySQL服务状态
docker-compose logs nexusvoice-mysql

# 重启MySQL服务
docker-compose restart nexusvoice-mysql

# 手动连接测试
docker-compose exec nexusvoice-mysql mysql -u root -proot123
```

#### 4. 应用启动失败

```bash
# 查看应用日志
docker-compose logs nexusvoice-backend

# 检查配置文件
cat .env

# 重新构建镜像
docker-compose build --no-cache nexusvoice-backend
```

### 日志查看

```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs -f nexusvoice-backend
docker-compose logs -f nexusvoice-mysql
docker-compose logs -f nexusvoice-redis

# 查看最近的日志
docker-compose logs --tail=100 nexusvoice-backend
```

## 📊 监控和维护

### 健康检查

```bash
# 应用健康检查
curl http://localhost:8080/actuator/health

# 数据库连接检查
docker-compose exec nexusvoice-mysql mysqladmin ping -u root -proot123

# Redis连接检查
docker-compose exec nexusvoice-redis redis-cli ping
```

### 性能监控

- **应用监控**: http://localhost:8080/actuator/metrics
- **Druid监控**: http://localhost:8080/druid
- **容器监控**: `docker stats`

### 定期维护

```bash
# 清理未使用的镜像
docker image prune -f

# 清理未使用的容器
docker container prune -f

# 清理未使用的数据卷
docker volume prune -f

# 查看磁盘使用情况
docker system df
```

## 🔒 安全配置

### 生产环境建议

1. **修改默认密码**
   ```bash
   # .env 文件中修改
   MYSQL_ROOT_PASSWORD=your-secure-password
   JWT_SECRET=your-secure-jwt-secret
   ```

2. **限制网络访问**
   ```yaml
   # docker-compose.yml 中配置
   ports:
     - "127.0.0.1:3306:3306"  # 只允许本地访问MySQL
   ```

3. **启用HTTPS** (生产环境)
   ```bash
   # 配置SSL证书
   SSL_ENABLED=true
   SSL_KEY_STORE=classpath:keystore.p12
   ```

4. **定期更新镜像**
   ```bash
   docker-compose pull
   docker-compose up -d
   ```

## 📈 扩展配置

### 启用Redis Commander

```bash
# 使用 --profile tools 启动
docker-compose --profile tools up -d

# 访问Redis管理界面
# http://localhost:8083
```

### 自定义网络配置

```yaml
# docker-compose.yml 中自定义网络
networks:
  nexusvoice-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

### 水平扩展

```bash
# 启动多个应用实例
docker-compose up -d --scale nexusvoice-backend=3

# 配置负载均衡器（如Nginx）
```

## 🆘 技术支持

如果遇到问题，请按以下顺序排查：

1. 检查 `.env` 文件配置
2. 查看容器日志 `docker-compose logs`
3. 检查服务状态 `docker-compose ps`
4. 验证网络连接 `docker network ls`
5. 检查资源使用 `docker stats`

更多详细信息请参考：
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Docker官方文档](https://docs.docker.com/)
- [MySQL官方文档](https://dev.mysql.com/doc/)
