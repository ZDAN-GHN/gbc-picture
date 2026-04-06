package com.zdan.gbcpicturebackend.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zdan.gbcpicturebackend.model.dto.space.SpaceAddRequest;
import com.zdan.gbcpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.vo.SpaceVO;

/**
 * @author LXH
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2026-01-08 15:46:23
 */
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间信息
     *
     * @param space 空间实体
     * @param add   是否为创建
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取空间包装类实体（单条）
     *
     * @param space 持久层实体
     */
    SpaceVO getSpaceVO(Space space);

    /**
     * 获取空间包装类实体（分页）
     *
     * @param spacePage 持久层分页实体
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

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
     * @param spaceAddRequest 添加空间请求
     * @param loginUser       登录用户
     * @return 空间id
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 检查空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
