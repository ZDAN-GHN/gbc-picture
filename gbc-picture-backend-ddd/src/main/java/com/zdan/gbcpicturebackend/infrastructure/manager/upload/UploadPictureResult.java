package com.zdan.gbcpicturebackend.infrastructure.manager.upload;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传图片后的结果
 */
@Data
public class UploadPictureResult implements Serializable {

    private static final long serialVersionUID = -3043070693317907425L;

    /**
     * 图片地址
     */
    private String url;

    /**
     * 压缩图url
     */
    private String compressedUrl;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;
}
