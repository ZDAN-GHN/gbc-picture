package com.zdan.gbcpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zdan.gbcpicturebackend.config.CosClientConfig;
import com.zdan.gbcpicturebackend.exception.ErrorCode;
import com.zdan.gbcpicturebackend.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件通用服务（基于CosManager再次封装，做业务操作上的修饰）
 * 初始阶段使用，现废弃，改用 upload 包下的模版方法优化
 */
@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    要传送的图片
     * @param uploadPathPrefix 保存到对象存储的位置的前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 图片校验
        validatePicture(multipartFile);
        // 设置上传路径
        String uuid = RandomUtil.randomString(16);
        String originalFileName = multipartFile.getOriginalFilename();
        String uploadFileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()),
                uuid, FileUtil.getSuffix(originalFileName));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);
        File file = null;
        try {
            // 解析结果并返回
            file = File.createTempFile(uploadPath, null);
            // 将上传的文件数据导出到临时文件
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            return buildResult(multipartFile, imageInfo, uploadPath, originalFileName);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new RuntimeException(e);
        } finally {
            // 清除临时文件
            if (file != null && !file.delete()) log.error("临时文件删除失败");
        }
    }


    /**
     * 文件校验
     *
     * @param multipartFile 表单传输过来的文件
     */
    public void validatePicture(MultipartFile multipartFile) {
        // 非空
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "上传文件不能为空");
        // 大小不得超过2M
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(multipartFile.getSize() > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "上传文件大小不能超过2M");
        // 后缀要求
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "不支持的图片类型");
    }

    /**
     * 构建上传结果
     *
     * @param multipartFile
     * @param imageInfo
     * @param uploadPath
     * @param originalFileName
     * @return
     */
    private UploadPictureResult buildResult(MultipartFile multipartFile, ImageInfo imageInfo, String uploadPath, String originalFileName) {
        String format = imageInfo.getFormat();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        // 需要手动结算图片宽高比，四舍五入，保留2位
        double picScale = NumberUtil.round(picHeight * 1.0 / picHeight, 2).doubleValue();
        // 设置好返回信息
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(originalFileName);
        uploadPictureResult.setPicSize(multipartFile.getSize());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(format);
        return uploadPictureResult;
    }
}
