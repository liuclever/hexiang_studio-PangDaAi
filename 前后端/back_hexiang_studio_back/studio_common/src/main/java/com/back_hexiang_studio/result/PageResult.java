package com.back_hexiang_studio.result;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class PageResult implements Serializable {
    // 总记录数
    private long total;
    // 当前页的记录
    private List records;
    // 当前页码
    private Integer page;
    // 每页大小
    private Integer pageSize;
    // 总页数
    private Integer pages;
    // 是否有下一页
    private Boolean hasNext;
    // 是否有上一页
    private Boolean hasPrevious;

    // 构造函数
    public PageResult(long total,List records) {
        // 确保total不为0，如果为0但有记录，则使用记录数
        this.total = (total > 0) ? total : (records != null ? records.size() : 0);
        this.records = records;
    }

    // 构造函数
    public PageResult(long total, List records, Integer page, Integer pageSize, Integer pages) {
        this.total = (total > 0) ? total : (records != null ? records.size() : 0);
        this.records = records;
        this.page = page;
        this.pageSize = pageSize;
        this.pages = pages;
        this.hasNext = page < pages;
        this.hasPrevious = page > 1;
    }
}
