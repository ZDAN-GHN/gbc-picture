package com.zdan.gbcpicturebackend.service.impl;


import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zdan.gbcpicturebackend.exception.BusinessException;
import com.zdan.gbcpicturebackend.exception.ErrorCode;
import com.zdan.gbcpicturebackend.exception.ThrowUtils;
import com.zdan.gbcpicturebackend.mapper.SpaceMapper;
import com.zdan.gbcpicturebackend.model.dto.space.analyze.*;
import com.zdan.gbcpicturebackend.model.entity.Picture;
import com.zdan.gbcpicturebackend.model.entity.Space;
import com.zdan.gbcpicturebackend.model.entity.User;
import com.zdan.gbcpicturebackend.model.vo.analyze.*;
import com.zdan.gbcpicturebackend.service.PictureService;
import com.zdan.gbcpicturebackend.service.SpaceAnalyzeService;
import com.zdan.gbcpicturebackend.service.SpaceService;
import com.zdan.gbcpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author LXH
 * @description 针对表【space(空间)】的数据库操作AnalyzeService实现
 * @createDate 2026-01-08 15:46:23
 */
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        // 校验参数 & 校验权限 & 获取图库对象
        Space space = this.checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
        // 分析空间使用情况
        SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
        if (space != null) { // 分析私有图库，只需要space的数据即可
            // 获取空间使用情况数据
            Long totalSize = space.getTotalSize();
            Long totalCount = space.getTotalCount();
            Long maxSize = space.getMaxSize();
            Long maxCount = space.getMaxCount();
            Double sizeUsageRatio = NumberUtil.round(totalSize * 100.0 / maxSize, 2).doubleValue();
            Double countUsageRatio = NumberUtil.round(totalCount * 100.0 / maxCount, 2).doubleValue();
            // 封装返回结果
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setMaxSize(maxSize);
            spaceUsageAnalyzeResponse.setMaxCount(maxCount);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
        } else { // 分析公共图库或者全部图库需要查询 Picture 表
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            this.fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            // 只查询图片表的 “picSize”，一列，查询结果也只要这一列
            queryWrapper.select("picSize");
            List<Object> objectList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            // 统计使用空间 和 图片数量
            long usedSize = objectList.stream()
                    .filter(ObjectUtil::isNotNull) // 防止有脏数据导致分析失败（map 对应操作如果涉及流内个体对象的自身操作，都应该保证非 null）
                    .mapToLong(obj -> ((Number) obj).longValue())
                    .sum();
            long usedCount = objectList.size();
            // 封装返回结果 --- 公共图库或者全部图库没有最大空间和最大图片数量的限制，不需要设定
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
        }
        return spaceUsageAnalyzeResponse;
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 校验参数 & 校验权限
        this.checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 分析空间图片分类
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize").groupBy("category");
        List<Map<String, Object>> mapList = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return mapList.stream()
                .filter(ObjectUtil::isNotNull)
                .map(result -> {
                    // 获取查询结果
                    String category = (String) result.get("category"); // category 为 null 代表为分类
                    Long count = ((Number) result.get("count")).longValue(); // 查询结果为 Number 类型，不能够直接强转，需要调用 longValue 方法转为 Long 类型
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    // 封装返回值
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 校验参数 & 校验权限
        this.checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 分析空间图片标签
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags");
        // 获取标签 json 列表
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjectUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());
        // 统计标签使用次数
        Map<String, Long> tagCountMap = tagJsonList.stream()
                .flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream()) // List<List<String>> -> List<String>
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting())); // List<String> -> Map<String, Long>，tag 为 null 代表无标签
        // 封装结果并返回
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) // 降序排序
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue())) // 将每个<tag, count>键值对转为响应对象
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        // 校验参数 & 校验权限
        this.checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 分析空间图片大小
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select("picSize");
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjectUtil::isNotNull)
                .map(obj -> ((Number) obj).longValue())
                .collect(Collectors.toList());
        // 统计图片大小
        Map<String, Long> sizeRangeCountMap = new LinkedHashMap<>();
        sizeRangeCountMap.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRangeCountMap.put("100KB-500KB", picSizeList.stream().filter(size -> size > 100 * 1024 && size < 500 * 1024).count());
        sizeRangeCountMap.put("500KB-1MB", picSizeList.stream().filter(size -> size > 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRangeCountMap.put(">1MB", picSizeList.stream().filter(size -> size > 1 * 1024 * 1024).count());
        // 封装结果并返回
        return sizeRangeCountMap.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        // 校验参数 & 校验权限
        this.checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 分析用户上传行为
        Long userId = spaceUserAnalyzeRequest.getUserId();
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        this.fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        queryWrapper.eq(ObjectUtil.isNotEmpty(userId), "userId", userId);
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period, COUNT(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period, COUNT(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period, COUNT(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间单位");
        }
        // 分组排序
        queryWrapper.groupBy("period").orderByAsc("period");
        // 查询结果
        List<Map<String, Object>> mapList = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return mapList.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        // 参数校验
        ThrowUtils.throwIf(spaceRankAnalyzeRequest.getTopN() == null, ErrorCode.PARAMS_ERROR);
        // 权限校验
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权限");
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "totalSize", "totalCount", "userId")
                .orderByDesc("totalSize") // 按照空间使用量进行降序排序
                .last("limit " + spaceRankAnalyzeRequest.getTopN()); // 取前 n 条
        // 查询并返回
        return spaceService.list(queryWrapper);
    }

    /**
     * 校验是否具备空间分析的权限，附带返回 space
     *
     * @param spaceAnalyzeRequest
     * @param loginUser
     * @return 如果分析的私有图库，返回值不为空
     */
    private Space checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryPublic || queryAll) { // 如果是分析公共图库或所有图库，系统管理员才有权限
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权限");
            return null;
        } else { // 私有图库，图库管理员才有权限
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR, "未指定查询范围");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            spaceService.checkSpaceAuth(loginUser, space);
            return space;
        }
    }

    /**
     * 根据分析范围补充查询sql
     *
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
        } else if (!queryAll) {
            queryWrapper.eq("spaceId", spaceId);
        }
    }
}




