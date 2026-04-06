package com.zdan.gbcpicturebackend.controller;

import com.zdan.gbcpicturebackend.common.BaseResponse;
import com.zdan.gbcpicturebackend.common.ResultUtils;
import com.zdan.gbcpicturebackend.exception.ErrorCode;
import com.zdan.gbcpicturebackend.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.model.dto.space.analyze.*;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.vo.analyze.*;
import com.zdan.gbcpicturebackend.service.SpaceAnalyzeService;
import com.zdan.gbcpicturebackend.service.UserService;
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
    private UserService userService;

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> analyzeSpaceUsage(@RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest,
                                                                     HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 空间使用情况分析
        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUsageAnalyzeResponse);
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> analyzeSpaceCategory(@RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
                                                                                 HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 空间使用情况分析
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResponseList = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceCategoryAnalyzeResponseList);
    }

    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> analyzeSpaceTag(@RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest,
                                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 空间使用情况分析
        List<SpaceTagAnalyzeResponse> spaceTagAnalyzeResponseList = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceTagAnalyzeResponseList);
    }

    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> analyzeSpaceSize(@RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                         HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 空间使用情况分析
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyzeResponseList = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceSizeAnalyzeResponseList);
    }

    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> analyzeSpaceUser(@RequestBody SpaceUserAnalyzeRequest spaceSizeAnalyzeRequest,
                                                                         HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 空间使用情况分析
        List<SpaceUserAnalyzeResponse> spaceUserAnalyzeResponseList = spaceAnalyzeService.getSpaceUserAnalyze(spaceSizeAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceUserAnalyzeResponseList);
    }

    @PostMapping("/rank")
    public BaseResponse<List<Space>> analyzeSpaceRank(@RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,
                                                      HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR, "请求体为空");
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 空间使用情况分析
        List<Space> spaceList = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
        return ResultUtils.success(spaceList);
    }
}
