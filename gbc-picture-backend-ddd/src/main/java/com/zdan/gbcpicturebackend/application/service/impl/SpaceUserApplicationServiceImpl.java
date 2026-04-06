package com.zdan.gbcpicturebackend.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zdan.gbcpicturebackend.application.service.SpaceApplicationService;
import com.zdan.gbcpicturebackend.domain.space.service.SpaceUserDomainService;
import com.zdan.gbcpicturebackend.domain.user.service.UserDomainService;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.space.entity.SpaceUser;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceRoleEnum;
import com.zdan.gbcpicturebackend.interfaces.vo.space.SpaceUserVO;
import com.zdan.gbcpicturebackend.interfaces.vo.space.SpaceVO;
import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;
import com.zdan.gbcpicturebackend.application.service.SpaceUserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 空间成员应用服务实现
 *
 * @author LXH
 */
@Service
@Slf4j
public class SpaceUserApplicationServiceImpl implements SpaceUserApplicationService {

    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    @Lazy  // 延迟加载（懒加载），避免循环依赖
    private SpaceApplicationService spaceApplicationService;

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = this.getUserEntity(spaceUser);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = this.getSpaceEntity(spaceUser);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        if (ObjectUtil.isEmpty(spaceUser)) {
            return new SpaceUserVO();
        }
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        User user = this.getUserEntity(spaceUser);
        UserVO userVO = userDomainService.getUserVO(user);
        spaceUserVO.setUser(userVO);
        // 关联查询空间信息
        Space space = this.getSpaceEntity(spaceUser);
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        spaceUserVO.setSpace(spaceVO);
        return spaceUserVO;
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream()
                .map(SpaceUserVO::objToVo)
                .collect(Collectors.toList());
        // 1. 收集需要关联查询的用户 ID 和空间 ID
        Set<Long> userIdSet = spaceUserList.stream()
                .map(SpaceUser::getUserId)
                .collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream()
                .map(SpaceUser::getSpaceId)
                .collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userDomainService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceApplicationService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3. 填充 SpaceUserVO 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(userDomainService.getUserVO(user));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceUserQueryRequest), ErrorCode.PARAMS_ERROR);
        return spaceUserDomainService.getQueryWrapper(spaceUserQueryRequest);
    }

    @Override
    public Long addSpaceUser(SpaceUser spaceUser) {
        // 参数校验
        this.validSpaceUser(spaceUser, true);
        return spaceUserDomainService.addSpaceUser(spaceUser);
    }

    @Override
    public SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        if (ObjectUtil.hasEmpty(spaceId, userId)) {
            log.error("传递了错误的参数, spaceId: {}, userId: {}", spaceId, userId);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return spaceUserDomainService.getSpaceUser(spaceId, userId);
    }

    @Override
    public void editSpaceUser(SpaceUser spaceUser, HttpServletRequest request) {
        this.validSpaceUser(spaceUser, false);
        spaceUserDomainService.editSpaceUser(spaceUser);
    }

    @Override
    public List<SpaceUserVO> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userDomainService.getLoginUser(request);
        List<SpaceUser> spaceUserList = spaceUserDomainService.listTeamSpaceByUserId(loginUser.getId());
        return this.getSpaceUserVOList(spaceUserList);
    }

    @Override
    public void deleteSpaceUser(DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        spaceUserDomainService.deleteSpaceUserById(deleteRequest.getId());
    }

    @Override
    public List<SpaceUserVO> listSpaceUserVOByPage(SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        List<SpaceUser> spaceUserList = spaceUserDomainService.listSpaceUserByPage(spaceUserQueryRequest, request);
        return this.getSpaceUserVOList(spaceUserList);
    }

    private User getUserEntity(SpaceUser spaceUser) {
        return userDomainService.getById(spaceUser.getUserId());
    }

    private Space getSpaceEntity(SpaceUser spaceUser) {
        return spaceApplicationService.getSpaceById(spaceUser.getSpaceId());
    }
}




