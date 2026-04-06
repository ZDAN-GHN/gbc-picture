package com.zdan.gbcpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zdan.gbcpicturebackend.exception.BusinessException;
import com.zdan.gbcpicturebackend.exception.ErrorCode;
import com.zdan.gbcpicturebackend.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.manager.sharding.DynamicShardingManager;
import com.zdan.gbcpicturebackend.model.dto.space.SpaceAddRequest;
import com.zdan.gbcpicturebackend.model.dto.space.SpaceQueryRequest;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.SpaceUser;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.enums.SpaceLevelEnum;
import com.zdan.gbcpicturebackend.model.enums.SpaceRoleEnum;
import com.zdan.gbcpicturebackend.model.enums.SpaceTypeEnum;
import com.zdan.gbcpicturebackend.model.vo.SpaceVO;
import com.zdan.gbcpicturebackend.model.vo.UserVO;
import com.zdan.gbcpicturebackend.service.SpaceService;
import com.zdan.gbcpicturebackend.mapper.SpaceMapper;
import com.zdan.gbcpicturebackend.service.SpaceUserService;
import com.zdan.gbcpicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author LXH
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-01-08 15:46:23
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

    /*
    // 分表对代码的改动很大，这里不再使用了，分表只做学习，不做应用
    @Resource
    @Lazy // 延迟加载（懒加载），避免循环依赖
    private DynamicShardingManager dynamicShardingManager;
    */

    private final Map<Long, Object> spaceLockMap = new ConcurrentHashMap<>();

    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        // 设定默认值
        if (StrUtil.isBlank(space.getSpaceName())) space.setSpaceName("默认空间");
        if (ObjectUtil.isEmpty(space.getSpaceLevel())) space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue()); // 默认是普通级别
        if (ObjectUtil.isEmpty(space.getSpaceType())) space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue()); // 默认是私有空间
        // 填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 校验参数
        this.validSpace(space, true);
        // 权限校验，非管理员只能创建普通空间
        if (space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue()) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 控制同一用户只能创建一个私有空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        Object lock = spaceLockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            Long spaceId;
            try {
                spaceId = transactionTemplate.execute(status -> {
                    boolean existent = this.lambdaQuery()
                            .eq(Space::getUserId, loginUser.getId())
                            .eq(Space::getSpaceType, space.getSpaceType())
                            .exists();
                    // 如果已有空间，就不能创建
                    ThrowUtils.throwIf(existent, ErrorCode.PARAMS_ERROR, "每个用户每类空间只能创建一个");
                    boolean spaceSave = this.save(space);
                    ThrowUtils.throwIf(!spaceSave, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                    // 创建成功后，如果是团队空间，关联新增团队成员记录
                    if (SpaceTypeEnum.TEAM.getValue() == space.getSpaceType()) {
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(space.getUserId());
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        boolean spaceUserSave = spaceUserService.save(spaceUser);
                        ThrowUtils.throwIf(!spaceUserSave, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
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
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
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
        // region --- 添加空间或修改空间的公共校验
        // 校验空间名称是否过长
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
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
        // endregion
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = spaceVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage) {
        // 数据转换
        Page<SpaceVO> spaceVOPage = new Page<>();
        long total = spacePage.getTotal();
        long size = spacePage.getSize();
        long current = spacePage.getCurrent();
        spaceVOPage.setTotal(total);
        spaceVOPage.setSize(size);
        spaceVOPage.setCurrent(current);
        // 获取空间列表
        List<SpaceVO> spaceVOList = spacePage.getRecords().stream()
                .map(this::getSpaceVO)
                .collect(Collectors.toList());
        spaceVOPage.setRecords(spaceVOList);
        // 填充用户信息
        if (CollUtil.isNotEmpty(spaceVOList)) {
            // 获取用户id集合
            Set<Long> userIdSet = spaceVOList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());
            // 将用户id和用户封装实体两者做绑定
            Map<Long, List<UserVO>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                    .map(userService::getUserVO)
                    .collect(Collectors.groupingBy(UserVO::getId));
            // 额外地需要设定用户包装信息
            spaceVOList.forEach(spaceVO -> {
                // 设定好UserVO
                Long userId = spaceVO.getUserId();
                UserVO userVO = userIdUserListMap.get(userId).get(0);
                if (userVO != null) spaceVO.setUser(userVO);
            });
        }
        return spaceVOPage;
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
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceLevelEnum), ErrorCode.PARAMS_ERROR, "空间级别不存在");
        space.setMaxSize(spaceLevelEnum.getMaxSize());
        space.setMaxCount(spaceLevelEnum.getMaxCount());
    }

}




