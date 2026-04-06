package com.zdan.gbcpicturebackend.infrastructure.common;

import com.zdan.gbcpicturebackend.infrastructure.exception.ErrorCode;

public class ResultUtils {

    /**
     * 成功
     *
     * @param data    数据
     * @param message 成功消息
     * @param <T>     响应类型
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(0, data, message);
    }

    /**
     * 成功
     *
     * @param data 数据
     * @param <T>  响应类型
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return ResultUtils.success(data, "OK");
    }

    /**
     * 失败
     *
     * @param code    错误码
     * @param data    数据
     * @param message 错误消息
     * @param <T>     响应类型
     * @return 响应
     */
    public static <T> BaseResponse<T> error(int code, T data, String message) {
        return new BaseResponse<>(code, data, message);
    }

    /**
     * 失败
     *
     * @param code    错误码
     * @param message 错误消息
     * @param <T>     响应类型
     * @return 响应
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return ResultUtils.error(code, null, message);
    }

    /**
     * 失败
     *
     * @param errorCode 错误码枚举
     * @param data      数据
     * @param <T>       响应类型
     * @return 响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, T data) {
        return ResultUtils.error(errorCode.getCode(), data, errorCode.getMessage());
    }

    /**
     * 失败
     *
     * @param errorCode 错误码枚举
     * @param <T>       响应类型
     * @return 响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return ResultUtils.error(errorCode, null);
    }
}
