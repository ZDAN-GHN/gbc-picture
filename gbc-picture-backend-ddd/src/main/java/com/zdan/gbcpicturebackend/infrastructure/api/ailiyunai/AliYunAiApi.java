package com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.model.CreateOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.model.GetOutPaintingTaskResponse;
import com.zdan.gbcpicturebackend.infrastructure.api.ailiyunai.model.CreateOutPaintingTaskRequest;
import com.zdan.gbcpicturebackend.infrastructure.exception.BusinessException;
import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;
import com.zdan.gbcpicturebackend.infrastructure.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

    // 读取配置文件中的apiKey
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    // 创建任务地址
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    // 查询任务状态
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        // 参数校验
        if (createOutPaintingTaskRequest == null || createOutPaintingTaskRequest.getInput() == null ||
                createOutPaintingTaskRequest.getInput().getImageUrl() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 发送请求
        /*
            curl --location --request POST 'https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting' \\
            --header "Authorization: Bearer $DASHSCOPE_API_KEY" \\
            --header 'X-DashScope-Async: enable' \\
            --header 'Content-Type: application/json' \\
            --data '{
                "model": "image-out-painting",
                "input": {
                    "image_url": "http://xxx/image.jpg"
                },
                "parameters":{
                    "angle": 45,
                    "x_scale":1.5,
                    "y_scale":1.5
                }
            }'
         */
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header("X-DashScope-Async", "enable") // 必须开启异步处理
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("AI 扩图失败，响应：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            CreateOutPaintingTaskResponse createOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            if (createOutPaintingTaskResponse == null || createOutPaintingTaskResponse.getCode() != null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
            }
            return createOutPaintingTaskResponse;
        }
    }

    /**
     * 查询创建任务的结果
     *
     * @param taskId
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        // 发送请求
        /*
            curl -X GET https://dashscope.aliyuncs.com/api/v1/tasks/86ecf553-d340-4e21-xxxxxxxxx \\
            --header "Authorization: Bearer $DASHSCOPE_API_KEY"
        */
        HttpRequest httpRequest = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            ThrowUtils.throwIf(!httpResponse.isOk(), ErrorCode.OPERATION_ERROR, "获取任务结果失败");
            GetOutPaintingTaskResponse getOutPaintingTaskResponse = JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
            if (getOutPaintingTaskResponse == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
            }
            return getOutPaintingTaskResponse;
        }
    }
}
