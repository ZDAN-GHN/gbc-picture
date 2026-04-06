package com.zdan.gbcpicturebackend.interfaces.facade;

import com.zdan.gbcpicturebackend.infrastructure.common.BaseResponse;
import com.zdan.gbcpicturebackend.infrastructure.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
