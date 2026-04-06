package com.zdan.gbcpicturebackend.interfaces.assembler;

import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.dto.user.AddUserRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 用户对象转换
 */
public class UserAssembler {

    public static User toUserEntity(AddUserRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }

    public static User toUserEntity(UserUpdateRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }
}