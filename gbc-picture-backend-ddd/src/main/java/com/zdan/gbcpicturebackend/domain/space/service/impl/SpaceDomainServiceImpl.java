package com.zdan.gbcpicturebackend.domain.space.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.space.entity.SpaceUser;
import com.zdan.gbcpicturebackend.domain.space.repository.SpaceRepository;
import com.zdan.gbcpicturebackend.domain.space.service.SpaceDomainService;
import com.zdan.gbcpicturebackend.domain.space.service.SpaceUserDomainService;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceLevelEnum;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceRoleEnum;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceTypeEnum;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.dto.space.SpaceQueryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 空间领域服务实现
 *
 * @author LXH
 */
@Service
public class SpaceDomainServiceImpl implements SpaceDomainService {

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserDomainService spaceUserDomainService;

    /*
    // 分表对代码的改动很大，这里不再使用了，分表只做学习，不做应用
    @Resource
    @Lazy // 延迟加载（懒加载），避免循环依赖
    private DynamicShardingManager dynamicShardingManager;
    */

    private final Map<Long, Object> spaceLockMap = new ConcurrentHashMap<>();

    @Override
    public void validSpace(Space space, boolean add) {
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
        // 创建时额外校验
        if (add) {
            // 校验空间名称是否为空
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            // 校验空间级别
            ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceLevel), ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            // 校验空间类型
            ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceType), ErrorCode.PARAMS_ERROR, "空间类型不能为空");
        }
        /* 添加空间或修改空间的公共校验 */
        // 校验空间名称是否过长
        final int spaceNameMaxLength = 30;
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > spaceNameMaxLength) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        // 校验空间级别是否存在
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 校验空间类别是否存在
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }
    }

    @Override
    public Long addSpace(Space space, User loginUser) {
        // 设定默认值
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (ObjectUtil.isEmpty(space.getSpaceLevel())) {
            // 默认是普通级别
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (ObjectUtil.isEmpty(space.getSpaceType())) {
            // 默认是私有空间
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 控制同一用户只能创建一个私有空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        Object lock = spaceLockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            Long spaceId;
            try {
                spaceId = transactionTemplate.execute(status -> {
                    boolean existent = spaceRepository.lambdaQuery()
                            .eq(Space::getUserId, loginUser.getId())
                            .eq(Space::getSpaceType, space.getSpaceType())
                            .exists();
                    // 如果已有空间，就不能创建
                    ThrowUtils.throwIf(existent, ErrorCode.PARAMS_ERROR, "每个用户每类空间只能创建一个");
                    boolean spaceSave = spaceRepository.save(space);
                    ThrowUtils.throwIf(!spaceSave, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                    // 创建成功后，如果是团队空间，关联新增团队成员记录
                    if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(space.getUserId());
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        spaceUserDomainService.addSpaceUser(spaceUser);
                        /*
                        // 分表对代码的改动很大，这里不再使用了，分表只做学习，不做应用
                        // 仅为旗舰版团队空间创建分表
                        if (space.getSpaceLevel() == SpaceLevelEnum.FLAGSHIP.getValue()) {
                            dynamicShardingManager.createSpacePictureTable(space);
                        }
                        */
                    }
                    // 返回新创建的空间的数据 id
                    return space.getId();
                });
            } finally {
                // 事务完成就要及时释放内存，避免OOM（内存溢出）
                spaceLockMap.remove(loginUser.getId());
            }
            return spaceId;
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可编辑
        if (!space.getSpaceOwner().equals(loginUser.getId()) && loginUser.isNotAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public Space getSpaceById(Long spaceId) {
        return spaceRepository.getById(spaceId);
    }

    @Override
    public List<Space> listByIds(Set<Long> spaceIdSet) {
        return spaceRepository.listByIds(spaceIdSet);
    }

    @Override
    public void deleteSpaceById(Long spaceId, User loginUser) {
        Space oldSpace = spaceRepository.getById(spaceId);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        if (
                !oldSpace.getSpaceOwner().equals(loginUser.getId()) && loginUser.isNotAdmin()
        ) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 删除数据库记录
        boolean isDelete = spaceRepository.removeById(spaceId);
        ThrowUtils.throwIf(!isDelete, ErrorCode.OPERATION_ERROR, "删除失败");
    }

    @Override
    public void updateSpace(Space space, User loginUser) {
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        space.setUpdateTime(new Date());
        // 对实体进行校验
        this.validSpace(space, false);
        Space oldSpace = spaceRepository.getById(space.getId());
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.PARAMS_ERROR, "空间不存在");
        // 真正修改
        boolean isUpdate = spaceRepository.updateById(space);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR, "修改失败");
    }

    @Override
    public Page<Space> listSpaceByPage(SpaceQueryRequest spaceQueryRequest) {
        // 设定好查询条件
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        Page<Space> page = new Page<>(current, pageSize);
        QueryWrapper<Space> queryWrapper = this.getQueryWrapper(spaceQueryRequest);
        // 分页查询
        return spaceRepository.page(page, queryWrapper);
    }

    @Override
    public void editSpace(Space space, User loginUser) {
        // 自动填充数据
        this.fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 数据校验
        this.validSpace(space, false);
        // 判断是否存在
        long id = space.getId();
        Space oldSpace = spaceRepository.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        this.checkSpaceAuth(loginUser, oldSpace);
        // 操作数据库
        boolean result = spaceRepository.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void updateSpaceUsage(Space spaceRef, Long newTotalSize, Long newTotalCount) {
        Long spaceId = spaceRef.getId();
        Long totalSize = spaceRef.getTotalSize();
        Long totalCount = spaceRef.getTotalCount();
        boolean updated = spaceRepository.lambdaUpdate()
                .eq(Space::getId, spaceId)
                .eq(Space::getTotalSize, totalSize)
                .eq(Space::getTotalCount, totalCount)
                .set(Space::getTotalSize, newTotalSize)
                .set(Space::getTotalCount, newTotalCount)
                .update();
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "额度更新失败");
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR, "用户查询请求为空！");
        // 解包
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 构建查询条件包装实例
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // eq
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // like
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        // orderBy
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), "ascend".equals(sortOrder), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceLevelEnum), ErrorCode.PARAMS_ERROR, "空间级别不存在");
        assert spaceLevelEnum != null;
        space.setMaxSize(spaceLevelEnum.getMaxSize());
        space.setMaxCount(spaceLevelEnum.getMaxCount());
    }

}




