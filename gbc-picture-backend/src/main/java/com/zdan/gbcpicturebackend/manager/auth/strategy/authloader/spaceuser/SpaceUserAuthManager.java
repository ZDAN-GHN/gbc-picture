package com.zdan.gbcpicturebackend.manager.auth.strategy.authloader.spaceuser;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zdan.gbcpicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.zdan.gbcpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.zdan.gbcpicturebackend.manager.auth.model.SpaceUserRole;
import com.zdan.gbcpicturebackend.model.entity.Picture;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.SpaceUser;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.enums.SpaceRoleEnum;
import com.zdan.gbcpicturebackend.model.enums.SpaceTypeEnum;
import com.zdan.gbcpicturebackend.service.SpaceUserService;
import com.zdan.gbcpicturebackend.service.UserService;
import com.zdan.gbcpicturebackend.utils.ThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 空间成员权限管理
 */
@Component
@Slf4j
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    private static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String spaceUserAuthConfigJson = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(spaceUserAuthConfigJson, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     *
     * @param roleKey
     * @return
     */
    public List<String> getPermissionsByRole(String roleKey) {
        // 如果角色键不存在，要返回空权限列表
        if (StrUtil.isBlank(roleKey)) return List.of();
        // 获取角色信息
        SpaceUserRole spaceUserROle = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(spaceUserRole -> spaceUserRole.getKey().equals(roleKey))
                .findFirst()
                .orElse(null);
        // 如果没有相应的角色直接返回空权限列表
        if (spaceUserROle == null) return List.of();
        // 返回角色权限列表
        return spaceUserROle.getPermissions();
    }

    /**
     * 获取权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        // 用户未登录
        if (loginUser == null) {
            if (space == null) {
                // 未登录状态下访问公共图库的角色是浏览者，只有查看图片的权限
                return getPermissionsByRole(SpaceRoleEnum.VIEWER.getValue());
            }
            // 访问非公共图库，应是没有任何权限
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                // 系统管理员拥有全部权限
                return ADMIN_PERMISSIONS;
            }
            // 非系统管理员需要根据是否是更改图片的操作来进行鉴权
            List<String> authList = new ArrayList<>();
            // 所有用户都有权限查看公共图库下的图片
            authList.add(SpaceUserPermissionConstant.PICTURE_VIEW);
            Object attach = ThreadLocalUtils.get();
            // 根据传入的图片对象进行校验
            if (attach instanceof Picture) {
                // 公共图库下只有图片所有人才有 编辑、上传 和 删除 图片的权限（即修改图片的权限，上传会导致图片url改变，本质也是一种修改操作）
                if (((Picture) attach).getUserId().equals(loginUser.getId())) {
                    this.pushPictureModifyAuth(authList);
                }
                // 公共图库下非图片所有人只有浏览权限
            } else {
                // 不针对某一张图片的情况下，普通用户具有 上传 和 修改 图片的权限（即添加图片的权限）
                this.pushPictureAddAuth(authList);
            }
            return authList;
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

    private void pushPictureAddAuth(List<String> authList) {
        authList.add(SpaceUserPermissionConstant.PICTURE_EDIT);
        authList.add(SpaceUserPermissionConstant.PICTURE_UPLOAD);
    }

    private void pushPictureModifyAuth(List<String> authList) {
        pushPictureAddAuth(authList);
        authList.add(SpaceUserPermissionConstant.PICTURE_DELETE);
    }
}
