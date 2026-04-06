package com.zdan.gbcpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zdan.gbcpicturebackend.model.dto.user.UserLoginRequest;
import com.zdan.gbcpicturebackend.model.dto.user.UserQueryRequest;
import com.zdan.gbcpicturebackend.model.dto.user.UserRegisterRequest;
import com.zdan.gbcpicturebackend.model.vo.UserVO;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zdan.gbcpicturebackend.model.vo.UserLoginVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author LXH
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-11-11 14:42:32
 */
public interface UserService extends IService<User> {

    /**
     * 新用户注册
     *
     * @param userRegisterRequest 注册携带的数据
     * @return 新用户的id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求
     * @return 脱敏后的用户数据
     */
    UserLoginVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest);

    /**
     * 获取脱敏后的用户数据
     *
     * @param user 原生用户数据
     * @return 脱敏后的用户数据
     */
    UserLoginVO getUserLoginVO(User user);

    /**
     * 获取脱敏的用户数据
     *
     * @param user 用户数据
     * @return 脱敏后的用户数据
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户数据列表
     *
     * @param userList 用户数据列表
     * @return 脱敏后的用户数据列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取加密后的密码
     *
     * @param userPassword 原始密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户
     *
     * @param httpServletRequest
     * @return 用户数据
     */
    User getLoginUser(HttpServletRequest httpServletRequest);

    /**
     * 注销当前登录用户（退出登录）
     *
     * @param httpServletRequest
     * @return 用户数据
     */
    Boolean userLogout(HttpServletRequest httpServletRequest);

    /**
     * 获取用户查询请求的条件包装实例
     *
     * @param userQueryRequest 用户查询请求
     * @return 查询条件包装实例
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断用户是否为管理员
     *
     * @param user 要判断的用户
     * @return 判断结果
     */
    boolean isAdmin(User user);
}