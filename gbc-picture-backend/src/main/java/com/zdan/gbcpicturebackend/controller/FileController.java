package com.zdan.gbcpicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.zdan.gbcpicturebackend.annotation.AuthCheck;
import com.zdan.gbcpicturebackend.common.BaseResponse;
import com.zdan.gbcpicturebackend.common.ResultUtils;
import com.zdan.gbcpicturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private CosManager cosManager;

    @AuthCheck
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile,
                                               HttpServletRequest request) {
        String fileName = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", fileName);
        File file = null;
        try {
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            return ResultUtils.success(filepath);
        } catch (IOException e) {
            log.error("failed to upload file, filepath is {}", filepath);
            throw new RuntimeException(e);
        } finally {
            if (file != null && !file.delete())
                log.error("file delete error, filepath is {}", filepath);
        }
    }

    @PostMapping
    public void testDownloadFile(String filepath,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        COSObjectInputStream objectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            objectInputStream = cosObject.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(objectInputStream);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 响应流式数据
            response.getOutputStream().write(bytes);
            // 刷新流
            response.getOutputStream().flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (objectInputStream != null) objectInputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
