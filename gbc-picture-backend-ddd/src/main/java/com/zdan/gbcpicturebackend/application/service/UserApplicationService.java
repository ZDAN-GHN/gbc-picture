package com.zdan.gbcpicturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserLoginRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserQueryRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserRegisterRequest;
import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.vo.user.LoginUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * 用户应用服务接口
 *
 * @author LXH
 */
public interface UserApplicationService {

    /**
     * 新用户注册
     *
     * @param userRegisterRequest 用户注册请求参数
     * @return 注册成功的用户ID
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest   用户登录请求参数
     * @param httpServletRequest HTTP请求对象
     * @return 包含用户登录信息的LoginUserVO对象
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest);

    /**
     * 获取脱敏后的用户登录数据
     *
     * @param user 原始用户数据
     * @return 脱敏后的LoginUserVO对象
     */
    LoginUserVO getUserLoginVO(User user);

    /**
     * 获取脱敏的用户数据
     *
     * @param user 原始用户数据
     * @return 脱敏后的UserVO对象
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户数据列表
     *
     * @param userList 原始用户数据列表
     * @return 脱敏后的UserVO对象列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取加密后的密码
     *
     * @param userPassword 原始密码
     * @return 加密后的密码字符串
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户
     *
     * @param httpServletRequest HTTP请求对象
     * @return 当前登录的User对象
     */
    User getLoginUser(HttpServletRequest httpServletRequest);

    /**
     * 注销当前登录用户（退出登录）
     *
     * @param httpServletRequest HTTP请求对象
     * @return 注销操作是否成功
     */
    Boolean userLogout(HttpServletRequest httpServletRequest);

    /**
     * 获取用户查询请求的条件包装实例
     *
     * @param userQueryRequest 用户查询请求参数
     * @return 查询条件包装器
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 根据用户ID获取用户信息
     *
     * @param id 用户ID
     * @return User对象，包含用户详细信息
     */
    User getUserById(long id);

    /**
     * 根据用户ID获取用户视图对象(UserVO)
     *
     * @param id 用户唯一标识符
     * @return UserVO 包含用户详细信息的视图对象
     */
    UserVO getUserVOById(long id);

    /**
     * 删除用户
     *
     * @param deleteRequest 删除请求参数
     * @return 删除操作是否成功
     */
    boolean deleteUser(DeleteRequest deleteRequest);

    /**
     * 更新用户信息
     *
     * @param user 用户对象
     */
    void updateUser(User user);

    /**
     * 分页获取用户视图列表
     *
     * @param userQueryRequest 用户查询请求参数
     * @return 分页的用户视图对象
     */
    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    /**
     * 根据ID集合批量查询用户
     *
     * @param userIdSet 用户ID集合
     * @return 用户列表
     */
    List<User> listByIds(Set<Long> userIdSet);

    /**
     * 添加新用户
     *
     * @param user 用户对象
     * @return 新增用户的ID
     */
    Long addUser(User user);
}
