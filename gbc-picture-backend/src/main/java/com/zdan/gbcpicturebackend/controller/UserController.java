package com.zdan.gbcpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.annotation.AuthCheck;
import com.zdan.gbcpicturebackend.common.BaseResponse;
import com.zdan.gbcpicturebackend.common.DeleteRequest;
import com.zdan.gbcpicturebackend.common.ResultUtils;
import com.zdan.gbcpicturebackend.constant.UserConstant;
import com.zdan.gbcpicturebackend.exception.ErrorCode;
import com.zdan.gbcpicturebackend.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.model.dto.user.*;
import com.zdan.gbcpicturebackend.model.vo.UserVO;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.vo.UserLoginVO;
import com.zdan.gbcpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 注册新用户
     *
     * @param userRegisterRequest 注册请求实体
     * @return 包装了用户id的响应实体
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 参数校验
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        // 操作数据库并返回结果
        return ResultUtils.success(userService.userRegister(userRegisterRequest));
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest   登录请求实体
     * @param httpServletRequest Servlet包装的用户http请求实体
     * @return 包装了已登录用户视图的响应实体
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                               HttpServletRequest httpServletRequest) {
        // 参数校验
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        // 登录校验
        return ResultUtils.success(userService.userLogin(userLoginRequest, httpServletRequest));
    }

    /**
     * 获取当前登录用户的视图
     *
     * @param request Servlet包装的用户http请求实体
     * @return 包装了已登录用户视图的响应实体
     */
    @GetMapping("/get/login")
    public BaseResponse<UserLoginVO> getLoginUser(HttpServletRequest request) {
        return ResultUtils.success(userService.getUserLoginVO(
                userService.getLoginUser(request)));
    }

    /**
     * 注销当前登录用户（登出）
     *
     * @param httpServletRequest Servlet包装的用户http请求实体
     * @return 包装了登出结果的响应实体
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest httpServletRequest) {
        return ResultUtils.success(userService.userLogout(httpServletRequest));
    }

    // region 简单逻辑不用封装到service中，直接写在controller层

    /**
     * 新增用户
     *
     * @param userAddRequest 新增请求
     * @return 新增的用户的id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> userAdd(UserAddRequest userAddRequest,
                                      HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 设置默认密码
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        final String DEFAULT_USER_PASSWORD = "12345678";
        // 一定记得加密密码
        String userPassword = userService.getEncryptPassword(DEFAULT_USER_PASSWORD);
        user.setUserPassword(userPassword);
        // 掺入数据库
        boolean save = userService.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据用户的id查询用户数据（未脱敏） -- 需要管理员权限
     *
     * @param id 用户id
     * @return 用户的所有数据
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id,
                                          HttpServletRequest request) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "传入id为空");
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR, "传入id不合法");
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(user);
    }

    /**
     * 根据用户id查询用户视图（脱敏）
     *
     * @param id 用户视图
     * @return 脱敏后的用户视图
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id,
                                              HttpServletRequest request) {
        BaseResponse<User> response = getUserById(id, request);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户 -- 需要管理员权限
     *
     * @param deleteRequest 通用删除请求实体
     * @return 包装了删除结果的响应实体
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> userDelete(@RequestBody DeleteRequest deleteRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "删除请求为空！");
        Long id = deleteRequest.getId();
        boolean isDelete = userService.removeById(id);
        ThrowUtils.throwIf(!isDelete, ErrorCode.OPERATION_ERROR, "删除失败");
        return ResultUtils.success(isDelete, "删除成功");
    }


    /**
     * 用户修改 -- 需要管理员权限
     *
     * @param userUpdateRequest 用户更新请求实体
     * @return 包装了修改结果的响应实体
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> userUpdate(@RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR, "删除请求为空！");
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean isUpdate = userService.updateById(user);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "修改失败");
        return ResultUtils.success(isUpdate, "修改成功");

    }

    /**
     * 分页查询用户信息 -- 需要管理员权限
     *
     * @param userQueryRequest 用户查询请求实体
     * @return 包装了用户脱敏分页数据的响应实体
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "用户查询请求为空!");
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> page = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        ThrowUtils.throwIf(page == null, ErrorCode.NOT_FOUND_ERROR);
        // 对数据脱敏
        Page<UserVO> userVOPage = new Page<>();
        BeanUtil.copyProperties(page, userVOPage, "records");
        List<UserVO> userVOList = userService.getUserVOList(page.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
    // endregion
}
