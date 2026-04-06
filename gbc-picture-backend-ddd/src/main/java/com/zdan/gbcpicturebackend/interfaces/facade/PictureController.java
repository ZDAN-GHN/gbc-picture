package com.zdan.gbcpicturebackend.interfaces.facade;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zdan.gbcpicturebackend.infrastructure.annotation.AuthCheck;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.AliYunAiApi;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.model.CreateOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.model.GetOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.zdan.gbcpicturebackend.infrastructure.common.BaseResponse;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.infrastructure.common.ResultUtils;
import com.zdan.gbcpicturebackend.interfaces.assembler.PictureAssembler;
import com.zdan.gbcpicturebackend.interfaces.dto.picture.*;
import com.zdan.gbcpicturebackend.domain.user.constant.UserConstant;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.shared.auth.annotation.SaSpaceCheckPermission;
import com.zdan.gbcpicturebackend.shared.auth.model.SpaceUserPermissionConstant;
import com.zdan.gbcpicturebackend.shared.auth.strategy.authloader.spaceuser.SpaceUserAuthManager;
import com.zdan.gbcpicturebackend.domain.picture.entity.Picture;
import com.zdan.gbcpicturebackend.domain.picture.valueobjectt.PictureReviewStatusEnum;
import com.zdan.gbcpicturebackend.interfaces.vo.picture.PictureTagCategory;
import com.zdan.gbcpicturebackend.interfaces.vo.picture.PictureVO;
import com.zdan.gbcpicturebackend.application.service.PictureApplicationService;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private PictureApplicationService pictureApplicationService;

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
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(multipartFile, pictureUploadRequest), ErrorCode.PARAMS_ERROR);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile, pictureUploadRequest, request);
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
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, pictureUploadRequest, request);
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
        pictureApplicationService.deletePicture(deleteRequest, request);
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
    public BaseResponse<?> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                         HttpServletRequest request) {
        // 参数校验
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将请求转换为持久层实体
        Picture picture = PictureAssembler.toPictureEntity(pictureUpdateRequest);
        pictureApplicationService.updatePicture(picture, request);
        return ResultUtils.success("修改成功");
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
        return ResultUtils.success(pictureApplicationService.getPictureById(id));
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
        return ResultUtils.success(pictureApplicationService.getPictureVoById(id, request));
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
        return ResultUtils.success(pictureApplicationService.listPictureByPage(pictureQueryRequest));
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
        return ResultUtils.success(pictureApplicationService.listPitureVOByPage(pictureQueryRequest));
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
        Page<PictureVO> pictureVOPage = pictureApplicationService.getPictureVOPage(picturePage);
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
    public BaseResponse<?> editPicture(@RequestBody PictureEditRequest pictureEditRequest,
                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = PictureAssembler.toPictureEntity(pictureEditRequest);
        pictureApplicationService.editPicture(picture, request);
        return ResultUtils.success("修改成功");
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
    public BaseResponse<?> doPictureReview(PictureReviewRequest pictureReviewRequest,
                                           HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        pictureApplicationService.doPictureReview(pictureReviewRequest, request);
        return ResultUtils.success("审核成功");
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
        Integer uploadCount = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, request);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 以图搜图
     */
    @Deprecated // 由于百度识图防爬虫加强，老 api 已失效，该接口随之失效
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(pictureApplicationService.searchPictureByPicture(searchPictureByPictureRequest));
    }

    /**
     * 按照颜色搜图
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest,
                                                              HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(pictureApplicationService.searchByColor(searchPictureByColorRequest, request));
    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
                                                    HttpServletRequest request) {

        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        pictureApplicationService.editPictureByBatch(pictureEditByBatchRequest, request);
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
        CreateOutPaintingTaskResponse createOutPaintingTaskResponse = pictureApplicationService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, request);
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