package com.zdan.gbcpicturebackend.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zdan.gbcpicturebackend.domain.picture.entity.Picture;
import com.zdan.gbcpicturebackend.domain.space.entity.Space;
import com.zdan.gbcpicturebackend.domain.user.entity.User;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.model.CreateOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.interfaces.dto.picture.*;
import com.zdan.gbcpicturebackend.interfaces.vo.picture.PictureVO;

import java.util.List;
import java.util.Map;

/**
 * 图片领域服务接口
 * 定义了与图片相关的业务操作方法，包括图片的获取、审核、编辑、删除等功能
 *
 * @author LXH
 */
public interface PictureDomainService {

    /**
     * 获取图片包装类实体（单条）
     * 将持久层实体对象转换为视图层展示对象
     *
     * @param picture 持久层实体，包含图片的基本信息
     * @return PictureVO 视图层展示对象，用于前端展示
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片包装类实体（单条）
     * 将持久层分页实体对象转换为视图层分页展示对象
     *
     * @param picturePage 持久层分页实体，包含分页信息和图片数据
     * @return Page<PictureVO> 视图层分页展示对象，用于前端分页展示
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 获取查询条件包装实体
     * 将查询请求对象转换为MyBatis-Plus的查询条件对象
     *
     * @param pictureQueryRequest 查询条件请求对象，包含查询参数
     * @return QueryWrapper<Picture> MyBatis-Plus查询条件对象，用于数据库查询
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 审核图片
     * 对图片进行审核操作，可能包括通过、拒绝等操作
     *
     * @param pictureReviewRequest 图片审核请求对象，包含审核相关信息
     * @param user                 当前登录用户，用于记录审核人信息
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User user);

    /**
     * 填充审核参数
     * 为图片对象填充审核相关的参数信息
     *
     * @param picture   图片实体对象，需要填充审核参数
     * @param loginUser 当前登录用户，可能是审核人
     */
    void fillReviewParam(Picture picture, User loginUser);

    /**
     * 删除图片文件
     * 从存储系统中删除图片的实际文件
     *
     * @param oldPicture 旧图片对象，包含文件路径等信息
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 编辑图片
     * 修改图片的基本信息，如名称、描述等
     *
     * @param loginUser 当前登录用户，用于记录操作人
     */
    void editPicture(Picture picture, User loginUser);

    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 批量编辑图片
     *
     * @param pictureIdList
     * @param spaceId
     * @param category
     * @param tags
     * @param nameRule
     */
    void editPictureByBatch(List<Long> pictureIdList, Long spaceId, String category, List<String> tags, String nameRule);

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    void updatePicture(Picture picture, User loginUser);

    Picture getPictureById(Long id);

    Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest);

    Page<PictureVO> listPictureVOByPage(PictureQueryRequest pictureQueryRequest);

    List<PictureVO> searchByColor(String searchColor, Long spaceId, User loginUser);

    boolean saveOrUpdate(Picture picture);

    boolean removePictureById(Long pictureId);

    List<Object> selectObjs(QueryWrapper<Picture> queryWrapper);

    List<Map<String, Object>> selectMaps(QueryWrapper<Picture> queryWrapper);

    void uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser, Space spaceRef, Picture pictureRef);
}
