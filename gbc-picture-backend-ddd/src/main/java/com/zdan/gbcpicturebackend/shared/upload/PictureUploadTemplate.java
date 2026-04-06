package com.zdan.gbcpicturebackend.shared.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.zdan.gbcpicturebackend.infrastructure.config.CosClientConfig;
import com.zdan.gbcpicturebackend.infrastructure.api.CosManager;
import com.zdan.gbcpicturebackend.infrastructure.manager.upload.UploadPictureResult;
import com.zdan.gbcpicturebackend.infrastructure.utils.HexColorUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模版
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param inputSource      要传送的图片
     * @param uploadPathPrefix 保存到对象存储的位置的前缀
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // todo 输入源校验
        this.validatePicture(inputSource);
        // todo 获取输入源文件格式
        String fileFormat = this.getFileFormat(inputSource);
        // 设置上传路径（timestamp_uuid.fileFormat）
        String uuid = RandomUtil.randomString(16);
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid, fileFormat);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            // 解析结果并返回
            file = File.createTempFile(uploadPath, null);
            // todo 将输入源导入到本地文件
            this.processFile(inputSource, file);
            // 上传文件
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // todo 获取原始文件的名称
            String originalFileName = this.getOriginalFileName(inputSource);
            // 获取图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) { // 如果图片处理成功，需要获取的图片信息是处理之后的信息
                // 获取压缩图对象
                CIObject compressedCiObject = objectList.get(0);
                // 如果有缩略图，则获取缩略图，否则将压缩图作为缩略图
                CIObject thumbnailCiObject = objectList.size() > 1 ? objectList.get(1) : compressedCiObject;
                return buildResult(originalFileName, uploadPath, compressedCiObject, thumbnailCiObject, imageInfo);
            }
            return buildResult(originalFileName, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new RuntimeException(e);
        } finally {
            // 清除临时文件
            if (file != null && !file.delete()) log.error("临时文件删除失败");
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     *
     * @param inputSource
     */
    protected abstract void validatePicture(Object inputSource);

    /**
     * 获取输入源原始文件名
     *
     * @param inputSource
     * @return
     */
    protected abstract String getFileFormat(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     *
     * @param file
     */
    protected abstract void processFile(Object inputSource, File file);

    /**
     * 获取原始文件名称
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFileName(Object inputSource);

    /**
     * 构建返回结果 --- 图片处理失败时候调用
     *
     * @param originalFileName
     * @param file
     * @param uploadPath
     * @param imageInfo
     * @return
     */
    private UploadPictureResult buildResult(String originalFileName, File file,
                                            String uploadPath, ImageInfo imageInfo) {
        String format = imageInfo.getFormat();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        // 需要手动结算图片宽高比，四舍五入，保留2位
        double picScale = NumberUtil.round(picHeight * 1.0 / picHeight, 2).doubleValue();
        // 设置好返回信息
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(originalFileName);
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        // 设置图片主色调
        String hexAve = HexColorUtils.toStandardHexColor(imageInfo.getAve());
        uploadPictureResult.setPicColor(hexAve);
        return uploadPictureResult;
    }

    /**
     * 构建返回结果 --- 图片处理成功时候调用
     *
     * @param originalFileName   原始文件名
     * @param compressedCiObject 压缩后的对象
     * @param thumbnailCiObject  缩略图对象
     * @return
     */
    private UploadPictureResult buildResult(String originalFileName, String uploadPath,
                                            CIObject compressedCiObject, CIObject thumbnailCiObject,
                                            ImageInfo imageInfo) {
        String format = compressedCiObject.getFormat();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        // 需要手动结算图片宽高比，四舍五入，保留2位
        double picScale = NumberUtil.round(picHeight * 1.0 / picHeight, 2).doubleValue();
        // 设置好返回信息
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(originalFileName);
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        String hexAve = HexColorUtils.toStandardHexColor(imageInfo.getAve());
        uploadPictureResult.setPicColor(hexAve);
        // 设置压缩后的原图地址
        uploadPictureResult.setCompressedUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        // 设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return uploadPictureResult;
    }
}
