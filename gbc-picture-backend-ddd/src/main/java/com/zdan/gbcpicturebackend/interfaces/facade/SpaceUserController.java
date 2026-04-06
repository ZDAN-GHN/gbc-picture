package com.zdan.gbcpicturebackend.interfaces.facade;

import cn.hutool.core.util.ObjectUtil;
import com.zdan.gbcpicturebackend.infrastructure.common.BaseResponse;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.infrastructure.common.ResultUtils;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.assembler.SpaceUserAssembler;
import com.zdan.gbcpicturebackend.shared.auth.annotation.SaSpaceCheckPermission;
import com.zdan.gbcpicturebackend.shared.auth.model.SpaceUserPermissionConstant;
import com.zdan.gbcpicturebackend.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.zdan.gbcpicturebackend.domain.space.entity.SpaceUser;
import com.zdan.gbcpicturebackend.interfaces.vo.space.SpaceUserVO;
import com.zdan.gbcpicturebackend.application.service.SpaceUserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = SpaceUserAssembler.toSpaceUserEntity(spaceUserAddRequest);
        long id = spaceUserApplicationService.addSpaceUser(spaceUser);
        return ResultUtils.success(id);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<?> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        spaceUserApplicationService.deleteSpaceUser(deleteRequest, request);
        return ResultUtils.success("删除成功");
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceUserApplicationService.getSpaceUser(spaceUserQueryRequest));
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUserVOByPage(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceUserQueryRequest), ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceUserApplicationService.listSpaceUserVOByPage(spaceUserQueryRequest, request));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<?> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                         HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SpaceUser spaceUser = SpaceUserAssembler.toSpaceUserEntity(spaceUserEditRequest);
        spaceUserApplicationService.editSpaceUser(spaceUser, request);
        return ResultUtils.success("编辑成功");
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        return ResultUtils.success(spaceUserApplicationService.listMyTeamSpace(request));
    }
}