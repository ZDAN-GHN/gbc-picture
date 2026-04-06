package com.zdan.gbcpicturebackend.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量上传请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    private static final long serialVersionUID = 7851115769884178864L;

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix = searchText;
}
