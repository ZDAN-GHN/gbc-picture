package com.zdan.gbcpicturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.zdan.gbcpicturebackend.domain.space.entity.SpaceUser;
import com.zdan.gbcpicturebackend.interfaces.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员服务接口
 *
 * @author LXH
 */
public interface SpaceUserApplicationService {

    /**
     * 验证空间成员信息
     *
     * @param spaceUser 要验证的空间成员对象
     * @param add       是否为新增操作(true为新增，false为更新)
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 将SpaceUser实体转换为SpaceUserVO视图对象
     *
     * @param spaceUser 空间成员实体
     * @return 空间成员视图对象
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 将空间成员实体列表转换为视图对象列表
     *
     * @param spaceUserList 空间成员实体列表
     * @return 空间成员视图对象列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 构建查询包装器
     *
     * @param spaceUserQueryRequest 查询请求参数
     * @return 查询包装器
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 添加空间成员
     *
     * @param spaceUser 空间成员对象
     * @return 新增空间成员的ID
     */
    Long addSpaceUser(SpaceUser spaceUser);

    /**
     * 根据查询条件获取空间成员
     *
     * @param spaceUserQueryRequest 查询请求参数
     * @return 空间成员实体
     */
    SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 编辑空间成员信息
     *
     * @param spaceUser 空间成员对象
     * @param request   HTTP请求
     */
    void editSpaceUser(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取我的团队空间列表
     *
     * @param request HTTP请求
     * @return 我的团队空间视图对象列表
     */
    List<SpaceUserVO> listMyTeamSpace(HttpServletRequest request);

    /**
     * 删除空间成员
     *
     * @param deleteRequest 删除请求参数
     * @param request       HTTP请求
     */
    void deleteSpaceUser(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 分页获取空间成员视图列表
     *
     * @param spaceUserQueryRequest 查询请求参数
     * @param request               HTTP请求
     * @return 分页的空间成员视图列表
     */
    List<SpaceUserVO> listSpaceUserVOByPage(SpaceUserQueryRequest spaceUserQueryRequest, HttpServletRequest request);
}


