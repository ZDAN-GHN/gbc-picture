package com.zdan.gbcpicturebackend.shared.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zdan.gbcpicturebackend.domain.picture.repository.PictureRepository;
import com.zdan.gbcpicturebackend.shared.websocket.disruptor.PictureEditEventProducer;
import com.zdan.gbcpicturebackend.shared.websocket.model.PictureEditActionEnum;
import com.zdan.gbcpicturebackend.shared.websocket.model.PictureEditMessageTypeEnum;
import com.zdan.gbcpicturebackend.shared.websocket.model.PictureEditRequestMessage;
import com.zdan.gbcpicturebackend.shared.websocket.model.PictureEditResponseMessage;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片协同编辑处理器
 */
@Component
@Slf4j
/*
todo 目前新加入的编辑者是无法看到已编辑的状态的，这可以通过 redis 保存记录来解决，
     此外，还要提供一个接口让前端轮询获取图片的编辑状态，对未实时同步的任务做补偿执行
     可设置存入 redis 的值为一个携带 当前图片的版本号 + 最新任务列表的对象，版本号为最新任务列表的长度
 */
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户（脱敏的）
    private final Map<Long, User> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    // region --- 方法实现

    /**
     * 连接建立成功，保存 WebSocketSession 并广播反馈消息
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        this.pictureSessions.computeIfAbsent(pictureId, key -> new HashSet<>()).add(session);
        // 构造响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        pictureEditResponseMessage.setMessage(String.format("用户 %s 加入编辑", user.getUserName()));
        pictureEditResponseMessage.setEditUser(userApplicationService.getUserVO(this.pictureEditingUsers.get(pictureId)));
        // todo 查询 redis 获取加入前已执行的所有命令
        // 广播给所有当前图片的协同编辑者（包括自己）
        this.broadcastToPictureEditor(pictureId, pictureEditResponseMessage);
    }

    /**
     * 收到前端消息，根据消息类型处理消息
     *
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        // 获取消息内容，将 JSON 转换为 PictureEditRequestMessage
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        // 从 session 属性中获取到公共参数
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 根据消息处理（生产消息到 Disruptor 环形队列中 => 使用任务队列以提升系统吞吐量）
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, user, pictureId);
    }


    /**
     * 连接关闭后，将 WebSocketSession 从集合中移除，并广播退出消息
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        // 图片处于协同编辑才能移除会话
        this.pictureSessions.computeIfPresent(pictureId, (key, value) -> {
            value.remove(session);
            return value;
        });
        // 判断当前用户是否为编辑用户，如果是，还需要移除
        if (this.pictureEditingUsers.get(pictureId).equals(user)) {
            this.pictureEditingUsers.remove(pictureId);
        }
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
        pictureEditResponseMessage.setEditUser(userApplicationService.getUserVO(this.pictureEditingUsers.get(pictureId)));
        pictureEditResponseMessage.setMessage(String.format("用户 %s 退出", user.getUserName() != null ? user.getUserName() : user.getUserAccount()));
        this.broadcastToPictureEditor(pictureId, pictureEditResponseMessage);
    }
    // endregion 方法实现

    // region --- 公有自定义方法

    /**
     * 处理协同编辑消息
     */
    public void handleEditMessage(PictureEditMessageTypeEnum messageTypeEnum,
                                  PictureEditRequestMessage pictureEditRequestMessage,
                                  WebSocketSession session,
                                  User user,
                                  Long pictureId) {
        switch (messageTypeEnum) {
            case ENTER_EDIT:
                handleEnterEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EXIT_EDIT:
                handleExitEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case EDIT_ACTION:
                handleEditActionMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            case SAVE_EDIT:
                handleSaveEditMessage(pictureEditRequestMessage, session, user, pictureId);
                break;
            default:
                this.errorEditMessageFeedback(session, user, "消息类型不合法");
                log.error("{} 用户在 {} 中发送了错误的消息类型 {}", user.getId(), session, messageTypeEnum);
                break;
        }
        // todo 将用户所有成功操作记录到 redis 中
    }

    /**
     * 处理进入协同编辑消息
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                       WebSocketSession session,
                                       User user,
                                       Long pictureId) {
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        // 没有用户正在编辑该图片，才能进入编辑
        User editUser = this.pictureEditingUsers.get(pictureId);
        if (editUser == null) {
            this.pictureEditingUsers.put(pictureId, user);
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("用户 %s 进入编辑", user.getUserName() != null ? user.getUserName() : user.getUserAccount()));
            pictureEditResponseMessage.setEditUser(userApplicationService.getUserVO(user));
            // 广播给所有的用户
            successEditMessageBroadcastFeedBack(pictureId, pictureEditResponseMessage, null);
        }
        // 有人正在编辑，发送错误消息给当前用户
        else {
            this.errorEditMessageFeedback(session, user, String.format("用户 %s 正在编辑该图片", editUser.getUserName() != null ? editUser.getUserName() : editUser.getUserAccount()));
        }
    }

    /**
     * 处理退出协同编辑消息
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                      WebSocketSession session,
                                      User user,
                                      Long pictureId) {
        User editUser = this.pictureEditingUsers.get(pictureId);
        // 当前用户为图片的编辑者才能执行退出编辑
        if (editUser != null && editUser.equals(user)) {
            this.pictureEditingUsers.remove(pictureId);
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("用户 %s 退出编辑", user.getUserName() != null ? user.getUserName() : user.getUserAccount()));
            pictureEditResponseMessage.setEditUser(null);
            // 将消息广播给所有协同编辑者
            successEditMessageBroadcastFeedBack(pictureId, pictureEditResponseMessage, null);
        }
        // 当前用户不是编辑者，需要给当前用户返回反馈信息
        else {
            this.errorEditMessageFeedback(session, user, "您非当前图片的编辑者");
        }
    }

    /**
     * 处理协同编辑动作消息
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                        WebSocketSession session,
                                        User user,
                                        Long pictureId) {
        User editUser = this.pictureEditingUsers.get(pictureId);
        // 当前用户为图片的编辑者才能执行编辑操作
        if (editUser != null && editUser.equals(user)) {
            String editAction = pictureEditRequestMessage.getEditAction();
            PictureEditActionEnum editActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
            // 操作合法才执行
            if (editActionEnum != null) {
                PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
                responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
                responseMessage.setMessage(String.format("用户 %s 执行了 %s", user.getUserName() != null ? user.getUserName() : user.getUserAccount(), editActionEnum.getText()));
                responseMessage.setEditAction(editAction);
                responseMessage.setEditUser(userApplicationService.getUserVO(user));
                // 广播给其他编辑者（不能发给自己，否则会重复编辑）
                this.successEditMessageBroadcastFeedBack(pictureId, responseMessage, session);
            }
            // 操作不合法需要反馈给客户端
            else {
                this.errorEditMessageFeedback(session, user, "非法的图片操作");
                log.error("{} 用户在 {} 中发送了非法的图片操作 {}", user.getId(), session.getId(), editAction);
            }
        }
        // 当前用户不是编辑者，需要给当前用户返回反馈信息
        else {
            this.errorEditMessageFeedback(session, user, "您非当前图片的编辑者");
        }
    }

    /**
     * 处理保存修改请求
     */
    public void handleSaveEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
                                      WebSocketSession session,
                                      User user,
                                      Long pictureId) {
        User editUser = this.pictureEditingUsers.get(pictureId);
        // 当前用户为图片的编辑者才能执行编辑操作
        if (editUser != null && editUser.equals(user)) {
            this.pictureEditingUsers.remove(pictureId);
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.SAVE_EDIT.getValue());
            pictureEditResponseMessage.setMessage(String.format("用户 %s 保存了图片", user.getUserName() != null ? user.getUserName() : user.getUserAccount()));
            pictureEditResponseMessage.setPicUrl(pictureRepository.getById(pictureId).getUrl());
            this.successEditMessageBroadcastFeedBack(pictureId, pictureEditResponseMessage, null);
        }
        // 当前用户不是编辑者，需要给当前用户返回反馈信息
        else {
            this.errorEditMessageFeedback(session, user, "您非当前图片的编辑者");
        }
    }

    // 请求处理成功后广播反馈
    private void successEditMessageBroadcastFeedBack(Long pictureId,
                                                     PictureEditResponseMessage pictureEditResponseMessage,
                                                     WebSocketSession excludeSession) {
        try {
            this.broadcastToPictureEditor(pictureId, pictureEditResponseMessage, excludeSession);
        } catch (IOException e) {
            log.error("消息广播失败", e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 发送错误反馈消息
     */
    private void errorEditMessageFeedback(WebSocketSession session,
                                          User user,
                                          String message) {
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
        pictureEditResponseMessage.setMessage(message);
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pictureEditResponseMessage)));
        } catch (IOException e) {
            log.error("错误消息反馈失败", e);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * 广播给该图片的所有协同编辑者（支持排除掉某个session）
     */
    private void broadcastToPictureEditor(Long pictureId,
                                          PictureEditResponseMessage pictureEditResponseMessage,
                                          WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessionSet = this.pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            for (WebSocketSession webSocketSession : sessionSet) {
                // 广播给其他的所有协同编辑者
                if (
                        webSocketSession != null &&
                                webSocketSession.isOpen() &&
                                !webSocketSession.equals(excludeSession)
                ) {
                    // 将消息转为 json 前需要注意消息的值中包含 Long 类型数据，
                    // 需要用上自定义的 json 序列化器，否则前端接收参数的时候会精度丢失
                    String textMessage = this.objectMapper.writeValueAsString(pictureEditResponseMessage);
                    webSocketSession.sendMessage(new TextMessage(textMessage));
                }
            }
        }
    }

    /**
     * 广播给该图片的所有协同编辑者（包括消息发送人本人）
     */
    private void broadcastToPictureEditor(Long pictureId,
                                          PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        broadcastToPictureEditor(pictureId, pictureEditResponseMessage, null);
    }
    // endregion 私有自定义方法
}
