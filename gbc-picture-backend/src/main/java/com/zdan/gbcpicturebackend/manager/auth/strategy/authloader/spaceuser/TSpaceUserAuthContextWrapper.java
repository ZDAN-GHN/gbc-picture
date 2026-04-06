package com.zdan.gbcpicturebackend.manager.auth.strategy.authloader.spaceuser;

import com.zdan.gbcpicturebackend.constant.ModuleNameConstant;

/**
 * SpaceUser 表的 SpaceUserContext 包装策略
 */
@SpaceUserAuthLoader.AuthContextWrapper(moduleName = ModuleNameConstant.SPACE_USER)
public class TSpaceUserAuthContextWrapper implements SpaceUserAuthLoader.WrapAuthContext {

    @Override
    public void wrapSpaceUserAuthContext(SpaceUserAuthLoader.SpaceUserAuthContext authContext) {
        authContext.setSpaceId(authContext.getId());
    }
}
