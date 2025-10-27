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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
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
@RequestMapping("/api/dev/mq")
@RequiredArgsConstructor
@Profile({"local", "dev", "test"})
@Tag(name = "开发测试-RocketMQ测试", description = "消息队列功能测试接口（仅开发环境）")
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQTestController {
    
    private final MessageService messageService;
    private final RocketMQHealthIndicator healthIndicator;
    
    @GetMapping("/health")
    @Operation(
        summary = "健康检查", 
        description = "检查RocketMQ服务连接状态，包括生产者状态、超时配置等信息"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "健康检查成功"),
        @ApiResponse(responseCode = "500", description = "RocketMQ服务不健康")
    })
    public Result<RocketMQHealthIndicator.RocketMQHealthStatus> health() {
        RocketMQHealthIndicator.RocketMQHealthStatus status = healthIndicator.checkHealth();
        if (!status.isHealthy()) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "RocketMQ服务不健康: " + status.getStatus());
        }
        return Result.success(status);
    }
    
    @PostMapping("/send/sync")
    @Operation(
        summary = "发送同步消息", 
        description = "发送同步消息到指定主题，等待服务器确认后返回结果。适用于重要消息，需要确保发送成功。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "消息发送成功"),
        @ApiResponse(responseCode = "500", description = "消息发送失败")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "消息请求参数",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = MessageRequest.class),
            examples = @ExampleObject(
                value = "{\"topic\":\"test_topic\",\"content\":{\"message\":\"Hello RocketMQ\"}}"
            )
        )
    )
    public Result<SendResult> sendSyncMessage(@RequestBody @Validated MessageRequest request) {
        log.info("发送同步消息 - Topic: {}, Content: {}", request.getTopic(), request.getContent());
        SendResult result = messageService.sendMessage(request.getTopic(), request.getContent());
        if (!result.isSuccess()) {
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "消息发送失败: " + result.getErrorMessage());
        }
        return Result.success(result);
    }
    
    @PostMapping("/send/async")
    @Operation(
        summary = "发送异步消息", 
        description = "发送异步消息到指定主题，通过回调函数异步获取发送结果。适用于高并发场景。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "消息发送成功"),
        @ApiResponse(responseCode = "500", description = "消息发送失败")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "消息请求参数",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = MessageRequest.class)
        )
    )
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
    
    @PostMapping("/send/oneway")
    @Operation(
        summary = "发送单向消息", 
        description = "发送单向消息，不等待服务器响应。适用于日志收集、数据埋点等对可靠性要求不高的场景。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "消息发送请求已提交")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "消息请求参数",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = MessageRequest.class)
        )
    )
    public Result<String> sendOnewayMessage(@RequestBody @Validated MessageRequest request) {
        log.info("发送单向消息 - Topic: {}, Content: {}", request.getTopic(), request.getContent());
        messageService.sendOnewayMessage(request.getTopic(), request.getContent());
        return Result.success("单向消息已发送，不等待响应");
    }
    
    @PostMapping("/send/delay")
    @Operation(
        summary = "发送延迟消息", 
        description = "发送延迟消息到指定主题，支持18个预设延迟级别（1秒到2小时）。适用于订单超时关闭、定时提醒等场景。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "延迟消息发送成功"),
        @ApiResponse(responseCode = "500", description = "延迟消息发送失败")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "延迟消息请求参数",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = DelayMessageRequest.class),
            examples = @ExampleObject(
                value = "{\"topic\":\"order_timeout_topic\",\"content\":\"ORDER_123456\",\"delayLevel\":\"DELAY_30M\"}"
            )
        )
    )
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
    
    @PostMapping("/send/orderly")
    @Operation(
        summary = "发送顺序消息", 
        description = "发送顺序消息到指定主题，保证同一orderKey的消息严格按照FIFO顺序消费。适用于订单状态流转、金融交易等需要严格顺序的场景。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "顺序消息发送成功"),
        @ApiResponse(responseCode = "500", description = "顺序消息发送失败")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "顺序消息请求参数",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = OrderlyMessageRequest.class),
            examples = @ExampleObject(
                value = "{\"topic\":\"order_status_topic\",\"content\":{\"status\":\"PAID\"},\"orderKey\":\"ORDER_123456\"}"
            )
        )
    )
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
    
    @PostMapping("/send/batch")
    @Operation(
        summary = "批量发送消息", 
        description = "批量发送多条消息到同一主题，提高发送效率，减少网络开销。适用于批量数据同步、批量通知等场景。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "批量消息发送成功"),
        @ApiResponse(responseCode = "500", description = "批量消息发送失败")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "批量消息请求参数",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BatchMessageRequest.class),
            examples = @ExampleObject(
                value = "{\"topic\":\"test_topic\",\"contents\":[\"message1\",\"message2\",\"message3\"]}"
            )
        )
    )
    public Result<SendResult> sendBatchMessages(@RequestBody @Validated BatchMessageRequest request) {
        log.info("批量发送消息 - Topic: {}, 数量: {}", request.getTopic(), request.getContents().size());
        
        List<Object> messages = new ArrayList<>(request.getContents());
        SendResult result = messageService.sendBatchMessages(request.getTopic(), messages);
        
        if (!result.isSuccess()) {
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "批量消息发送失败: " + result.getErrorMessage());
        }
        
        return Result.success(result);
    }
    
    @PostMapping("/test/order")
    @Operation(
        summary = "测试订单场景", 
        description = "模拟完整的订单流程：1.发送订单创建消息 2.发送30分钟延迟的超时关闭消息 3.发送订单状态变更顺序消息。用于演示RocketMQ在订单系统中的应用。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "订单测试场景执行成功")
    })
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
    
    @GetMapping("/delay-levels")
    @Operation(
        summary = "获取延迟级别列表", 
        description = "获取RocketMQ支持的所有延迟级别，包括级别编号、延迟时间和描述。共18个级别：1s、5s、10s、30s、1m、2m、3m、4m、5m、6m、7m、8m、9m、10m、20m、30m、1h、2h。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功获取延迟级别列表")
    })
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
    @Schema(description = "消息请求参数")
    public static class MessageRequest {
        @Schema(description = "消息主题", example = "test_topic", required = true)
        @NotBlank(message = "主题不能为空")
        private String topic;
        
        @Schema(description = "消息内容，支持任意JSON格式", example = "{\"message\":\"Hello RocketMQ\"}", required = true)
        @NotNull(message = "消息内容不能为空")
        private Object content;
    }
    
    /**
     * 延迟消息请求对象
     */
    @Data
    @Schema(description = "延迟消息请求参数")
    public static class DelayMessageRequest {
        @Schema(description = "消息主题", example = "order_timeout_topic", required = true)
        @NotBlank(message = "主题不能为空")
        private String topic;
        
        @Schema(description = "消息内容，支持任意JSON格式", example = "ORDER_123456", required = true)
        @NotNull(message = "消息内容不能为空")
        private Object content;
        
        @Schema(
            description = "延迟级别，可选值：DELAY_1S、DELAY_5S、DELAY_10S、DELAY_30S、DELAY_1M、DELAY_2M、DELAY_3M、DELAY_4M、DELAY_5M、DELAY_6M、DELAY_7M、DELAY_8M、DELAY_9M、DELAY_10M、DELAY_20M、DELAY_30M、DELAY_1H、DELAY_2H", 
            example = "DELAY_30M",
            required = true,
            allowableValues = {"DELAY_1S", "DELAY_5S", "DELAY_10S", "DELAY_30S", "DELAY_1M", "DELAY_2M", "DELAY_3M", "DELAY_4M", "DELAY_5M", "DELAY_6M", "DELAY_7M", "DELAY_8M", "DELAY_9M", "DELAY_10M", "DELAY_20M", "DELAY_30M", "DELAY_1H", "DELAY_2H"}
        )
        @NotBlank(message = "延迟级别不能为空")
        private String delayLevel;
    }
    
    /**
     * 顺序消息请求对象
     */
    @Data
    @Schema(description = "顺序消息请求参数")
    public static class OrderlyMessageRequest {
        @Schema(description = "消息主题", example = "order_status_topic", required = true)
        @NotBlank(message = "主题不能为空")
        private String topic;
        
        @Schema(description = "消息内容，支持任意JSON格式", example = "{\"status\":\"PAID\"}", required = true)
        @NotNull(message = "消息内容不能为空")
        private Object content;
        
        @Schema(description = "顺序键，相同orderKey的消息会保证顺序消费", example = "ORDER_123456", required = true)
        @NotBlank(message = "顺序键不能为空")
        private String orderKey;
    }
    
    /**
     * 批量消息请求对象
     */
    @Data
    @Schema(description = "批量消息请求参数")
    public static class BatchMessageRequest {
        @Schema(description = "消息主题", example = "test_topic", required = true)
        @NotBlank(message = "主题不能为空")
        private String topic;
        
        @Schema(description = "消息内容列表，支持任意JSON格式的数组", example = "[\"message1\", \"message2\", \"message3\"]", required = true)
        @NotNull(message = "消息内容列表不能为空")
        private List<Object> contents;
    }
}
