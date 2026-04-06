package com.zdan.gbcpicturebackend.interfaces.facade;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.infrastructure.annotation.AuthCheck;
import com.zdan.gbcpicturebackend.infrastructure.common.BaseResponse;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.infrastructure.common.ResultUtils;
import com.zdan.gbcpicturebackend.interfaces.assembler.SpaceAssembler;
import com.zdan.gbcpicturebackend.interfaces.dto.space.*;
import com.zdan.gbcpicturebackend.domain.user.constant.UserConstant;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.shared.auth.strategy.authloader.spaceuser.SpaceUserAuthManager;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceLevelEnum;
import com.zdan.gbcpicturebackend.interfaces.vo.space.SpaceVO;
import com.zdan.gbcpicturebackend.application.service.SpaceApplicationService;
import com.zdan.gbcpicturebackend.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest,
                                       HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        Space space = SpaceAssembler.toSpaceEntity(spaceAddRequest);
        Long spaceId = spaceApplicationService.addSpace(space, request);
        return ResultUtils.success(spaceId);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<?> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "删除请求为空！");
        spaceApplicationService.deleteSpaceById(deleteRequest, request);
        return ResultUtils.success("删除成功");
    }

    /**
     * 更新空间 --- admin
     *
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<?> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                       HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceUpdateRequest), ErrorCode.PARAMS_ERROR);
        Space space = SpaceAssembler.toSpaceEntity(spaceUpdateRequest);
        spaceApplicationService.updateSpace(space, request);
        return ResultUtils.success("修改成功");
    }

    /**
     * 根据id获取实体 --- admin
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(Long id) {
        return ResultUtils.success(spaceApplicationService.getSpaceById(id));
    }

    /**
     * 根据id获取封装实体
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(Long id,
                                                HttpServletRequest request) {
        // 参数校验
        return ResultUtils.success(spaceApplicationService.getSpaceVOById(id, request));
    }

    /**
     * 分页查询空间实体 --- admin
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                     HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(spaceApplicationService.listSpaceByPage(spaceQueryRequest, request));
    }

    /**
     * 分页查询空间视图实体
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<Space> spacePage = spaceApplicationService.listSpaceByPage(spaceQueryRequest, request);
        Page<SpaceVO> spaceVOPage = spaceApplicationService.getSpaceVOPage(spacePage);
        return ResultUtils.success(spaceVOPage);
    }

    /**
     * 编辑空间
     *
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<?> editSpace(@RequestBody SpaceEditRequest spaceEditRequest,
                                           HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Space space = SpaceAssembler.toSpaceEntity(spaceEditRequest);
        spaceApplicationService.editSpace(space, request);
        return ResultUtils.success("编辑成功");
    }

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
}