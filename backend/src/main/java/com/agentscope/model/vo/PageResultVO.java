package com.agentscope.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页查询结果视图对象
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResultVO<T> {

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页大小 */
    private Integer size;

    /** 数据列表 */
    private List<T> records;
}
