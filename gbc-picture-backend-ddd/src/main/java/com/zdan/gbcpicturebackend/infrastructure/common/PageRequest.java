package com.zdan.gbcpicturebackend.infrastructure.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页请求类
 */
@Data
public class PageRequest implements Serializable {

    /**
     * 当前页号（默认第一页）
     */
    private int current = 1;

    /**
     * 页面大小（默认一页10条数据）
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}