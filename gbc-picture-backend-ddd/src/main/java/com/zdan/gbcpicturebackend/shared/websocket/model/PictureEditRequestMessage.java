package com.zdan.gbcpicturebackend.shared.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图片编辑请求消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage implements Serializable {

    private static final long serialVersionUID = -660580615846144779L;

    /**
     * 消息类型，例如 "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION"
     */
    private String type;

    /**
     * 执行的编辑动作（放大、缩小）
     */
    private String editAction;
}