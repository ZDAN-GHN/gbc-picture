package com.zdan.gbcpicturebackend.shared.auth.strategy.authloader.spaceuser;

import com.zdan.gbcpicturebackend.domain.space.repository.SpaceRepository;
import com.zdan.gbcpicturebackend.shared.constant.ModuleNameConstant;

import javax.annotation.Resource;

/**
 * Space 表的 SpaceUserContext 包装策略
 */
@SpaceUserAuthLoader.AuthContextWrapper(moduleName = ModuleNameConstant.SPACE)
public class TSpaceAuthContextWrapper implements SpaceUserAuthLoader.WrapAuthContext {

    @Resource
    private SpaceRepository spaceRepository;

    @Override
    public void wrapSpaceUserAuthContext(SpaceUserAuthLoader.SpaceUserAuthContext authContext) {
        Long spaceId = authContext.getId();
        authContext.setSpaceUserId(spaceId);
        authContext.setSpace(spaceRepository.getById(spaceId));
    }
}
