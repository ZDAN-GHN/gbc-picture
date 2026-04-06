package com.zdan.gbcpicturebackend.infrastructure.api.imagesearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取图片搜索结果地址（step 1）
 */
@Slf4j
public class GetImagePageUrlApi {

    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl) {
        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        // 添加 sdkParams 参数（反爬虫验证参数）
        // 这些参数是百度的反爬虫机制，理论上需要动态生成
        // 但实际测试发现，传入固定值或空值也可能通过验证
        String sdkParams = generateSdkParams();
        formData.put("sdkParams", sdkParams);

        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        try {
            // 2. 发送 POST 请求到百度接口，必须添加请求头模拟浏览器行为
            HttpResponse response = HttpRequest.post(url)
                    .header("Accept", "*/*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Origin", "https://graph.baidu.com")
                    .header("Referer", "https://graph.baidu.com/pcpage/index?tpl_from=pc")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .form(formData)
                    .timeout(10000)
                    .execute();
            System.out.println(response);
            // 判断响应状态
            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            // 解析响应
            String responseBody = response.body();
            Map<String, Object> result = JSONUtil.toBean(responseBody, Map.class);

            // 3. 处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            // 对 URL 进行解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    /**
     * 生成 sdkParams 参数
     * 百度的反爬虫验证参数，包含 data、key_id、sign
     * <p>
     * 实现说明：
     * 1. data: 通常是对请求参数、时间戳等信息的加密结果（使用特定算法）
     * 2. key_id: 密钥ID，通常固定为 "23"
     * 3. sign: 签名，对 data 进行签名验证
     * <p>
     * 由于百度的加密算法是前端 JS 动态生成的，且经常变化，
     * 这里提供几种方案：
     * 方案1: 传空值（某些情况下可能通过）
     * 方案2: 使用固定的有效值（从浏览器抓包获取，但会过期）
     * 方案3: 逆向百度的 JS 加密算法（复杂且容易失效）
     */
    private static String generateSdkParams() {
        Map<String, String> sdkParamsMap = new HashMap<>();

        // todo 模拟从浏览器访问百度识图，抓包获取 sdk_param

        return JSONUtil.toJsonStr(sdkParamsMap);
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://shitu-query-gz.gz.bcebos.com/2026-01-11/13/a02b729d47db0077?authorization=bce-auth-v1%2F7e22d8caf5af46cc9310f1e3021709f3%2F2026-01-11T06%3A04%3A15Z%2F300%2Fhost%2F3ee03ac128eb6e177518d6e99b41dfa119a94a3b66091dfed91cdee697cc8ad0";
        String result = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果URL：" + result);
    }
}