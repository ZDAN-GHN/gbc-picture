package com.zdan.gbcpicturebackend.manager.auth.strategy.authloader.spaceuser;

import com.zdan.gbcpicturebackend.constant.ModuleNameConstant;
import com.zdan.gbcpicturebackend.service.SpaceService;

import javax.annotation.Resource;

/**
 * Space 表的 SpaceUserContext 包装策略
 */
@SpaceUserAuthLoader.AuthContextWrapper(moduleName = ModuleNameConstant.SPACE)
public class TSpaceAuthContextWrapper implements SpaceUserAuthLoader.WrapAuthContext {

    @Resource
    private SpaceService spaceService;

    @Override
    public void wrapSpaceUserAuthContext(SpaceUserAuthLoader.SpaceUserAuthContext authContext) {
        Long spaceId = authContext.getId();
        authContext.setSpaceUserId(spaceId);
        authContext.setSpace(spaceService.getById(spaceId));
    }
}
