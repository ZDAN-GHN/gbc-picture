package com.zdan.gbcpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zdan.gbcpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zdan.gbcpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.zdan.gbcpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author LXH
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2026-01-20 21:01:47
 */
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 校验空间成员
     *
     * @param spaceUser 空间实体
     * @param add       是否为创建
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间包装类实体（单条）
     *
     * @param spaceUser 持久层实体
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获取空间成员包装类实体（分页）
     *
     * @param spaceUserList 持久层分页实体
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询条件包装实体
     *
     * @param spaceUserQueryRequest 查询条件
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 添加空间成员
     *
     * @param spaceUserAddRequest 添加空间请求
     * @return 空间id
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);
}
