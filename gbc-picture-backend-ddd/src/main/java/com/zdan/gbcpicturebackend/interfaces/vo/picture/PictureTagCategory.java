package com.zdan.gbcpicturebackend.interfaces.vo.picture;

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
