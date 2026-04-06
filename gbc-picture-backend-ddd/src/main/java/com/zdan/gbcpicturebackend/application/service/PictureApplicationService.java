package com.zdan.gbcpicturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.model.CreateOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.zdan.gbcpicturebackend.infrastructure.common.DeleteRequest;
import com.zdan.gbcpicturebackend.interfaces.dto.picture.*;
import com.zdan.gbcpicturebackend.domain.picture.entity.Picture;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.interfaces.vo.picture.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 图片应用服务接口
 *
 * @author LXH
 */
public interface PictureApplicationService {

    /**
     * 上传图片
     *
     * @param inputSource          上传的图片源对象
     * @param pictureUploadRequest 图片上传请求参数
     * @param request              HTTP请求对象
     * @return 返回处理后的图片视图对象
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, HttpServletRequest request);

    /**
     * 获取图片视图对象
     *
     * @param picture 图片实体对象
     * @return 返回对应的图片视图对象
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 分页获取图片视图列表
     *
     * @param picturePage 图片分页实体
     * @return 返回图片视图分页结果
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 构建查询条件包装器
     *
     * @param pictureQueryRequest 图片查询请求参数
     * @return 返回构建好的查询条件包装器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 对图片进行审核
     *
     * @param pictureReviewRequest 图片审核请求参数
     * @param request              HTTP请求对象
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, HttpServletRequest request);

    /**
     * 填充审核参数
     *
     * @param picture   图片实体对象
     * @param loginUser 当前登录用户
     */
    void fillReviewParam(Picture picture, User loginUser);

    /**
     * 批量上传图片
     *
     * @param pictureUploadByBatchRequest 批量上传图片请求参数
     * @param request                     HTTP请求对象
     * @return 返回成功上传的图片数量
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, HttpServletRequest request);

    /**
     * 清理图片文件
     *
     * @param oldPicture 需要清理的旧图片对象
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 删除图片
     *
     * @param deleteRequest 删除请求参数
     * @param request       HTTP请求对象
     */
    void deletePicture(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 编辑图片信息
     *
     * @param picture 图片实体对象
     * @param request HTTP请求对象
     */
    void editPicture(Picture picture, HttpServletRequest request);

    /**
     * 检查图片访问权限
     *
     * @param loginUser 当前登录用户
     * @param picture   图片实体对象
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 根据颜色搜索图片
     *
     * @param searchPictureByColorRequest 按颜色搜索图片请求参数
     * @param request                     HTTP请求对象
     * @return 返回符合条件的图片视图列表
     */
    List<PictureVO> searchByColor(SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request);

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest 批量编辑图片请求参数
     * @param request                   HTTP请求对象
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request);

    /**
     * 创建图片外绘任务
     *
     * @param createPictureOutPaintingTaskRequest 创建图片外绘任务请求参数
     * @param request                             HTTP请求对象
     * @return 返回创建的外绘任务响应对象
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request);

    /**
     * 更新图片信息
     *
     * @param picture 图片实体对象
     * @param request HTTP请求对象
     */
    void updatePicture(Picture picture, HttpServletRequest request);

    /**
     * 根据ID获取图片实体
     *
     * @param id 图片ID
     * @return 返回对应的图片实体对象
     */
    Picture getPictureById(Long id);

    /**
     * 根据ID获取图片视图对象
     *
     * @param id      图片ID
     * @param request HTTP请求对象
     * @return 返回对应的图片视图对象
     */
    PictureVO getPictureVoById(Long id, HttpServletRequest request);

    /**
     * 分页查询图片列表
     *
     * @param pictureQueryRequest 图片查询请求参数
     * @return 返回图片实体分页结果
     */
    Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest);

    /**
     * 分页查询图片视图列表
     *
     * @param pictureQueryRequest 图片查询请求参数
     * @return 返回图片视图分页结果
     */
    Page<PictureVO> listPitureVOByPage(PictureQueryRequest pictureQueryRequest);

    /**
     * 以图搜图功能
     *
     * @param searchPictureByPictureRequest 以图搜图请求参数
     * @return 返回图片搜索结果列表
     */
    List<ImageSearchResult> searchPictureByPicture(SearchPictureByPictureRequest searchPictureByPictureRequest);

    /**
     * 查询并返回指定字段的对象列表
     *
     * @param queryWrapper 查询条件包装器
     * @return 返回查询结果对象列表
     */
    List<Object> selectObjs(QueryWrapper<Picture> queryWrapper);

    /**
     * 查询并返回Map格式的结果列表
     *
     * @param queryWrapper 查询条件包装器
     * @return 返回查询结果Map列表
     */
    List<Map<String, Object>> selectMaps(QueryWrapper<Picture> queryWrapper);
}
