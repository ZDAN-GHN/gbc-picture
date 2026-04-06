package com.zdan.gbcpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zdan.gbcpicturebackend.api.ailiyunai.AliYunAiApi;
import com.zdan.gbcpicturebackend.api.ailiyunai.model.CreateOutPaintingTaskRequest;
import com.zdan.gbcpicturebackend.api.ailiyunai.model.CreateOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.constant.UserConstant;
import com.zdan.gbcpicturebackend.exception.BusinessException;
import com.zdan.gbcpicturebackend.exception.ErrorCode;
import com.zdan.gbcpicturebackend.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.manager.CosManager;
import com.zdan.gbcpicturebackend.manager.auth.StpKit;
import com.zdan.gbcpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.zdan.gbcpicturebackend.manager.upload.FilePictureUpload;
import com.zdan.gbcpicturebackend.manager.upload.PictureUploadTemplate;
import com.zdan.gbcpicturebackend.manager.upload.UrlPictureUpload;
import com.zdan.gbcpicturebackend.mapper.PictureMapper;
import com.zdan.gbcpicturebackend.model.dto.file.UploadPictureResult;
import com.zdan.gbcpicturebackend.model.dto.picture.*;
import com.zdan.gbcpicturebackend.model.entity.Picture;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zdan.gbcpicturebackend.model.vo.PictureVO;
import com.zdan.gbcpicturebackend.model.vo.UserVO;
import com.zdan.gbcpicturebackend.service.PictureService;
import com.zdan.gbcpicturebackend.service.SpaceService;
import com.zdan.gbcpicturebackend.service.UserService;
import com.zdan.gbcpicturebackend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author LXH
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-11-14 23:21:25
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id不能为空且大于0，有参数则检验
        ThrowUtils.throwIf(ObjectUtil.isNull(id) && id <= 0, ErrorCode.PARAMS_ERROR, "id不能为空且要大于0");
        // 如果传递了url，才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        // 如果传递了introduction，才校验
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource,
                                   PictureUploadRequest pictureUploadRequest,
                                   User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(pictureUploadRequest), ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureUploadRequest.getSpaceId();
        Space space = null;
        if (ObjectUtil.isNotEmpty(spaceId)) {
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验空间条数
            ThrowUtils.throwIf(space.getTotalCount() >= space.getMaxCount(), ErrorCode.PARAMS_ERROR, "空间条数不足");
            // 校验空间大小
            ThrowUtils.throwIf(space.getTotalSize() >= space.getMaxSize(), ErrorCode.PARAMS_ERROR, "空间大小不足");
        }
        // 判断是新增还是更新
        Long pictureId = pictureUploadRequest.getId();
        boolean picExists = false;
        if (pictureId != null) { // 更新
            Picture oldPicture = getById(pictureId);
            ThrowUtils.throwIf(!(picExists = (oldPicture != null)), ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 校验空间是否一致
            // 没传 spaceId 则复用原有图片的 spaceId （这样也兼容了公共图库）
            if (spaceId == null) spaceId = oldPicture.getSpaceId();
            ThrowUtils.throwIf(!Objects.equals(spaceId, oldPicture.getSpaceId()), ErrorCode.PARAMS_ERROR, "空间不一致");
        }
        // 上传到对象存储
        // 设置上传路径前缀（需要根据空间id进行判断后赋值）
        final String UPLOAD_PATH_PREFIX = spaceId == null ?
                String.format("public/%s", loginUser.getId()) : // 公共图库
                String.format("space/%s", spaceId); // 空间
        // 根据参数类型决定是调用文件上传还是url上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        // 文件上传
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, UPLOAD_PATH_PREFIX);
        // 构造要入库的对象 -- picture
        Picture picture = new Picture();
        BeanUtil.copyProperties(uploadPictureResult, picture);
        picture.setSpaceId(spaceId);
        // 设置图片名称（如果提交表单中包含了文件名称优先使用表单的，否则使用上传结果提供的文件名称）
        String picName = uploadPictureResult.getPicName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        // 设置图片主色调
        String picColor = uploadPictureResult.getPicColor();
        picture.setPicColor(picColor);
        picture.setUserId(loginUser.getId());
        // 填充审核参数
        fillReviewParam(picture, loginUser);
        // 如果是更新操作，需要补充图片的id和更新的时间
        if (picExists) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 操作数据库
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 非公共图库才需要执行额度修改
            if (finalSpaceId != null) {
                boolean updated = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return null;
        });
        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = pictureVO.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
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
            Map<Long, List<UserVO>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                    .map(userService::getUserVO)
                    .collect(Collectors.groupingBy(UserVO::getId));
            // 额外地需要设定用户包装信息
            pictureVOList.forEach(pictureVO -> {
                // 设定好UserVO
                Long userId = pictureVO.getUserId();
                UserVO userVO = userIdUserListMap.get(userId).get(0);
                if (userVO != null) pictureVO.setUserVO(userVO);
            });
        }
        return pictureVOPage;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR, "用户查询请求为空！");
        // 解包
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Date reviewTime = pictureQueryRequest.getReviewTime();
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        // 构建查询条件包装实例
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw ->
                    qw.like("name", searchText).or()
                            .like("introduction", searchText)
            );
        }
        // eq
        queryWrapper.eq(ObjectUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjectUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjectUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjectUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        // isNull
        queryWrapper.isNull(nullSpaceId, "spaceId");
        // ge(greater or equal, 即 >=)
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        // lt(less than, 即 <)
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // like
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(ObjectUtil.isNotNull(picFormat), "picFormat", picFormat);
        queryWrapper.like(ObjectUtil.isNotNull(reviewMessage), "reviewMessage", reviewMessage);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", String.format("\"%s\"", tag));
            }
        }
        // orderBy
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User user) {
        // 参数校验
        Long pictureId = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        ThrowUtils.throwIf(pictureId <= 0 || reviewStatusEnum == null, ErrorCode.PARAMS_ERROR);
        // todo ??
        ThrowUtils.throwIf(!UserConstant.ADMIN_ROLE.equals(user.getUserRole()), ErrorCode.NO_AUTH_ERROR, "无权限");
        // 判断图片是否存在（从数据库中获取）
        Picture picture = getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验审核状态是否已经重复（重复不需要操作数据库）
        ThrowUtils.throwIf(picture.getReviewStatus().equals(pictureReviewRequest.getReviewStatus()),
                ErrorCode.OPERATION_ERROR, "重复的审核");
        // 操作数据库
        picture.setReviewStatus(reviewStatus);
        picture.setReviewMessage(reviewMessage);
        boolean isUpdate = updateById(picture);
        ThrowUtils.throwIf(!isUpdate, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParam(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
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
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
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
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
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
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR);
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        ThrowUtils.throwIf(StrUtil.isBlank(pictureUrl), ErrorCode.PARAMS_ERROR);
        // 判断该图片是否被多条记录使用
        long count = lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 不止一条记录使用到这个图片，则不删除图片文件
        if (count > 1) {
            return;
        }
        // 删除对象存储中的图片
        deleteCOSPicture(oldPicture, pictureUrl);
    }

    public void deletePicture(Long pictureId, User loginUser) {
        Picture oldPicture = getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 已经改为使用 sa-token 注解鉴权
        // checkPictureAuth(loginUser, oldPicture);
        // 删除数据库记录
        Long spaceId = oldPicture.getSpaceId();
        transactionTemplate.execute(status -> {
            Space space = spaceService.getById(spaceId);
            boolean deleted = removeById(pictureId);
            ThrowUtils.throwIf(!deleted, ErrorCode.OPERATION_ERROR, "删除失败");
            // 更新空间的使用额度，释放额度
            boolean updated = spaceService.lambdaUpdate()
                    .eq(Space::getId, oldPicture.getSpaceId())
                    .ge(Space::getTotalSize, oldPicture.getPicSize()) // 确保总大小大于等于图片大小
                    .gt(Space::getTotalCount, 0) // 确保总数量大于0
                    .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                    .setSql("totalCount = totalCount - 1")
                    .update();
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "额度更新失败");
            return null;
        });
        // 清理对象存储记录
        clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture, "tags");
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        fillReviewParam(picture, loginUser);
        // 对实体进行校验
        validPicture(picture);
        // 已经改为使用 sa-token 注解鉴权
        // checkPictureAuth(loginUser, picture);
        // 真正修改
        boolean updated = updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "修改失败");
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人和管理员可操作
            if (!Objects.equals(loginUser.getId(), picture.getUserId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
            }
        }
        // 检查图库是否存在
        Space space = spaceService.getById(spaceId);
        if (space == null && !picture.getUserId().equals(loginUser.getId()) || // 公共图库，仅图片持有者有权限
                space != null && !space.getUserId().equals(loginUser.getId())) { // 私有图库，仅空间管理员有权限
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
    }

    @Override
    public List<PictureVO> searchColor(Long spaceId, String searchColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(searchColor), ErrorCode.PARAMS_ERROR);
        // 2. 校验权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        spaceService.checkSpaceAuth(loginUser, space);
        // 3. 获取指定空间下的所有图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
        // 如果结果为空直接返回
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 将颜色字符串转换为Color对象
        Color targetColor = Color.decode(searchColor);
        // 4. 计算颜色相识度，以此对结果进行排序
        List<Picture> sortedPictureList = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
            String hexColor = picture.getPicColor();
            // 没有主色调的颜色默认放到最后
            if (StrUtil.isBlank(hexColor)) {
                return Double.MAX_VALUE;
            }
            // 计算相似度
            Color pictureColor = Color.decode(hexColor);
            // calculateSimilarity 计算的值是值越大颜色越相似，
            // 但 sorted(Comparator.comparingDouble)是升序排序，所以需要对计算值取反
            return -ColorSimilarUtils.calculateSimilarity(pictureColor, targetColor);
        })).collect(Collectors.toList());
        // 5. 返回排序后的结果
        return sortedPictureList.stream()
                .map(PictureVO::objToVo) // service的toVO方法会关联用户信息，但这里不需要
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
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
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        spaceService.checkSpaceAuth(loginUser, space);
        // 获取空间下的图片
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId) // 限制查询字段，只查询 id，减少数据库压力提升性能
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        // 批量更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) picture.setCategory(category);
            if (CollUtil.isNotEmpty(tags)) picture.setTags(JSONUtil.toJsonStr(tags));
        });
        // 批量重命名
        if (StrUtil.isNotBlank(nameRule)) fillPictureWithNameRule(pictureList, nameRule);
        // 批量更新数据库数据
        boolean updated = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "批量编辑失败");
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 校验权限，已经改为使用 sa-token 注解鉴权
        // checkPictureAuth(loginUser, picture);
        // 创建扩图任务
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        // 设置图片信息（目前只有url）
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setInput(input);
        // 设置扩图参数
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskRequest.getParameters());
        // 通过 api 创建扩图任务
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
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

    private void deleteCOSPicture(Picture oldPicture, String pictureUrl) {
        // 删除原图片
        cosManager.deleteObject(pictureUrl);
        // 删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) cosManager.deleteObject(thumbnailUrl);
        // 删除压缩图
        String compressedUrl = oldPicture.getCompressedUrl();
        if (StrUtil.isNotBlank(compressedUrl)) cosManager.deleteObject(compressedUrl);
    }
}




