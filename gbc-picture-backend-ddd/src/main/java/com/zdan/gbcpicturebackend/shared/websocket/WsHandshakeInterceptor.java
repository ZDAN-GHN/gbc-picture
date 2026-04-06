package com.zdan.gbcpicturebackend.shared.websocket;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.zdan.gbcpicturebackend.domain.picture.repository.PictureRepository;
import com.zdan.gbcpicturebackend.domain.space.repository.SpaceRepository;
import com.zdan.gbcpicturebackend.shared.auth.model.SpaceUserPermissionConstant;
import com.zdan.gbcpicturebackend.shared.auth.strategy.authloader.spaceuser.SpaceUserAuthManager;
import com.zdan.gbcpicturebackend.domain.picture.entity.Picture;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceTypeEnum;
import com.zdan.gbcpicturebackend.application.service.UserApplicationService;
import com.zdan.gbcpicturebackend.infrastructure.utils.ThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手拦截器，用于建立连接前鉴权
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureRepository pictureRepository;

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 建立连接前需要鉴权
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 给 WebSocketSession 会话设置属性
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        // 获取当前登录用户，编辑的图片以及空间信息
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // http 升级为 ws 使用的 get 请求，应从请求参数中获取
            String pictureIdStr = httpServletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureIdStr)) {
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            // 获取图片信息
            Picture picture = pictureRepository.getById(pictureIdStr);
            if (ObjectUtil.isEmpty(picture)) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceRepository.getById(spaceId);
                if (ObjectUtil.isEmpty(space)) {
                    log.error("图片所在空间不存在，拒绝握手");
                    return false;
                }
                if (SpaceTypeEnum.TEAM.getValue() != space.getSpaceType()) {
                    log.error("图片所在空间不是团队空间，拒绝握手");
                    return false;
                }
            }
            // 校验用户是否有当前图片的编辑信息
            ThreadLocalUtils.set(picture);
            User loginUser = userApplicationService.getLoginUser(httpServletRequest);
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("{} 在 {} 中无 {} 的编辑权限", loginUser, space, picture);
                return false;
            }
            // 用户有在团队空间中有编辑权限，将用户登录信息等属性设置到 WebSocket 会话中
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.valueOf(pictureIdStr));
            return true;
        }
        // 非 http 请求，拒绝握手
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {

    }
}
