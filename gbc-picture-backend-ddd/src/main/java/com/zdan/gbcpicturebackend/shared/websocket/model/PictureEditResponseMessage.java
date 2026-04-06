package com.zdan.gbcpicturebackend.shared.websocket.model;

import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片编辑响应消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {

    /**
     * 消息类型，例如 "INFO", "ERROR", "ENTER_EDIT", "EXIT_EDIT", "EDIT_ACTION", "SAVE_EDIT"
     */
    private String type;

    /**
     * 信息
     */
    private String message;

    /**
     * 执行的编辑动作
     */
    private String editAction;

    /**
     * 正在编辑的用户信息
     */
    private UserVO editUser;


    /**
     * todo 用于在用户协同编辑图片的过程中确认了修改后返回给其他用户用于展示
     * 图片新 url
     */
    private String picUrl;
}