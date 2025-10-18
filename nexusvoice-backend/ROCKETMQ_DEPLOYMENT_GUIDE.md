# RocketMQ Docker部署指南

## 📋 目录
- [快速开始](#快速开始)
- [详细部署](#详细部署)
- [配置说明](#配置说明)
- [常见问题](#常见问题)
- [性能调优](#性能调优)

## 🚀 快速开始

### 1. 创建Docker Compose文件

在服务器上创建 `docker-compose.yml` 文件：

```yaml
version: '3.8'
services:
  # NameServer
  rocketmq-namesrv:
    image: apache/rocketmq:5.1.4
    container_name: rocketmq-namesrv
    ports:
      - "9876:9876"
    volumes:
      - ./data/namesrv/logs:/home/rocketmq/logs
      - ./data/namesrv/store:/home/rocketmq/store
    command: sh mqnamesrv
    environment:
      - JAVA_OPT_EXT=-server -Xms256m -Xmx256m -Xmn128m
    networks:
      - rocketmq-net
    restart: unless-stopped

  # Broker
  rocketmq-broker:
    image: apache/rocketmq:5.1.4
    container_name: rocketmq-broker
    ports:
      - "10909:10909"
      - "10911:10911"
      - "10912:10912"
    volumes:
      - ./data/broker/logs:/home/rocketmq/logs
      - ./data/broker/store:/home/rocketmq/store
      - ./conf/broker.conf:/home/rocketmq/broker.conf
    command: sh mqbroker -n rocketmq-namesrv:9876 -c /home/rocketmq/broker.conf
    environment:
      - JAVA_OPT_EXT=-server -Xms512m -Xmx512m -Xmn256m
    depends_on:
      - rocketmq-namesrv
    networks:
      - rocketmq-net
    restart: unless-stopped

  # 控制台（可选）
  rocketmq-console:
    image: apacherocketmq/rocketmq-dashboard:latest
    container_name: rocketmq-console
    ports:
      - "8080:8080"
    environment:
      - JAVA_OPTS=-Drocketmq.namesrv.addr=rocketmq-namesrv:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false
    depends_on:
      - rocketmq-namesrv
      - rocketmq-broker
    networks:
      - rocketmq-net
    restart: unless-stopped

networks:
  rocketmq-net:
    driver: bridge
```

### 2. 创建Broker配置文件

创建 `conf/broker.conf` 文件：

```properties
# Broker配置
brokerClusterName = DefaultCluster
brokerName = broker-a
brokerId = 0
deleteWhen = 04
fileReservedTime = 48
brokerRole = ASYNC_MASTER
flushDiskType = ASYNC_FLUSH

# 重要：配置公网IP（替换为你的服务器IP）
brokerIP1 = YOUR_SERVER_IP

# 存储路径
storePathRootDir = /home/rocketmq/store
storePathCommitLog = /home/rocketmq/store/commitlog
storePathConsumeQueue = /home/rocketmq/store/consumequeue
storePathIndex = /home/rocketmq/store/index
storeCheckpoint = /home/rocketmq/store/checkpoint
abortFile = /home/rocketmq/store/abort

# 性能相关
sendMessageThreadPoolNums = 4
pullMessageThreadPoolNums = 8
queryMessageThreadPoolNums = 4
adminBrokerThreadPoolNums = 4
clientManageThreadPoolNums = 4
consumerManageThreadPoolNums = 4
heartbeatThreadPoolNums = 2
endTransactionThreadPoolNums = 4

# 消息大小限制（4MB）
maxMessageSize = 4194304

# 其他配置
serverWorkerThreads = 8
useEpollNativeSelector = false
```

### 3. 创建目录结构

```bash
mkdir -p data/namesrv/logs data/namesrv/store
mkdir -p data/broker/logs data/broker/store
mkdir -p conf
```

### 4. 启动RocketMQ

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 只启动NameServer和Broker（不启动控制台）
docker-compose up -d rocketmq-namesrv rocketmq-broker
```

## 📝 详细配置

### 腾讯云服务器配置

#### 1. 防火墙配置

在腾讯云控制台的安全组中开放以下端口：

| 端口 | 协议 | 说明 |
|------|------|------|
| 9876 | TCP | NameServer端口 |
| 10909 | TCP | Broker VIP通道端口 |
| 10911 | TCP | Broker普通端口 |
| 10912 | TCP | Broker HA端口 |
| 8080 | TCP | 控制台端口（可选） |

#### 2. 修改broker.conf

**重要**：必须将 `brokerIP1` 设置为服务器的公网IP：

```bash
# 获取服务器公网IP
curl ifconfig.me

# 编辑配置文件
vim conf/broker.conf
# 将 YOUR_SERVER_IP 替换为实际的公网IP
```

### Spring Boot应用配置

在 `application-local.yml` 中配置：

```yaml
rocketmq:
  # 使用服务器公网IP
  name-server: YOUR_SERVER_IP:9876
  producer:
    group: nexusvoice_producer
    send-message-timeout: 3000
    max-message-size: 4194304
    retry-times-when-send-failed: 2
    retry-times-when-send-async-failed: 2
```

## 🔧 常用命令

### Docker操作

```bash
# 查看容器状态
docker-compose ps

# 停止服务
docker-compose stop

# 启动服务
docker-compose start

# 重启服务
docker-compose restart

# 删除服务（保留数据）
docker-compose down

# 删除服务和数据
docker-compose down -v
rm -rf data/
```

### 查看日志

```bash
# 查看NameServer日志
docker logs -f rocketmq-namesrv

# 查看Broker日志
docker logs -f rocketmq-broker

# 查看控制台日志
docker logs -f rocketmq-console
```

### 进入容器

```bash
# 进入NameServer容器
docker exec -it rocketmq-namesrv bash

# 进入Broker容器
docker exec -it rocketmq-broker bash
```

## 🎯 测试验证

### 1. 使用控制台测试

访问 `http://YOUR_SERVER_IP:8080` 查看RocketMQ控制台：
- 检查Cluster状态
- 查看Topic列表
- 发送测试消息
- 查看消费者组

### 2. 使用命令行测试

进入Broker容器测试：

```bash
# 进入容器
docker exec -it rocketmq-broker bash

# 设置环境变量
export NAMESRV_ADDR=rocketmq-namesrv:9876

# 创建测试Topic
sh bin/mqadmin updateTopic -c DefaultCluster -n $NAMESRV_ADDR -t test_topic

# 发送测试消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Producer

# 消费测试消息
sh bin/tools.sh org.apache.rocketmq.example.quickstart.Consumer
```

### 3. 使用Java代码测试

运行Spring Boot应用，使用提供的MessageService发送测试消息：

```java
@RestController
@RequestMapping("/api/test/mq")
public class MQTestController {
    
    @Autowired
    private MessageService messageService;
    
    @PostMapping("/send")
    public Result<SendResult> sendTestMessage(@RequestBody String message) {
        SendResult result = messageService.sendMessage("test_topic", message);
        return Result.success(result);
    }
}
```

## 🚨 常见问题

### 1. 连接超时

**问题**：客户端连接RocketMQ超时

**解决方案**：
1. 检查防火墙端口是否开放
2. 确认broker.conf中的`brokerIP1`配置为公网IP
3. 检查NameServer地址配置是否正确

### 2. 消息发送失败

**问题**：RemotingTooMuchRequestException

**解决方案**：
1. 调整发送超时时间
2. 增加重试次数
3. 检查网络延迟

### 3. 内存不足

**问题**：容器频繁重启

**解决方案**：
修改docker-compose.yml中的JVM参数：
```yaml
environment:
  # NameServer（小内存）
  - JAVA_OPT_EXT=-server -Xms128m -Xmx128m -Xmn64m
  # Broker（根据服务器内存调整）
  - JAVA_OPT_EXT=-server -Xms1g -Xmx1g -Xmn512m
```

### 4. 磁盘空间不足

**问题**：消息堆积导致磁盘满

**解决方案**：
1. 定期清理过期消息
2. 调整消息保留时间（fileReservedTime）
3. 增加磁盘空间

## ⚡ 性能调优

### 1. JVM调优

对于生产环境，建议使用以下JVM参数：

```yaml
# Broker JVM参数
JAVA_OPT_EXT: >-
  -server 
  -Xms2g -Xmx2g -Xmn1g
  -XX:+UseG1GC 
  -XX:G1HeapRegionSize=16m
  -XX:G1ReservePercent=25
  -XX:InitiatingHeapOccupancyPercent=30
  -XX:SoftRefLRUPolicyMSPerMB=0
  -XX:+DisableExplicitGC
  -verbose:gc
  -Xloggc:/home/rocketmq/logs/rmq_broker_gc.log
  -XX:+PrintGCDetails
  -XX:+PrintGCDateStamps
```

### 2. Broker配置优化

```properties
# 异步刷盘（性能优先）
flushDiskType = ASYNC_FLUSH

# 同步刷盘（数据安全优先）
# flushDiskType = SYNC_FLUSH

# 消息索引
enableConsumeQueueExt = true
mappedFileSizeConsumeQueue = 300000

# 事务消息
transactionCheckMax = 5
transactionCheckInterval = 60000
```

### 3. 网络优化

```properties
# 使用VIP通道
vipChannelEnabled = true

# 增加网络缓冲区
sendBufferSize = 131072
receiveBufferSize = 131072
```

## 📊 监控告警

### 1. 使用Prometheus监控

添加RocketMQ Exporter：

```yaml
rocketmq-exporter:
  image: apache/rocketmq-exporter:latest
  container_name: rocketmq-exporter
  ports:
    - "5557:5557"
  environment:
    - rocketmq.config.namesrvAddr=rocketmq-namesrv:9876
  networks:
    - rocketmq-net
```

### 2. 关键监控指标

- **消息堆积数**：及时发现消费延迟
- **TPS/QPS**：监控系统吞吐量
- **消费延迟**：跟踪消息处理时效
- **磁盘使用率**：防止磁盘满
- **内存使用率**：预防OOM

## 📚 参考文档

- [RocketMQ官方文档](https://rocketmq.apache.org/docs/)
- [RocketMQ Docker镜像](https://hub.docker.com/r/apache/rocketmq)
- [Spring Boot RocketMQ Starter](https://github.com/apache/rocketmq-spring)

## 🎉 总结

按照以上步骤，你应该能够成功在腾讯云OpenCloudOS 9.4服务器上部署RocketMQ。

关键要点：
1. **必须配置brokerIP1为公网IP**
2. **开放所需的防火墙端口**
3. **根据服务器配置调整JVM参数**
4. **定期监控和维护**

如有问题，请查看容器日志排查或参考官方文档。
