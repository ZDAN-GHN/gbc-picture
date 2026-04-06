package com.zdan.gbcpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zdan.gbcpicturebackend.api.ailiyunai.model.CreateOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.model.dto.picture.*;
import com.zdan.gbcpicturebackend.model.entity.Picture;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.vo.PictureVO;

import java.util.List;

/**
 * @author LXH
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-11-14 23:21:25
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片信息
     *
     * @param picture 图片实体
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     *
     * @param inputSource          要上传的图片
     * @param pictureUploadRequest 上传请求
     * @param loginUser            登录用户
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取图片包装类实体（单条）
     *
     * @param picture 持久层实体
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片包装类实体（单条）
     *
     * @param picturePage 持久层分页实体
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 获取查询条件包装实体
     *
     * @param pictureQueryRequest 查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 审核图片
     *
     * @param pictureReviewRequest
     * @param user
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User user);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param loginUser
     */
    void fillReviewParam(Picture picture, User loginUser);


    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量上传图片请求
     * @param loginUser                   登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);


    /**
     * 删除图片文件
     *
     * @param oldPicture 旧图片
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(Long pictureId, User loginUser);


    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 校验空间图片的权限
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 颜色搜图
     *
     * @param spaceId
     * @param searchColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchColor(Long spaceId, String searchColor, User loginUser);

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 批量编辑图片请求
     * @param loginUser                 当前登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
