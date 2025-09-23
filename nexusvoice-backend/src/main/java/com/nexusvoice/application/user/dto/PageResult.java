package com.nexusvoice.application.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 分页结果DTO
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Integer current;

    @Schema(description = "每页大小")
    private Integer size;

    @Schema(description = "总页数")
    private Integer pages;

    // 构造函数
    public PageResult() {}

    public PageResult(List<T> records, Long total, Integer current, Integer size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = (int) Math.ceil((double) total / size);
    }

    // Getter and Setter methods
    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "total=" + total +
                ", current=" + current +
                ", size=" + size +
                ", pages=" + pages +
                ", recordsSize=" + (records != null ? records.size() : 0) +
                '}';
    }
}
