package com.zdan.gbcpicturebackend.manager.auth.strategy.interfaces;

import java.util.ArrayList;
import java.util.List;

public interface AuthLoaderInterface {

    /**
     * 获取权限列表
     *
     * @param loginId 登录id
     * @return 权限列表
     */
    List<String> getPermissionList(Object loginId);

    /**
     * 获取角色列表（多角色环境下，一个用户可能有多个角色）
     *
     * @param loginId 登录id
     * @return 角色列表
     */
    default List<String> getRoleList(Object loginId) {
        return new ArrayList<>();
    }

    /**
     * 获取鉴权上下文信息
     *
     * @return 上下文信息
     */
    default <T> T getAuthContext() {
        return null;
    }
}
