package com.zdan.gbcpicturebackend.infrastructure.api;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.zdan.gbcpicturebackend.infrastructure.config.CosClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * cos通用服务
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file)
            throws CosClientException {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(),
                key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key)
            throws CosClientException {
        return cosClient.getObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 上传图片对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file)
            throws CosClientException {
        // 上传到对象存储请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 对图片处理（获取基本信息也被视作一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 代表返回原图信息
        picOperations.setIsPicInfo(1); // ---- 没有这个配置，返回的PutObjectResult是无法获取到图片信息的
        // 处理规则集合
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩规则（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule(); // 压缩规则
        compressRule.setFileId(webpKey); // 设定处理后的生成的文件的文件名
        compressRule.setRule("imageMogr2/format/webp"); // 具体规则描述
        compressRule.setBucket(cosClientConfig.getBucket()); // 设置规则生效的桶名
        rules.add(compressRule); // 将定义好的规则加入规则集合
        // 图片缩放规则（缩略图处理）
        if (file.length() > 2 * 1024) { // 仅对大小大于 20k 的图片进行缩略处理
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setFileId(FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key));
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s", 256, 256));
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            rules.add(thumbnailRule);
        }
        // 注意要将规则集合放到“ 图片处理 ”对象中一并作为请求发送才能使得规则生效
        picOperations.setRules(rules);
        // 构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象存储中的存储对象
     *
     * @param key 对象唯一标识
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
