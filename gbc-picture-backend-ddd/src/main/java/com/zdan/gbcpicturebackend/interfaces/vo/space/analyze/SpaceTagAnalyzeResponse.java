package com.zdan.gbcpicturebackend.interfaces.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片标签分析响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = -2707565779744762053L;

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 使用次数
     */
    private long count;
}