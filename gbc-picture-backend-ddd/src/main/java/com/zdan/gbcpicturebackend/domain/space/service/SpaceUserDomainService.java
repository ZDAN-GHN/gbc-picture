package com.zdan.gbcpicturebackend.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zdan.gbcpicturebackend.domain.space.entity.SpaceUser;
import com.zdan.gbcpicturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员领域服务接口
 *
 * @author LXH
 */
public interface SpaceUserDomainService {

    /**
     * 获取查询条件包装实体
     *
     * @param spaceUserQueryRequest 查询条件
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 添加空间成员
     *
     * @param spaceUser
     * @return 空间id
     */
    Long addSpaceUser(SpaceUser spaceUser);

    SpaceUser getSpaceUser(Long spaceId, Long userId);

    void editSpaceUser(SpaceUser spaceUser);

    List<SpaceUser> listTeamSpaceByUserId(Long userId);

    void deleteSpaceUserById(Long id);

    List<SpaceUser> listSpaceUserByPage(SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request);
}
