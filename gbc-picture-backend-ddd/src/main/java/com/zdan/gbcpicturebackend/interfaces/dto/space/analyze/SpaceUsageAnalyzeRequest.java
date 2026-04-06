package com.zdan.gbcpicturebackend.interfaces.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 空间使用分析请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUsageAnalyzeRequest extends SpaceAnalyzeRequest {

    private static final long serialVersionUID = 6381436347519155233L;
}
