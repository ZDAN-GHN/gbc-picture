package com.zdan.gbcpicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.util.ObjectUtil;
import com.zdan.gbcpicturebackend.manager.auth.strategy.interfaces.AuthLoaderInterface;
import com.zdan.gbcpicturebackend.manager.auth.strategy.register.AuthLoaderAutoRegister;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * 定义 sa-token 获取权限逻辑
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private AuthLoaderAutoRegister authLoaderAutoRegister;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 根据登录类型获取对应权限加载策略
        AuthLoaderInterface authLoader = authLoaderAutoRegister.getAuthLoader(loginType);
        // 如果权限加载策略不存在说明有问题，应拒绝访问
        if (ObjectUtil.isNotEmpty(authLoader)) {
            return authLoader.getPermissionList(loginId);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getRoleList(Object o, String s) {
        return new ArrayList<>();
    }
}
