package com.zdan.gbcpicturebackend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间使用排行分析请求（仅管理员）
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = -105380605231046458L;

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10; // 默认取前 10 条
}