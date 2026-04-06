package com.zdan.gbcpicturebackend.domain.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserQueryRequest;
import com.zdan.gbcpicturebackend.interfaces.vo.user.LoginUserVO;
import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author LXH
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-11-11 14:42:32
 */
public interface UserDomainService {

    /**
     * 新用户注册
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 用户登录
     *
     * @param userAccount
     * @param userPassword
     * @param httpServletRequest
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest httpServletRequest);

    /**
     * 获取脱敏后的用户数据
     *
     * @param user 原生用户数据
     * @return 脱敏后的用户数据
     */
    LoginUserVO getUserLoginVO(User user);

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


    // region --- 为了遵循规范，需要将数据库操作再提取
    Boolean removeById(Long id);

    boolean updateById(User user);

    User getById(long id);

    Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper);

    List<User> listByIds(Set<Long> userIdSet);

    long addUser(User userEntity);
    // endregion 为了遵循规范，需要将数据库操作提取
}