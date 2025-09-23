# 配置文件设置说明

## 概述

本项目使用了配置文件分离的方案来保护敏感信息，避免将数据库密码、JWT密钥等敏感信息提交到版本控制系统中。

## 文件结构

- `application.yml` - 主配置文件（可提交到Git）
- `application-local.yml` - 本地配置文件（包含敏感信息，已被Git忽略）
- `.gitignore` - Git忽略文件配置

## 配置说明

### 1. 主配置文件 (application.yml)
- 包含非敏感的应用配置
- 通过 `spring.profiles.include: local` 引入本地配置
- 敏感信息使用占位符或注释说明

### 2. 本地配置文件 (application-local.yml)
- 包含所有敏感信息：
  - 数据库连接信息（用户名、密码）
  - JWT密钥
  - Druid监控页面登录信息
  - 其他第三方服务密钥

### 3. Git忽略配置 (.gitignore)
- 忽略 `**/application-local.yml` 文件
- 忽略其他敏感配置文件和临时文件

## 使用方法

### 首次设置
1. 复制 `application-local.yml` 到本地
2. 根据你的环境修改其中的配置信息
3. 确保该文件不会被提交到Git

### 团队协作
1. 每个开发者需要创建自己的 `application-local.yml`
2. 可以提供一个 `application-local.yml.example` 作为模板
3. 在项目文档中说明需要配置的敏感信息

### 部署到生产环境
1. 生产环境使用环境变量或配置管理系统
2. 不要在生产环境使用 `application-local.yml`
3. 确保生产环境的敏感信息安全存储

## 安全注意事项

1. ✅ `application-local.yml` 已添加到 `.gitignore`
2. ✅ 主配置文件中移除了所有敏感信息
3. ✅ 使用了合理的默认值和占位符
4. ⚠️ 定期检查是否有敏感信息意外提交
5. ⚠️ 生产环境使用更安全的配置管理方案

## 验证配置

运行以下命令验证配置是否正确：

```bash
# 检查Git状态，确认敏感文件被忽略
git status

# 启动应用验证配置加载
mvn spring-boot:run
```

如果配置正确，应用应该能够正常启动并连接到数据库。
