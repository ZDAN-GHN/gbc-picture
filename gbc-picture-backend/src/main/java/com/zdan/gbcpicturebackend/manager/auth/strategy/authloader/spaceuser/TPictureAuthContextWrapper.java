package com.zdan.gbcpicturebackend.manager.auth.strategy.authloader.spaceuser;

import com.zdan.gbcpicturebackend.constant.ModuleNameConstant;
import com.zdan.gbcpicturebackend.service.PictureService;

import javax.annotation.Resource;

/**
 * Picture 表的 SpaceUserContext 包装策略
 */
@SpaceUserAuthLoader.AuthContextWrapper(moduleName = ModuleNameConstant.PICTURE)
public class TPictureAuthContextWrapper implements SpaceUserAuthLoader.WrapAuthContext {

    @Resource
    private PictureService pictureService;

    @Override
    public void wrapSpaceUserAuthContext(SpaceUserAuthLoader.SpaceUserAuthContext authContext) {
        Long pictureId = authContext.getId();
        authContext.setPictureId(pictureId);
        authContext.setPicture(pictureService.getById(pictureId));
    }
}
