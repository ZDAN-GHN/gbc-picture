package com.zdan.gbcpicturebackend.interfaces.facade;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.infrastructure.annotation.AuthCheck;
import com.zdan.gbcpicturebackend.infrastructure.common.BaseResponse;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.infrastructure.common.ResultUtils;
import com.zdan.gbcpicturebackend.interfaces.assembler.UserAssembler;
import com.zdan.gbcpicturebackend.interfaces.dto.user.*;
import com.zdan.gbcpicturebackend.domain.user.constant.UserConstant;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.vo.user.LoginUserVO;
import com.zdan.gbcpicturebackend.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

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
        return ResultUtils.success(userApplicationService.userRegister(userRegisterRequest));
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest   登录请求实体
     * @param httpServletRequest Servlet包装的用户http请求实体
     * @return 包装了已登录用户视图的响应实体
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                               HttpServletRequest httpServletRequest) {
        // 参数校验
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.userLogin(userLoginRequest, httpServletRequest));
    }

    /**
     * 获取当前登录用户的视图
     *
     * @param request Servlet包装的用户http请求实体
     * @return 包装了已登录用户视图的响应实体
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        return ResultUtils.success(userApplicationService.getUserLoginVO(
                userApplicationService.getLoginUser(request)));
    }

    /**
     * 注销当前登录用户（登出）
     *
     * @param httpServletRequest Servlet包装的用户http请求实体
     * @return 包装了登出结果的响应实体
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest httpServletRequest) {
        return ResultUtils.success(userApplicationService.userLogout(httpServletRequest));
    }

    // region 简单逻辑不用封装到service中，直接写在controller层

    /**
     * 新增用户
     *
     * @param addUserRequest 新增请求
     * @return 新增的用户的id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(AddUserRequest addUserRequest,
                                      HttpServletRequest request) {

        // 参数校验
        ThrowUtils.throwIf(addUserRequest == null, ErrorCode.PARAMS_ERROR);
        User user = UserAssembler.toUserEntity(addUserRequest);
        return ResultUtils.success(userApplicationService.addUser(user));
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
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.getUserById(id));
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
        return ResultUtils.success(userApplicationService.getUserVO(user));
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
        boolean isDelete = userApplicationService.deleteUser(deleteRequest);
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
    public BaseResponse<?> userUpdate(@RequestBody UserUpdateRequest userUpdateRequest,
                                      HttpServletRequest request) {
        User user = UserAssembler.toUserEntity(userUpdateRequest);
        userApplicationService.updateUser(user);
        return ResultUtils.success("修改成功");
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
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userApplicationService.listUserVOByPage(userQueryRequest));
    }
    // endregion
}
