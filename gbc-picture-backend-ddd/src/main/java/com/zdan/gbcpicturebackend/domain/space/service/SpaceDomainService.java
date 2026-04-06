package com.zdan.gbcpicturebackend.domain.space.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.dto.space.SpaceQueryRequest;

import java.util.List;
import java.util.Set;

/**
 * 空间领域服务接口
 *
 * @author LXH
 */
public interface SpaceDomainService {

    /**
     * 校验空间信息
     *
     * @param space 空间实体
     * @param add   是否为创建
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取查询条件包装实体
     *
     * @param spaceQueryRequest 查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间信息
     *
     * @param space 空间实体
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 添加空间
     *
     * @param space     添加空间请求
     * @param loginUser 登录用户
     * @return 空间id
     */
    Long addSpace(Space space, User loginUser);

    /**
     * 检查空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);

    Space getSpaceById(Long spaceId);

    List<Space> listByIds(Set<Long> spaceIdSet);

    void deleteSpaceById(Long spaceId, User loginUser);

    void updateSpace(Space space, User loginUser);

    Page<Space> listSpaceByPage(SpaceQueryRequest spaceQueryRequest);

    void editSpace(Space space, User loginUser);

    void updateSpaceUsage(Space spaceRef, Long newTotalSize, Long newTotalCount);
}
