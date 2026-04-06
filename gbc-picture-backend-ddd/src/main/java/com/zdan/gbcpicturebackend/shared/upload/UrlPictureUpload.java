package com.zdan.gbcpicturebackend.shared.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 通过 url 上传图片
 */
@Service
@Slf4j
public class UrlPictureUpload extends PictureUploadTemplate {

    private String originalFileName;

    private String fileFormat;

    @Override
    protected void validatePicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 非空校验
        ThrowUtils.throwIf(fileUrl == null, ErrorCode.PARAMS_ERROR, "文件地址为空");
        // 校验url格式
        ThrowUtils.throwIf(!HttpUtil.isHttp(fileUrl) && !HttpUtil.isHttps(fileUrl),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 和 HTTPS 协议的文件地址"
        );
        // 文件格式校验 （通过发送 head 请求获取文件信息，如果发现是不支持的，可以提前阻止缓存到本地，减少网络开销）
        // 发送 head 请求
        try (HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute()) {
            // 请求失败有可能是因为url不支持 header 请求，不应该抛异常，而应该终止方法
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) return;
            // 文件格式校验
            String contentType = httpResponse.header("Content-Type");
            ThrowUtils.throwIf(StrUtil.isBlank(contentType), ErrorCode.PARAMS_ERROR, "文件格式缺失");
            // 允许的图片类型
            final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
            ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                    ErrorCode.PARAMS_ERROR, "文件类型错误");
            // 记录文件格式，做兜底方案
            this.fileFormat = contentType.toLowerCase().split("/")[1];
            // 文件大小校验
            String contentLengthStr = httpResponse.header("Content-Length");
            // 文件长度获取为空
            ThrowUtils.throwIf(StrUtil.isBlank(contentLengthStr), ErrorCode.PARAMS_ERROR, "不能上传空文件");
            try {
                long contentLength = Long.parseLong(contentLengthStr);
                final long ONE_M = 1024 * 1024;
                ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "上传文件大小不能超过2M");
            } catch (NumberFormatException e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
            }
        }
    }

    @Override
    protected String getFileFormat(Object inputSource) {
        String fileUrl = (String) inputSource;
        String fileOriginalName = FileUtil.mainName(fileUrl);
        this.originalFileName = fileOriginalName;
        String fileSuffix;
        if (StrUtil.isNotBlank(fileOriginalName) // 文件名不为空
                && StrUtil.isNotBlank(fileSuffix = FileUtil.getSuffix(fileOriginalName)) // 且拓展名也不为空
        ) { // 则将原始文件名的后缀名当作文件的格式类型
            return this.fileFormat = fileSuffix;
        }
        // 兜底使用 header 获取到的文件格式作为图片的格式类型
        return this.fileFormat;
    }

    @Override
    protected void processFile(Object inputSource, File file) {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }

    @Override
    protected String getOriginalFileName(Object inputSource) {
        return this.originalFileName;
    }
}
