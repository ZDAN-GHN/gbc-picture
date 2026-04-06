package com.zdan.gbcpicturebackend.shared.auth.strategy.authloader.spaceuser;

import com.zdan.gbcpicturebackend.domain.picture.repository.PictureRepository;
import com.zdan.gbcpicturebackend.shared.constant.ModuleNameConstant;

import javax.annotation.Resource;

/**
 * Picture 表的 SpaceUserContext 包装策略
 */
@SpaceUserAuthLoader.AuthContextWrapper(moduleName = ModuleNameConstant.PICTURE)
public class TPictureAuthContextWrapper implements SpaceUserAuthLoader.WrapAuthContext {

    @Resource
    private PictureRepository pictureRepository;

    @Override
    public void wrapSpaceUserAuthContext(SpaceUserAuthLoader.SpaceUserAuthContext authContext) {
        Long pictureId = authContext.getId();
        authContext.setPictureId(pictureId);
        authContext.setPicture(pictureRepository.getById(pictureId));
    }
}
