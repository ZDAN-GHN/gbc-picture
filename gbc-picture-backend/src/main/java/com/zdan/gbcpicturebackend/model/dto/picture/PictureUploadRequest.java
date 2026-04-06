package com.zdan.gbcpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传图片请求
 */
@Data
public class PictureUploadRequest implements Serializable {

    private static final long serialVersionUID = 694800440605966914L;

    /**
     * 图片 id，用于追溯已上传的图片，做后续可能的修改
     */
    private Long id;

    /**
     * 文件地址 url
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间id
     */
    private Long spaceId = null;
}
