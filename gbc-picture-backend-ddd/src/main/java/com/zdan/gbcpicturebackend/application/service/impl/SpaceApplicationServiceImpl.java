package com.zdan.gbcpicturebackend.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.application.service.SpaceApplicationService;
import com.zdan.gbcpicturebackend.domain.space.service.SpaceDomainService;
import com.zdan.gbcpicturebackend.domain.user.service.UserDomainService;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.interfaces.dto.space.SpaceQueryRequest;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.domain.space.valueobjectt.SpaceLevelEnum;
import com.zdan.gbcpicturebackend.interfaces.vo.space.SpaceVO;
import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;
import com.zdan.gbcpicturebackend.shared.auth.strategy.authloader.spaceuser.SpaceUserAuthManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 空间应用服务实现
 * @author LXH
 */
@Service
@Slf4j
public class SpaceApplicationServiceImpl implements SpaceApplicationService {

    @Resource
    private SpaceDomainService spaceDomainService;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /*
    // 分表对代码的改动很大，这里不再使用了，分表只做学习，不做应用
    @Resource
    @Lazy // 延迟加载（懒加载），避免循环依赖
    private DynamicShardingManager dynamicShardingManager;
    */

    private final Map<Long, Object> spaceLockMap = new ConcurrentHashMap<>();

    @Override
    public void validSpace(Space space, boolean add) {
        if (space == null) {
            log.error("错误的传值, space == null");
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        spaceDomainService.validSpace(space, add);
    }

    @Override
    public Long addSpace(Space space, HttpServletRequest request) {
        // 校验参数
        spaceDomainService.validSpace(space, true);
        User loginUser = userDomainService.getLoginUser(request);
        // 权限校验，非管理员只能创建普通空间
        if (space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue()) {
            ThrowUtils.throwIf(loginUser.isNotAdmin(), ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        return spaceDomainService.addSpace(space, loginUser);
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        if (ObjectUtil.hasEmpty(loginUser, space)) {
            log.error("错误的传值, loginUser: {}, Space: {}", loginUser, space);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        spaceDomainService.checkSpaceAuth(loginUser, space);
    }

    @Override
    public void deleteSpaceById(DeleteRequest deleteRequest, HttpServletRequest request) {
        User loginUser = userDomainService.getLoginUser(request);
        Long spaceId = deleteRequest.getId();
        // 获取空间对象
        spaceDomainService.deleteSpaceById(spaceId, loginUser);
    }

    @Override
    public void updateSpace(Space space, HttpServletRequest request) {
        User loginUser = userDomainService.getLoginUser(request);
        spaceDomainService.updateSpace(space, loginUser);

    }

    @Override
    public Space getSpaceById(Long id) {
        ThrowUtils.throwIf(ObjectUtil.isEmpty(id) || id <= 0, ErrorCode.PARAMS_ERROR);
        return spaceDomainService.getSpaceById(id);
    }

    @Override
    public SpaceVO getSpaceVOById(Long id, HttpServletRequest request) {
        // 查询数据库
        Space space = this.getSpaceById(id);
        SpaceVO spaceVO = this.getSpaceVO(space);
        User loginUser = userDomainService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        return spaceVO;
    }

    @Override
    public Page<Space> listSpaceByPage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        Long spaceId = spaceQueryRequest.getId();
        if ((spaceId != null && spaceId <= 0) || spaceQueryRequest.getPageSize() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
return spaceDomainService.listSpaceByPage(spaceQueryRequest);
    }

    @Override
    public void editSpace(Space space, HttpServletRequest request) {
        User loginUser = userDomainService.getLoginUser(request);
        spaceDomainService.editSpace(space, loginUser);
    }

    @Override
    public void updateSpaceUsage(Space spaceRef, Long newTotalSize, Long newTotalCount) {
        if (ObjectUtil.hasEmpty(spaceRef, newTotalSize, newTotalCount)) {
            log.error("错误的传值, spaceRef: {}, newTotalSize: {}, newTotalCount: {}", spaceRef, newTotalSize, newTotalCount);
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        spaceDomainService.updateSpaceUsage(spaceRef, newTotalSize, newTotalCount);
    }

    @Override
    public List<Space> listByIds(Set<Long> spaceIdSet) {
        ThrowUtils.throwIf(CollUtil.isEmpty(spaceIdSet), ErrorCode.PARAMS_ERROR);
        return spaceDomainService.listByIds(spaceIdSet);
    }

    @Override
    public SpaceVO getSpaceVO(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        Long userId = spaceVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userDomainService.getById(userId);
            UserVO userVO = userDomainService.getUserVO(user);
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
            Map<Long, List<UserVO>> userIdUserListMap = userDomainService.listByIds(userIdSet).stream()
                    .map(userDomainService::getUserVO)
                    .collect(Collectors.groupingBy(UserVO::getId));
            // 额外地需要设定用户包装信息
            spaceVOList.forEach(spaceVO -> {
                // 设定好UserVO
                Long userId = spaceVO.getUserId();
                UserVO userVO = userIdUserListMap.get(userId).get(0);
                if (userVO != null) {
                    spaceVO.setUser(userVO);
                }
            });
        }
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        if (spaceQueryRequest == null) {
            log.error("错误的传值, spaceQueryRequest == null");
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        if (space == null) {
            log.error("错误的传值, space == null");
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }
}




