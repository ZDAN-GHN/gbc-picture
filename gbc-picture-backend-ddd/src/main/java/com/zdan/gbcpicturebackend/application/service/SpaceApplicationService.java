package com.zdan.gbcpicturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.space.SpaceQueryRequest;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * 空间应用服务接口
 *
 * @author LXH
 */
public interface SpaceApplicationService {

    /**
     * 验证空间信息
     *
     * @param space 要验证的空间对象
     * @param add   是否为新增操作(true为新增，false为更新)
     */
    void validSpace(Space space, boolean add);

    /**
     * 将Space实体转换为SpaceVO视图对象
     *
     * @param space 空间实体
     * @return 空间视图对象
     */
    SpaceVO getSpaceVO(Space space);

    /**
     * 将分页的Space实体列表转换为分页的SpaceVO视图列表
     *
     * @param spacePage 分页的空间实体
     * @return 分页的空间视图对象
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage);

    /**
     * 构建查询包装器
     *
     * @param spaceQueryRequest 查询请求参数
     * @return 查询包装器
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间等级填充空间信息
     *
     * @param space 空间对象
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 添加新的空间
     *
     * @param space   空间对象
     * @param request HTTP请求
     * @return 新增空间的ID
     */
    Long addSpace(Space space, HttpServletRequest request);

    /**
     * 检查空间权限
     *
     * @param loginUser 登录用户
     * @param space     空间对象
     */
    void checkSpaceAuth(User loginUser, Space space);

    /**
     * 根据ID删除空间
     *
     * @param deleteRequest 删除请求参数
     * @param request       HTTP请求
     */
    void deleteSpaceById(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 更新空间信息
     *
     * @param space   要更新的空间对象
     * @param request HTTP请求
     */
    void updateSpace(Space space, HttpServletRequest request);

    /**
     * 根据ID获取空间信息
     *
     * @param id 空间ID
     * @return 空间实体
     */
    Space getSpaceById(Long id);

    /**
     * 根据ID获取空间视图对象
     *
     * @param id      空间ID
     * @param request HTTP请求
     * @return 空间视图对象
     */
    SpaceVO getSpaceVOById(Long id, HttpServletRequest request);

    /**
     * 分页获取空间列表
     *
     * @param spaceQueryRequest 查询请求参数
     * @param request           HTTP请求
     * @return 分页的空间列表
     */
    Page<Space> listSpaceByPage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request);

    /**
     * 编辑空间信息
     *
     * @param space   要编辑的空间对象
     * @param request HTTP请求
     */
    void editSpace(Space space, HttpServletRequest request);

    /**
     * 更新空间使用情况
     *
     * @param spaceRef      空间引用
     * @param newTotalSize  新的总大小
     * @param newTotalCount 新的总数量
     */
    void updateSpaceUsage(Space spaceRef, Long newTotalSize, Long newTotalCount);

    /**
     * 根据ID集合批量查询空间
     *
     * @param spaceIdSet 空间ID集合
     * @return 空间列表
     */
    List<Space> listByIds(Set<Long> spaceIdSet);
}