package com.zdan.gbcpicturebackend.domain.user.entity;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;

import com.zdan.gbcpicturebackend.domain.user.valueobject.UserRoleEnum;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户
 *
 * @author LXH
 * @TableName user
 */
@TableName(value = "user")
@EqualsAndHashCode
@Data
public class User implements Serializable {

    private static final long serialVersionUID = 6568018504757332837L;

    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    final static int USER_ACCOUNT_MIN_LENGTH = 4;
    final static int USER_PASSWORD_MIN_LENGTH = 6;

    /**
     * 校验用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     */
    public static void validUserRegister(String userAccount,
                                         String userPassword,
                                         String checkPassword) {
        // 校验是参数是否合法
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 校验用户账号长度
        else if (userAccount.length() < USER_ACCOUNT_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        // 校验密码长度
        else if (
                userPassword.length() < USER_PASSWORD_MIN_LENGTH
                || checkPassword.length() < USER_PASSWORD_MIN_LENGTH
        ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度不能小于6位");
        }
        // 校验两次密码是否一致
        else if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    /**
     * 校验用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     */
    public static void validUserLogin(String userAccount, String userPassword) {


        // 校验是参数是否合法
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 校验账号长度
        else if (userAccount.length() < USER_ACCOUNT_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        // 校验密码长度
        else if (userPassword.length() < USER_PASSWORD_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码长度不能小于6位");
        }
    }

    /**
     * 判断用户是否为管理员
     *
     * @return true 为管理员
     */
    public boolean isAdmin() {
        return UserRoleEnum.ADMIN.getValue()
                .equals(this.getUserRole());
    }

    /**
     * 判断用户是否不为管理员
     *
     * @return true 不为管理员
     */
    public boolean isNotAdmin() {
        return !this.isAdmin();
    }
}