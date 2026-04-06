package com.zdan.gbcpicturebackend.interfaces.facade;

import com.zdan.gbcpicturebackend.infrastructure.common.BaseResponse;
import com.zdan.gbcpicturebackend.infrastructure.common.ResultUtils;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.dto.space.analyze.*;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.vo.space.analyze.*;
import com.zdan.gbcpicturebackend.application.service.SpaceAnalyzeApplicationService;
import com.zdan.gbcpicturebackend.application.service.UserApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;


@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceAnalyzeApplicationService spaceAnalyzeApplicationService;

    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> analyzeSpaceUsage(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                                     HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 空间使用情况分析
        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = spaceAnalyzeApplicationService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageAnalyzeResponse);
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> analyzeSpaceCategory(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                                 HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 空间使用情况分析
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResponseList = spaceAnalyzeApplicationService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceCategoryAnalyzeResponseList);
    }

    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> analyzeSpaceTag(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 空间使用情况分析
        List<SpaceTagAnalyzeResponse> spaceTagAnalyzeResponseList = spaceAnalyzeApplicationService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceTagAnalyzeResponseList);
    }

    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> analyzeSpaceSize(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                         HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 空间使用情况分析
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyzeResponseList = spaceAnalyzeApplicationService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceSizeAnalyzeResponseList);
    }

    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> analyzeSpaceUser(@RequestBody SpaceUserAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                         HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 空间使用情况分析
        List<SpaceUserAnalyzeResponse> spaceUserAnalyzeResponseList = spaceAnalyzeApplicationService.getSpaceUserAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUserAnalyzeResponseList);
    }

    @PostMapping("/rank")
    public BaseResponse<List<Space>> analyzeSpaceRank(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                      HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        // 空间使用情况分析
        List<Space> spaceList = spaceAnalyzeApplicationService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceList);
    }
}
