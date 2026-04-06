package com.zdan.gbcpicturebackend.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 编辑图片请求
 */
@Data
public class PictureEditRequest implements Serializable {

    private static final long serialVersionUID = -532438777736263120L;

    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;
}