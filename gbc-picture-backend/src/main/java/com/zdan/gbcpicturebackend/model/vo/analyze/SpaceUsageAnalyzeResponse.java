package com.zdan.gbcpicturebackend.model.vo.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间使用分析响应
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = -7502732490059370662L;

    /**
     * 已使用大小
     */
    private long usedSize;

    /**
     * 总大小
     */
    private Long maxSize;

    /**
     * 空间使用比例
     */
    private Double sizeUsageRatio;

    /**
     * 当前图片数量
     */
    private Long usedCount;

    /**
     * 最大图片数量
     */
    private Long maxCount;

    /**
     * 图片数量占比
     */
    private Double countUsageRatio;
}