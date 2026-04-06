package com.zdan.gbcpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.zdan.gbcpicturebackend.exception.ErrorCode;
import com.zdan.gbcpicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 通过文件上传图片
 */
@Service
@Slf4j
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void validatePicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
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

    @Override
    protected String getFileFormat(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return FileUtil.getSuffix(multipartFile.getOriginalFilename());
    }

    @Override
    protected void processFile(Object inputSource, File file) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        // 将上传的文件数据导出到临时文件
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            log.error("[FilePictureUpload] 图片上传到对象存储失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getOriginalFileName(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }
}
