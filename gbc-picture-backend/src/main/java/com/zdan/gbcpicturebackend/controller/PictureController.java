package com.zdan.gbcpicturebackend.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zdan.gbcpicturebackend.annotation.AuthCheck;
import com.zdan.gbcpicturebackend.api.ailiyunai.AliYunAiApi;
import com.zdan.gbcpicturebackend.api.ailiyunai.model.CreateOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.api.ailiyunai.model.GetOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.zdan.gbcpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.zdan.gbcpicturebackend.common.BaseResponse;
import com.zdan.gbcpicturebackend.common.DeleteRequest;
import com.zdan.gbcpicturebackend.common.ResultUtils;
import com.zdan.gbcpicturebackend.constant.UserConstant;
import com.zdan.gbcpicturebackend.exception.BusinessException;
import com.zdan.gbcpicturebackend.exception.ErrorCode;
import com.zdan.gbcpicturebackend.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.manager.auth.StpKit;
import com.zdan.gbcpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.zdan.gbcpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.zdan.gbcpicturebackend.manager.auth.strategy.authloader.spaceuser.SpaceUserAuthManager;
import com.zdan.gbcpicturebackend.model.dto.picture.*;
import com.zdan.gbcpicturebackend.model.entity.Picture;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.enums.PictureReviewStatusEnum;
import com.zdan.gbcpicturebackend.model.vo.PictureTagCategory;
import com.zdan.gbcpicturebackend.model.vo.PictureVO;
import com.zdan.gbcpicturebackend.service.PictureService;
import com.zdan.gbcpicturebackend.service.SpaceService;
import com.zdan.gbcpicturebackend.service.UserService;
import com.zdan.gbcpicturebackend.utils.ThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 本地缓存 caffeine
    private final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024)
            .maximumSize(10_000L)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Resource
    private AliYunAiApi aliYunAiApi;
    @Autowired
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 通过用户文件上传图片 --- admin
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                 PictureUploadRequest pictureUploadRequest,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先完成登录");
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 通过 url 上传图片 --- admin
     *
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest,
                                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "请先完成登录");
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(deleteRequest == null, ErrorCode.PARAMS_ERROR, "删除请求为空！");
        // 获取图片对象
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true, "删除成功");
    }

    /**
     * 更新图片 --- admin
     *
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                               HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pictureUpdateRequest == null
                || pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 将请求转换为持久层实体
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture, "tags");
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        picture.setUpdateTime(new Date());
        // 对实体进行校验
        pictureService.validPicture(picture);
        Picture oldPicture = pictureService.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "图片不存在");
        // 真正修改
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParam(picture, loginUser);
        boolean updated = pictureService.updateById(picture);
        ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "修改失败");
        return ResultUtils.success(updated, "修改成功");
    }

    /**
     * 根据id获取实体 --- admin
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id,
                                                HttpServletRequest request) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "id不能为空");
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取封装实体
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id,
                                                    HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        // 只允许查看已审核的图片
        ThrowUtils.throwIf(PictureReviewStatusEnum.PASS.getValue() != picture.getReviewStatus(), ErrorCode.OPERATION_ERROR, "图片未审核");
        // 图库校验
        Long spaceId = picture.getSpaceId();
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.getPictureVO(picture);
        Space space = null;
        List<String> permissionList = null;
        // 如果图片有空间 id 需要获取空间信息供后需要获取权限列表
        if (spaceId != null) space = spaceService.getById(spaceId);
        // 根据 space, picture, user 三个对象获取权限列表（将图片放入线程中，鉴权使用）
        ThreadLocalUtils.set(picture);
        permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 如果一个权限没有，说明请求异常
        if (CollUtil.isEmpty(permissionList)) {
            log.error("非法请求！请求为{}", request);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页查询图片实体列表 --- admin
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = pictureQueryRequest.getId();
        // 参数校验 + 限制爬虫
        if (pictureId != null && pictureId <= 0 || pictureQueryRequest.getPageSize() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // 设定好查询条件
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        Page<Picture> page = new Page<>(current, pageSize);
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        // 分页查询
        Page<Picture> picturePage = pictureService.page(page, queryWrapper);
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页查询图片视图实体
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) { // 查看公共图库
            // 普通用户只能看到已经审核好的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else { // 私有空间 or 团队空间
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            // 已改为使用 sa-token 编程式鉴权
            // User loginUser = userService.getLoginUser(request);
            // Picture picture = new Picture();
            // BeanUtils.copyProperties(pictureQueryRequest, picture);
            // pictureService.checkPictureAuth(loginUser, picture);
        }
        Page<Picture> picturePage = listPictureByPage(pictureQueryRequest).getData();
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 分页查询封装实体（带缓存）
     *
     * @param pictureQueryRequest
     * @return
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest) {
        ThrowUtils.throwIf(pictureQueryRequest == null, ErrorCode.PARAMS_ERROR);
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // todo 查询缓存，缓存没有再查询数据库
        //      TIP: 这里缓存使用颗粒度比较大的缓存分页对象，后续可以优化
        // 构建 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes()); // 对json进行压缩
        String cacheKey = String.format("gbcpicture:listPictureVOByPageWithCache:%s", hashKey);
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        // 先从本地缓存获取
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 如果本地缓存命中，返回本地缓存
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachePage);
        }
        // 本地缓存未命中则到 redis 缓存中去找
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) { // 缓存命中
            // 如果 redis 缓存命中，更新本地缓存，返回缓存
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachePage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachePage);
        }
        // 查询数据库
        Page<Picture> picturePage = listPictureByPage(pictureQueryRequest).getData();
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage);
        // 更新本地缓存
        cachedValue = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(cacheKey, cachedValue);
        // 更新 redis 缓存（设定随机过期时间避免缓存雪崩，同时 pictureVOPage 必定不为空，也防止了缓存击穿）
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300); // 3 ~ 6 分钟
        opsForValue.set(cacheKey, cachedValue, cacheExpireTime, TimeUnit.SECONDS);
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                             HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 将请求转换为持久层实体
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true, "修改成功");
    }

    /**
     * 获取标签和分类的菜单项
     *
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模版", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片 --- 需要图片审核权限
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_PERMIT)
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取图片并上传 --- admin
     *
     * @param pictureUploadByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userService.getLoginUser(request);
        Integer uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图
     */
    @Deprecated // 由于百度识图防爬虫加强，老 api 已失效，该接口随之失效
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(picture.getUrl());
        return ResultUtils.success(resultList);
    }

    /**
     * 按照颜色搜图
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        String searchColor = searchPictureByColorRequest.getPicColor();
        ThrowUtils.throwIf(StrUtil.isBlank(searchColor), ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> resultList = pictureService.searchColor(spaceId, searchColor, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
                                                    HttpServletRequest request) {

        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true, "批量编辑成功");
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
                                                                                    HttpServletRequest request) {
        // 参数校验
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse createOutPaintingTaskResponse = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(createOutPaintingTaskResponse);
    }

    /**
     * 查询 AI 扩图任务执行结果
     */
    @GetMapping("/out_painting/get_response")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTaskResponse(String taskId, HttpServletRequest request) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse outPaintingTask = aliYunAiApi.getOutPaintingTask(taskId);
        if (outPaintingTask == null || outPaintingTask.getOutput().getCode() != null) {
            new BusinessException(ErrorCode.PARAMS_ERROR, "查询失败");
        }
        return ResultUtils.success(outPaintingTask);
    }
}