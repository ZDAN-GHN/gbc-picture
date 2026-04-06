package com.zdan.gbcpicturebackend.api.imagesearch;

import com.zdan.gbcpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.zdan.gbcpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.zdan.gbcpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.zdan.gbcpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 百度识图已经加强了防爬虫，这个 api 已失效
 */
@Deprecated
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        List<ImageSearchResult> imageList = searchImage("https://www.codefather.cn/logo.png");
        System.out.println("结果列表" + imageList);
    }
}
