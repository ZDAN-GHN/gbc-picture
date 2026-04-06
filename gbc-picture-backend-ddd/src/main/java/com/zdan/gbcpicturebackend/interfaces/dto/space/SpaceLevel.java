package com.zdan.gbcpicturebackend.interfaces.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SpaceLevel {


    /**
     * 子别抽象数值
     */
    private int value;

    /**
     * 级别文字描述
     */
    private String text;

    /**
     * 对应空间最大数量
     */
    private long maxCount;

    /**
     * 对应空间最大大小
     */
    private long maxSize;
}