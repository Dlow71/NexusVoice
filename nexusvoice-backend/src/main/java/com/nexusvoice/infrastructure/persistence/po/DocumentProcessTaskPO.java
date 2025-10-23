package com.nexusvoice.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 文档处理任务持久化对象
 * 包含所有MyBatis-Plus相关的技术注解
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_process_tasks")
public class DocumentProcessTaskPO extends BasePO {

    /**
     * 所属文件ID
     */
    @TableField("file_id")
    private Long fileId;

    /**
     * 任务类型：PARSE-解析，OCR-识别，SPLIT-分割，VECTORIZE-向量化
     */
    @TableField("task_type")
    private String taskType;

    /**
     * 任务状态：PENDING-待处理，RUNNING-执行中，SUCCESS-成功，FAILED-失败，CANCELLED-取消
     */
    @TableField("status")
    private String status;

    /**
     * 优先级（数值越大优先级越高）
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    @TableField("max_retry")
    private Integer maxRetry;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 计划执行时间
     */
    @TableField("scheduled_at")
    private LocalDateTime scheduledAt;

    /**
     * 开始执行时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;
}
