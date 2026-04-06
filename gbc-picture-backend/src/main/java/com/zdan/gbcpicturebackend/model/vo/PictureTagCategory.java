package com.zdan.gbcpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片标签和分类
 */
@Data
public class PictureTagCategory {

    private List<String> categoryList;

    private List<String> tagList;
}
