#!/bin/bash

echo "🚀 启动NexusVoice MySQL开发环境..."

# 检查Docker是否运行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker未运行，请先启动Docker"
    exit 1
fi

# 启动MySQL数据库
echo "📦 启动MySQL数据库..."
docker-compose -f docker-compose-mysql.yml up -d

# 等待数据库启动
echo "⏳ 等待MySQL数据库启动..."
sleep 15

# 检查数据库连接
echo "🔍 检查数据库连接..."
if docker exec nexusvoice-mysql-dev mysqladmin ping -h localhost -u root -proot > /dev/null 2>&1; then
    echo "✅ MySQL数据库启动成功"
else
    echo "❌ MySQL数据库启动失败"
    exit 1
fi

# 编译项目
echo "🔨 编译项目..."
mvn clean compile

if [ $? -eq 0 ]; then
    echo "✅ 项目编译成功"
    echo ""
    echo "🎉 MySQL开发环境启动完成！"
    echo ""
    echo "📊 数据库信息："
    echo "   - MySQL: localhost:3306"
    echo "   - 数据库名: nexusvoice_dev"
    echo "   - 用户名: root"
    echo "   - 密码: root"
    echo ""
    echo "🔧 管理工具："
    echo "   - phpMyAdmin: http://localhost:8081"
    echo "   - 用户名: root"
    echo "   - 密码: root"
    echo ""
    echo "🚀 启动应用："
    echo "   mvn spring-boot:run"
    echo ""
    echo "📚 API文档："
    echo "   http://localhost:8080/swagger-ui.html"
    echo ""
    echo "👤 默认管理员账户："
    echo "   - 邮箱: admin@nexusvoice.com"
    echo "   - 密码: admin123"
else
    echo "❌ 项目编译失败"
    exit 1
fi
