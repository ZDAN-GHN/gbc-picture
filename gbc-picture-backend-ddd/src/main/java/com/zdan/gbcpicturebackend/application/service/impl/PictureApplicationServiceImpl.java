package com.zdan.gbcpicturebackend.application.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.application.service.PictureApplicationService;
import com.zdan.gbcpicturebackend.domain.picture.service.PictureDomainService;
import com.zdan.gbcpicturebackend.domain.space.service.SpaceDomainService;
import com.zdan.gbcpicturebackend.domain.user.service.UserDomainService;
import com.zdan.gbcpicturebackend.infrastructure.api.imagesearch.ImageSearchApiFacade;
import com.zdan.gbcpicturebackend.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.infrastructure.utils.ThreadLocalUtils;
import com.zdan.gbcpicturebackend.shared.auth.strategy.authloader.spaceuser.SpaceUserAuthManager;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.model.CreateOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.interfaces.dto.picture.*;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.shared.auth.StpKit;
import com.zdan.gbcpicturebackend.shared.auth.model.SpaceUserPermissionConstant;
import com.zdan.gbcpicturebackend.domain.picture.entity.Picture;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.domain.picture.valueobjectt.PictureReviewStatusEnum;
import com.zdan.gbcpicturebackend.interfaces.vo.picture.PictureVO;
import com.zdan.gbcpicturebackend.interfaces.vo.user.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片应用服务实现
 */
