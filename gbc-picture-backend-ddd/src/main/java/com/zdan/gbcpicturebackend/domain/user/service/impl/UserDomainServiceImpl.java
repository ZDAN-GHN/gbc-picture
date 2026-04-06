package com.zdan.gbcpicturebackend.domain.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.domain.user.constant.UserConstant;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.domain.user.repository.UserRepository;
import com.zdan.gbcpicturebackend.domain.user.service.UserDomainService;
import com.zdan.gbcpicturebackend.domain.user.valueobject.UserRoleEnum;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserQueryRequest;
import com.zdan.gbcpicturebackend.interfaces.vo.user.LoginUserVO;
import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;
import com.zdan.gbcpicturebackend.shared.auth.StpKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author LXH
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-11-11 14:42:32
 */
@Service
@Slf4j
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserRepository userRepository;

    @Override
    public long userRegister(String userAccount,
                             String userPassword,
                             String checkPassword) {
        // 验证账号是否已重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        ThrowUtils.throwIf(userRepository.getBaseMapper().selectCount(queryWrapper) > 0,
                ErrorCode.PARAMS_ERROR, "账号已存在");
        // 对密码加密
        userPassword = getEncryptPassword(userPassword);
        // 往数据库中插入一条新记录
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserName("无名");
        user.setUserPassword(userPassword);
        user.setUserRole(UserRoleEnum.USER.getValue());
        userRepository.getBaseMapper().insert(user);
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword,
                                 HttpServletRequest httpServletRequest) {
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", this.getEncryptPassword(userPassword));
        User user = userRepository.getBaseMapper().selectOne(queryWrapper);
        if (user == null) {
            log.info("failed to login, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 保存用户的登录态
        httpServletRequest.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        // 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        // 返回脱敏后的用户数据
        return getUserLoginVO(user);
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "zdan";
        // 参数校验
        if (StrUtil.isEmpty(userPassword)) {
            return null;
        }
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes());
    }

    @Override
    public LoginUserVO getUserLoginVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollectionUtil.isEmpty(userList)) {
            return List.of();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public User getLoginUser(HttpServletRequest httpServletRequest) {
        // 参数校验
        User currentUser = (User) httpServletRequest.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (currentUser == null) {
            // 用户未登录  =>  todo 全基于 session 记录用户信息无法实现会话保持，这里可配合 redis + jwt 令牌
            return null;
        }
        if (currentUser.getId() == null) {
            log.warn("用户id为空");
            return null;
        }
        // 从数据库获取最新用户数据（追求性能考虑注释掉以下代码）
        currentUser = userRepository.getBaseMapper().selectById(currentUser.getId());
        if (currentUser == null) {
            log.error("传递了不存在的用户id");
            return null;
        }
        return currentUser;
    }

    @Override
    public Boolean userLogout(HttpServletRequest httpServletRequest) {
        // 参数校验
        ThrowUtils.throwIf(httpServletRequest == null, ErrorCode.OPERATION_ERROR);
        HttpSession httpSession = httpServletRequest.getSession();
        User user = (User) httpSession.getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwIf(user == null || user.getId() == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 删除登录态数据
        httpSession.removeAttribute(UserConstant.USER_LOGIN_STATE);
        // 删除后获取一下看是否删除成功，相应地作异常处理
        ThrowUtils.throwIf(httpServletRequest.getAttribute(UserConstant.USER_LOGIN_STATE) != null,
                ErrorCode.OPERATION_ERROR, "登出失败");
        log.info("登出成功，用户信息为：\n" + user);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "用户查询请求为空！");
        // 解包
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        // 构建查询条件包装实例
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotNull(userRole), "userRole", userRole);
        queryWrapper.like(ObjectUtil.isNotNull(userName), "userName", userName);
        queryWrapper.like(ObjectUtil.isNotNull(userAccount), "userAccount", userAccount);
        queryWrapper.like(ObjectUtil.isNotNull(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public Boolean removeById(Long id) {
        return userRepository.removeById(id);
    }

    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    @Override
    public User getById(long id) {
        return userRepository.getById(id);
    }

    @Override
    public Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper) {
        return userRepository.page(userPage, queryWrapper);
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userRepository.listByIds(userIdSet);
    }

    @Override
    public long addUser(User user) {
        // 设置默认密码
        final String DEFAULT_USER_PASSWORD = "123456";
        // 一定记得加密密码
        String userPassword = this.getEncryptPassword(DEFAULT_USER_PASSWORD);
        user.setUserPassword(userPassword);
        // 掺入数据库
        boolean save = userRepository.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }
}




