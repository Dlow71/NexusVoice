package com.nexusvoice.domain.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具参数值对象（纯POJO）
 * 
 * 职责：
 * - 定义工具的输入参数
 * - 描述参数类型和约束
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameter {
    
    /**
     * 参数名称
     */
    private String name;
    
    /**
     * 参数类型：string/number/boolean/object/array
     */
    private String type;
    
    /**
     * 参数描述
     */
    private String description;
    
    /**
     * 是否必需
     */
    @Builder.Default
    private Boolean required = false;
    
    /**
     * 默认值
     */
    private Object defaultValue;
    
    /**
     * 枚举值（如果是枚举类型）
     */
    private String[] enumValues;
    
    /**
     * 最小值（数值类型）
     */
    private Double minValue;
    
    /**
     * 最大值（数值类型）
     */
    private Double maxValue;
    
    /**
     * 正则表达式（字符串类型）
     */
    private String pattern;
    
    /**
     * 示例值
     */
    private String example;
    
    /**
     * 验证参数定义是否有效
     */
    public boolean isValid() {
        return name != null && !name.isEmpty()
            && type != null && !type.isEmpty()
            && description != null && !description.isEmpty();
    }
}

