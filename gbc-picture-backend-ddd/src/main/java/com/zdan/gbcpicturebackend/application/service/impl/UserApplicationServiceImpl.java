package com.zdan.gbcpicturebackend.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.domain.user.service.UserDomainService;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserLoginRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserQueryRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.user.UserRegisterRequest;
import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.vo.user.LoginUserVO;
import com.zdan.gbcpicturebackend.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * 用户应用服务实现
 *
 * @author LXH
 */
@Service
@Slf4j
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 校验参数
        User.validUserRegister(userAccount, userPassword, checkPassword);
        // 执行注册
        return userDomainService.userRegister(userAccount, userPassword, checkPassword);
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest,
                                 HttpServletRequest request) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        // 校验参数
        User.validUserLogin(userAccount, userPassword);
        // 执行校验
        return userDomainService.userLogin(userAccount, userPassword, request);
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        return userDomainService.getEncryptPassword(userPassword);
    }

    @Override
    public LoginUserVO getUserLoginVO(User user) {
        return userDomainService.getUserLoginVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return userDomainService.getLoginUser(request);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userDomainService.getUserVO(user);
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return userDomainService.getUserVOList(userList);
    }


    @Override
    public Boolean userLogout(HttpServletRequest request) {
        return userDomainService.userLogout(request);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return userDomainService.getQueryWrapper(userQueryRequest);
    }

    @Override
    public User getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public UserVO getUserVOById(long id) {
        return userDomainService.getUserVO(getUserById(id));
    }

    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userDomainService.removeById(deleteRequest.getId());
    }

    @Override
    public void updateUser(User user) {
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userDomainService.page(new Page<>(current, size),
                userDomainService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = userDomainService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVO);
        return userVOPage;
    }

    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userDomainService.listByIds(userIdSet);
    }

    @Override
    public Long addUser(User user) {
        return userDomainService.addUser(user);
    }
}




