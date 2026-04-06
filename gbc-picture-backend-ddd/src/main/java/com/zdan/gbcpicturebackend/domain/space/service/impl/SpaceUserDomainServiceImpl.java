package com.zdan.gbcpicturebackend.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zdan.gbcpicturebackend.domain.space.entity.SpaceUser;
import com.zdan.gbcpicturebackend.domain.space.repository.SpaceUserRepository;
import com.zdan.gbcpicturebackend.domain.space.service.SpaceUserDomainService;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员领域服务实现
 *
 * @author LXH
 */
@Service
public class SpaceUserDomainServiceImpl implements SpaceUserDomainService {

    @Resource
    private SpaceUserRepository spaceUserRepository;

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

    @Override
    public Long addSpaceUser(SpaceUser spaceUser) {
        // 数据库操作
        boolean result = spaceUserRepository.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    @Override
    public SpaceUser getSpaceUser(Long spaceId, Long userId) {
        return spaceUserRepository.lambdaQuery()
                .eq(SpaceUser::getSpaceId, spaceId)
                .eq(SpaceUser::getUserId, userId)
                .one();
    }

    @Override
    public void editSpaceUser(SpaceUser spaceUser) {
        // 判断是否存在
        long id = spaceUser.getId();
        SpaceUser oldSpaceUser = spaceUserRepository.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceUserRepository.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<SpaceUser> listTeamSpaceByUserId(Long userId) {
        return spaceUserRepository.lambdaQuery()
                .eq(SpaceUser::getUserId, userId)
                .list();
    }

    @Override
    public void deleteSpaceUserById(Long id) {
        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserRepository.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceUserRepository.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<SpaceUser> listSpaceUserByPage(SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request) {
        QueryWrapper<SpaceUser> queryWrapper = this.getQueryWrapper(spaceUserQueryRequest);
        return spaceUserRepository.list(queryWrapper);
    }
}




