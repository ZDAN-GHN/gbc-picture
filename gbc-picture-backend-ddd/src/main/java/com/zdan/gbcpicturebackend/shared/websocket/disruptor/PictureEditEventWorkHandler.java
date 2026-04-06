package com.zdan.gbcpicturebackend.shared.websocket.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.zdan.gbcpicturebackend.shared.websocket.PictureEditHandler;
import com.zdan.gbcpicturebackend.shared.websocket.model.PictureEditMessageTypeEnum;
import com.zdan.gbcpicturebackend.shared.websocket.model.PictureEditRequestMessage;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

/**
 * 图片协同编辑事件处理器（消费者）
 */
@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum messageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
        // 根据消息处理
        pictureEditHandler.handleEditMessage(messageTypeEnum, pictureEditRequestMessage, session, user, pictureId);
    }
}
