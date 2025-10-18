package com.nexusvoice.interfaces.api.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.nexusvoice.application.mq.service.MessageService;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.mq.constants.MQTopicConstants;
import com.nexusvoice.domain.mq.enums.DelayLevelEnum;
import com.nexusvoice.domain.mq.model.SendResult;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.infrastructure.mq.monitor.RocketMQHealthIndicator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * RocketMQ测试接口
 * 用于测试消息队列的各种功能
 * 
 * @author Dlow
 * @date 2025/10/18
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test/mq")
@RequiredArgsConstructor
@Tag(name = "RocketMQ测试接口", description = "测试消息队列的各种功能")
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQTestController {
    
    private final MessageService messageService;
    private final RocketMQHealthIndicator healthIndicator;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查RocketMQ连接状态")
    public Result<RocketMQHealthIndicator.RocketMQHealthStatus> health() {
        RocketMQHealthIndicator.RocketMQHealthStatus status = healthIndicator.checkHealth();
        if (!status.isHealthy()) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "RocketMQ服务不健康: " + status.getStatus());
        }
        return Result.success(status);
    }
    
    /**
     * 发送同步消息
     */
    @PostMapping("/send/sync")
    @Operation(summary = "发送同步消息", description = "发送同步消息到指定主题")
    public Result<SendResult> sendSyncMessage(@RequestBody @Validated MessageRequest request) {
        log.info("发送同步消息 - Topic: {}, Content: {}", request.getTopic(), request.getContent());
        SendResult result = messageService.sendMessage(request.getTopic(), request.getContent());
        if (!result.isSuccess()) {
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "消息发送失败: " + result.getErrorMessage());
        }
        return Result.success(result);
    }
    
    /**
     * 发送异步消息
     */
    @PostMapping("/send/async")
    @Operation(summary = "发送异步消息", description = "发送异步消息到指定主题")
    public Result<SendResult> sendAsyncMessage(@RequestBody @Validated MessageRequest request) {
        log.info("发送异步消息 - Topic: {}, Content: {}", request.getTopic(), request.getContent());
        CompletableFuture<SendResult> future = messageService.sendAsyncMessage(request.getTopic(), request.getContent());
        
        try {
            SendResult result = future.get(); // 等待异步发送完成
            if (!result.isSuccess()) {
                throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "消息发送失败: " + result.getErrorMessage());
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("异步消息发送异常", e);
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "异步消息发送异常: " + e.getMessage());
        }
    }
    
    /**
     * 发送单向消息
     */
    @PostMapping("/send/oneway")
    @Operation(summary = "发送单向消息", description = "发送单向消息（不关心发送结果）")
    public Result<String> sendOnewayMessage(@RequestBody @Validated MessageRequest request) {
        log.info("发送单向消息 - Topic: {}, Content: {}", request.getTopic(), request.getContent());
        messageService.sendOnewayMessage(request.getTopic(), request.getContent());
        return Result.success("单向消息已发送，不等待响应");
    }
    
    /**
     * 发送延迟消息
     */
    @PostMapping("/send/delay")
    @Operation(summary = "发送延迟消息", description = "发送延迟消息到指定主题")
    public Result<SendResult> sendDelayMessage(@RequestBody @Validated DelayMessageRequest request) {
        log.info("发送延迟消息 - Topic: {}, DelayLevel: {}, Content: {}", 
            request.getTopic(), request.getDelayLevel(), request.getContent());
        
        DelayLevelEnum delayLevel = DelayLevelEnum.valueOf(request.getDelayLevel());
        SendResult result = messageService.sendDelayMessage(request.getTopic(), request.getContent(), delayLevel);
        
        if (!result.isSuccess()) {
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "延迟消息发送失败: " + result.getErrorMessage());
        }
        
        return Result.success(result);
    }
    
    /**
     * 发送顺序消息
     */
    @PostMapping("/send/orderly")
    @Operation(summary = "发送顺序消息", description = "发送顺序消息到指定主题")
    public Result<SendResult> sendOrderlyMessage(@RequestBody @Validated OrderlyMessageRequest request) {
        log.info("发送顺序消息 - Topic: {}, OrderKey: {}, Content: {}", 
            request.getTopic(), request.getOrderKey(), request.getContent());
        
        SendResult result = messageService.sendOrderlyMessage(
            request.getTopic(), request.getContent(), request.getOrderKey());
        
        if (!result.isSuccess()) {
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "顺序消息发送失败: " + result.getErrorMessage());
        }
        
        return Result.success(result);
    }
    
    /**
     * 批量发送消息
     */
    @PostMapping("/send/batch")
    @Operation(summary = "批量发送消息", description = "批量发送多条消息")
    public Result<SendResult> sendBatchMessages(@RequestBody @Validated BatchMessageRequest request) {
        log.info("批量发送消息 - Topic: {}, 数量: {}", request.getTopic(), request.getContents().size());
        
        List<Object> messages = new ArrayList<>(request.getContents());
        SendResult result = messageService.sendBatchMessages(request.getTopic(), messages);
        
        if (!result.isSuccess()) {
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "批量消息发送失败: " + result.getErrorMessage());
        }
        
        return Result.success(result);
    }
    
    /**
     * 测试订单场景
     */
    @PostMapping("/test/order")
    @Operation(summary = "测试订单场景", description = "模拟订单创建和超时关闭")
    public Result<Map<String, Object>> testOrderScenario() {
        String orderId = "ORDER_" + RandomUtil.randomNumbers(10);
        Map<String, Object> results = new HashMap<>();
        
        // 1. 创建订单消息
        Map<String, Object> orderInfo = new HashMap<>();
        orderInfo.put("orderId", orderId);
        orderInfo.put("userId", RandomUtil.randomLong(10000, 99999));
        orderInfo.put("amount", RandomUtil.randomDouble(100.00, 1000.00));
        orderInfo.put("createTime", DateUtil.now());
        
        SendResult createResult = messageService.sendMessage(MQTopicConstants.TOPIC_ORDER_CREATE, orderInfo);
        results.put("createOrder", createResult);
        log.info("订单创建消息发送: {}", createResult.isSuccess() ? "成功" : "失败");
        
        // 2. 发送订单超时关闭延迟消息（30分钟后）
        SendResult timeoutResult = messageService.sendDelayMessage(
            MQTopicConstants.TOPIC_ORDER_TIMEOUT, orderId, DelayLevelEnum.DELAY_30M);
        results.put("orderTimeout", timeoutResult);
        log.info("订单超时关闭消息发送（30分钟延迟）: {}", timeoutResult.isSuccess() ? "成功" : "失败");
        
        // 3. 发送订单状态变更消息
        Map<String, Object> statusChange = new HashMap<>();
        statusChange.put("orderId", orderId);
        statusChange.put("oldStatus", "CREATED");
        statusChange.put("newStatus", "PENDING_PAYMENT");
        statusChange.put("changeTime", DateUtil.now());
        
        SendResult statusResult = messageService.sendOrderlyMessage(
            MQTopicConstants.TOPIC_ORDER_STATUS_CHANGE, statusChange, orderId);
        results.put("statusChange", statusResult);
        log.info("订单状态变更消息发送: {}", statusResult.isSuccess() ? "成功" : "失败");
        
        return Result.success(results);
    }
    
    /**
     * 测试各种延迟级别
     */
    @GetMapping("/delay-levels")
    @Operation(summary = "获取延迟级别", description = "获取所有支持的延迟级别")
    public Result<List<Map<String, Object>>> getDelayLevels() {
        List<Map<String, Object>> levels = new ArrayList<>();
        for (DelayLevelEnum level : DelayLevelEnum.values()) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", level.name());
            info.put("level", level.getLevel());
            info.put("time", level.getTime());
            info.put("description", level.getDesc());
            levels.add(info);
        }
        return Result.success(levels);
    }
    
    /**
     * 消息请求对象
     */
    @Data
    public static class MessageRequest {
        @NotBlank(message = "主题不能为空")
        private String topic;
        
        @NotNull(message = "消息内容不能为空")
        private Object content;
    }
    
    /**
     * 延迟消息请求对象
     */
    @Data
    public static class DelayMessageRequest {
        @NotBlank(message = "主题不能为空")
        private String topic;
        
        @NotNull(message = "消息内容不能为空")
        private Object content;
        
        @NotBlank(message = "延迟级别不能为空")
        private String delayLevel;
    }
    
    /**
     * 顺序消息请求对象
     */
    @Data
    public static class OrderlyMessageRequest {
        @NotBlank(message = "主题不能为空")
        private String topic;
        
        @NotNull(message = "消息内容不能为空")
        private Object content;
        
        @NotBlank(message = "顺序键不能为空")
        private String orderKey;
    }
    
    /**
     * 批量消息请求对象
     */
    @Data
    public static class BatchMessageRequest {
        @NotBlank(message = "主题不能为空")
        private String topic;
        
        @NotNull(message = "消息内容列表不能为空")
        private List<Object> contents;
    }
}
