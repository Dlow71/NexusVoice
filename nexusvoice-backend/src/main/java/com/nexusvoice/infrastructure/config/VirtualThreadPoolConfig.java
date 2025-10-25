package com.nexusvoice.infrastructure.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 虚拟线程池统一配置
 * 
 * <p>JDK 21+的虚拟线程（Virtual Threads）特性，适用于I/O密集型任务：
 * <ul>
 *   <li>SSE流式聊天 - 长时间阻塞等待AI响应</li>
 *   <li>WebSocket消息处理 - 并发连接可能达到成千上万</li>
 *   <li>TTS语音合成 - 调用外部API的I/O等待</li>
 *   <li>文件迁移 - 大量网络传输任务</li>
 * </ul>
 * 
 * <h3>虚拟线程 vs 传统线程</h3>
 * <table border="1">
 *   <tr><th>特性</th><th>传统平台线程</th><th>虚拟线程</th></tr>
 *   <tr><td>创建成本</td><td>高（MB级内存）</td><td>低（KB级内存）</td></tr>
 *   <tr><td>最大数量</td><td>数千个</td><td>百万级</td></tr>
 *   <tr><td>阻塞成本</td><td>高（线程被占用）</td><td>几乎为零（自动挂起）</td></tr>
 *   <tr><td>适用场景</td><td>CPU密集型</td><td>I/O密集型</td></tr>
 * </table>
 * 
 * @author NexusVoice
 * @since 2025-10-25
 */
@Slf4j
@Configuration
public class VirtualThreadPoolConfig {

    /**
     * SSE流式聊天专用虚拟线程池
     * 
     * <p>用于处理SSE长连接的异步响应流，特点：
     * <ul>
     *   <li>每个连接可能持续数分钟（等待AI逐字返回）</li>
     *   <li>高并发场景下可能有数千个同时连接</li>
     *   <li>阻塞时间长，使用虚拟线程可避免线程耗尽</li>
     * </ul>
     * 
     * @return SSE专用虚拟线程执行器
     */
    @Bean(name = "sseVirtualThreadExecutor", destroyMethod = "shutdown")
    public ExecutorService sseVirtualThreadExecutor() {
        log.info("初始化SSE虚拟线程池");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * TTS语音合成专用虚拟线程池
     * 
     * <p>用于分段TTS的并发调用，特点：
     * <ul>
     *   <li>每次对话可能生成数十个TTS请求</li>
     *   <li>调用外部API，I/O等待时间长（1-5秒/段）</li>
     *   <li>需要并发处理多段音频合成</li>
     * </ul>
     * 
     * @return TTS专用虚拟线程执行器
     */
    @Bean(name = "ttsVirtualThreadExecutor", destroyMethod = "shutdown")
    public ExecutorService ttsVirtualThreadExecutor() {
        log.info("初始化TTS虚拟线程池");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * WebSocket消息处理专用虚拟线程池
     * 
     * <p>用于WebSocket长连接的异步消息处理，特点：
     * <ul>
     *   <li>WebSocket连接可能持续数小时</li>
     *   <li>每个连接的消息处理可能阻塞等待AI</li>
     *   <li>并发连接数可能达到上万</li>
     * </ul>
     * 
     * @return WebSocket专用虚拟线程执行器
     */
    @Bean(name = "webSocketVirtualThreadExecutor", destroyMethod = "shutdown")
    public ExecutorService webSocketVirtualThreadExecutor() {
        log.info("初始化WebSocket虚拟线程池");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 心跳任务专用虚拟线程池
     * 
     * <p>用于SSE/WebSocket的心跳保活机制，特点：
     * <ul>
     *   <li>每个连接一个独立心跳任务</li>
     *   <li>长期运行，定期发送心跳包</li>
     *   <li>轻量级任务，适合虚拟线程</li>
     * </ul>
     * 
     * @return 心跳专用虚拟线程执行器
     */
    @Bean(name = "heartbeatVirtualThreadExecutor", destroyMethod = "shutdown")
    public ExecutorService heartbeatVirtualThreadExecutor() {
        log.info("初始化心跳虚拟线程池");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 通用异步任务虚拟线程池
     * 
     * <p>用于其他I/O密集型异步任务，如：
     * <ul>
     *   <li>文件上传/下载</li>
     *   <li>数据库批量操作</li>
     *   <li>外部API调用</li>
     * </ul>
     * 
     * @return 通用虚拟线程执行器
     */
    @Bean(name = "commonVirtualThreadExecutor", destroyMethod = "shutdown")
    public ExecutorService commonVirtualThreadExecutor() {
        log.info("初始化通用虚拟线程池");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 应用关闭时优雅停止所有线程池
     */
    @PreDestroy
    public void shutdown() {
        log.info("开始关闭所有虚拟线程池...");
        
        shutdownExecutor(sseVirtualThreadExecutor(), "SSE虚拟线程池");
        shutdownExecutor(ttsVirtualThreadExecutor(), "TTS虚拟线程池");
        shutdownExecutor(webSocketVirtualThreadExecutor(), "WebSocket虚拟线程池");
        shutdownExecutor(heartbeatVirtualThreadExecutor(), "心跳虚拟线程池");
        shutdownExecutor(commonVirtualThreadExecutor(), "通用虚拟线程池");
        
        log.info("所有虚拟线程池已关闭");
    }

    /**
     * 优雅关闭单个线程池
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        try {
            log.info("关闭{}...", name);
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("{}未能在10秒内完成，强制关闭", name);
                executor.shutdownNow();
            } else {
                log.info("{}已优雅关闭", name);
            }
        } catch (InterruptedException e) {
            log.error("{}关闭时被中断", name, e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