@Service
@Slf4j
public class PictureApplicationServiceImpl
        implements PictureApplicationService {

    // region --- 领域服务
    @Resource
    private PictureDomainService pictureDomainService;

    @Resource
    private UserDomainService userDomainService;

    @Resource
    private SpaceDomainService spaceDomainService;

    // endregion 领域服务

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    // todo 重构
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
        User loginUser = userDomainService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Space spaceRef = new Space();
        Picture pictureRef = new Picture();
        pictureDomainService.uploadPicture(inputSource, pictureUploadRequest, loginUser, spaceRef, pictureRef);
        final Long finalSpaceId = spaceRef.getId();
        transactionTemplate.execute(status -> {
            boolean result = pictureDomainService.saveOrUpdate(pictureRef);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 非公共图库才需要执行额度修改
            if (finalSpaceId != null) {
                Long newTotalSize = spaceRef.getTotalSize() + pictureRef.getPicSize();
                Long newTotalCount = spaceRef.getTotalCount() + 1;
                spaceDomainService.updateSpaceUsage(spaceRef, newTotalSize, newTotalCount);
            }
            return null;
        });
        return PictureVO.objToVo(pictureRef);
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = pictureVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userDomainService.getById(userId);
            UserVO userVO = userDomainService.getUserVO(user);
            pictureVO.setUserVO(userVO);
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        // 数据转换
        Page<PictureVO> pictureVOPage = new Page<>();
        long total = picturePage.getTotal();
        long size = picturePage.getSize();
        long current = picturePage.getCurrent();
        pictureVOPage.setTotal(total);
        pictureVOPage.setSize(size);
        pictureVOPage.setCurrent(current);
        List<PictureVO> pictureVOList = picturePage.getRecords().stream()
                .map(this::getPictureVO).collect(Collectors.toList());
        pictureVOPage.setRecords(pictureVOList);
        if (CollUtil.isNotEmpty(pictureVOList)) {
            // 获取用户id集合
            Set<Long> userIdSet = pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
            // 将用户id和用户封装实体两者做绑定
            Map<Long, List<UserVO>> userIdUserListMap = userDomainService.listByIds(userIdSet).stream()
                    .map(userDomainService::getUserVO)
                    .collect(Collectors.groupingBy(UserVO::getId));
            // 额外地需要设定用户包装信息
            pictureVOList.forEach(pictureVO -> {
                // 设定好UserVO
                Long userId = pictureVO.getUserId();
                UserVO userVO = userIdUserListMap.get(userId).get(0);
                if (userVO != null) {
                    pictureVO.setUserVO(userVO);
                }
            });
        }
        return pictureVOPage;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.getQueryWrapper(pictureQueryRequest);
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        User optUser = userDomainService.getLoginUser(request);
        pictureDomainService.doPictureReview(pictureReviewRequest, optUser);
    }

    @Override
    public void fillReviewParam(Picture picture, User loginUser) {
        if (loginUser.isAdmin()) {
            // 系统管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非系统管理员需要鉴权
            if (
                    StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_PERMIT)
            ) { // 空间内的审核人员才能自动过审
                picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            } else {
                picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            }
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 抓取页面
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析数据
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, request);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Async // 异步执行
    @Override
    public void clearPictureFile(Picture oldPicture) {
        pictureDomainService.clearPictureFile(oldPicture);
    }

    @Override
    public void deletePicture(DeleteRequest deleteRequest, HttpServletRequest request) {
        Long pictureId = deleteRequest.getId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureDomainService.getPictureById(pictureId);
        // 删除数据库记录
        Long spaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            Space space = spaceDomainService.getSpaceById(spaceId);
            boolean deleted = pictureDomainService.removePictureById(pictureId);
            ThrowUtils.throwIf(!deleted, ErrorCode.OPERATION_ERROR, "删除失败");
            // 更新空间的使用额度，释放额度
            Long newTotalSize = space.getTotalSize() - oldPicture.getPicSize();
            Long newTotalCount = space.getTotalCount() - 1;
            if (newTotalSize >= 0 && newTotalCount >= 0) {
                spaceDomainService.updateSpaceUsage(space, newTotalSize, newTotalCount);
            }
            return null;
        });
        // 清理对象存储记录
        clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(Picture picture, HttpServletRequest request) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userDomainService.getLoginUser(request);
        pictureDomainService.editPicture(picture, loginUser);
    }

    // todo 重构
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人和管理员可操作
            if (!Objects.equals(loginUser.getId(), picture.getUserId()) && !loginUser.isAdmin()) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
            }
        }
        // 检查图库是否存在
        Space space = spaceDomainService.getSpaceById(spaceId);
        if (space == null && !picture.getUserId().equals(loginUser.getId()) || // 公共图库，仅图片持有者有权限
                space != null && !space.getUserId().equals(loginUser.getId())) { // 私有图库，仅空间管理员有权限
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
    }

    @Override
    public List<PictureVO> searchByColor(SearchPictureByColorRequest
                                                 searchPictureByColorRequest, HttpServletRequest request) {
        String searchColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userDomainService.getLoginUser(request);
        // 1. 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(searchColor), ErrorCode.PARAMS_ERROR);
        // 2. 校验权限
        Space space = spaceDomainService.getSpaceById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        spaceDomainService.checkSpaceAuth(loginUser, space);
        return pictureDomainService.searchByColor(searchColor, spaceId, loginUser);
    }

    // todo 重构
    @Override
    @Transactional
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        User loginUser = userDomainService.getLoginUser(request);
        // 获取参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        // 参数校验
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        // 校验权限
        ThrowUtils.throwIf(ObjectUtil.isEmpty(spaceId), ErrorCode.PARAMS_ERROR);
        Space space = spaceDomainService.getSpaceById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        spaceDomainService.checkSpaceAuth(loginUser, space);
        // 批量编辑
        pictureDomainService.editPictureByBatch(pictureIdList, spaceId, category, tags, nameRule);
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest
                                                                              createPictureOutPaintingTaskRequest, HttpServletRequest request) {
        User loginUser = userDomainService.getLoginUser(request);
        return pictureDomainService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    }

    @Override
    public void updatePicture(Picture picture, HttpServletRequest request) {
        // 对实体进行校验
        picture.validate();
        User loginUser = userDomainService.getLoginUser(request);
        pictureDomainService.updatePicture(picture, loginUser);
    }

    @Override
    public Picture getPictureById(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "id不能为空");
        return pictureDomainService.getPictureById(id);
    }

    @Override
    public PictureVO getPictureVoById(Long id,
                                      HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureDomainService.getPictureById(id);
        // 只允许查看已审核的图片
        ThrowUtils.throwIf(PictureReviewStatusEnum.PASS.getValue() != picture.getReviewStatus(), ErrorCode.OPERATION_ERROR, "图片未审核");
        // 图库校验
        Long spaceId = picture.getSpaceId();
        // 获取权限列表
        User loginUser = userDomainService.getLoginUser(request);
        PictureVO pictureVO = pictureDomainService.getPictureVO(picture);
        Space space = null;
        List<String> permissionList = null;
        // 如果图片有空间 id 需要获取空间信息供后需要获取权限列表
        if (spaceId != null) {
            space = spaceDomainService.getSpaceById(spaceId);
        }
        // 根据 space, picture, user 三个对象获取权限列表（将图片放入线程中，鉴权使用）
        ThreadLocalUtils.set(picture);
        permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 如果一个权限没有，说明请求异常
        if (CollUtil.isEmpty(permissionList)) {
            log.error("非法请求！请求为{}", request);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
        pictureVO.setPermissionList(permissionList);
        return pictureVO;
    }

    @Override
    public Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest) {
        Long pictureId = pictureQueryRequest.getId();
        // 参数校验 + 限制爬虫
        if (pictureId != null && pictureId <= 0 || pictureQueryRequest.getPageSize() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        return pictureDomainService.listPictureByPage(pictureQueryRequest);
    }

    @Override
    public Page<PictureVO> listPitureVOByPage(PictureQueryRequest pictureQueryRequest) {
        return pictureDomainService.listPictureVOByPage(pictureQueryRequest);
    }

    @Override
    public List<ImageSearchResult> searchPictureByPicture(SearchPictureByPictureRequest
                                                                  searchPictureByPictureRequest) {
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = this.getPictureById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ImageSearchApiFacade.searchImage(picture.getUrl());
    }

    @Override
    public List<Object> selectObjs(QueryWrapper<Picture> queryWrapper) {
        return pictureDomainService.selectObjs(queryWrapper);
    }

    @Override
    public List<Map<String, Object>> selectMaps(QueryWrapper<Picture> queryWrapper) {
        return pictureDomainService.selectMaps(queryWrapper);
    }

    /**
     * 根据命名规则批量重命名图片
     *
     * @param pictureList 图片列表
     * @param nameRule    格式：图片名{序号}
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        try {
            for (int i = 0; i < pictureList.size(); i++) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(i + 1));
                pictureList.get(i).setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称规则解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }
}




