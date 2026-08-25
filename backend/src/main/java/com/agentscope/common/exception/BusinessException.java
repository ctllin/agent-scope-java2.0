package com.agentscope.common.exception;

import com.agentscope.common.Response;
import com.agentscope.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * <p>
 * 用于抛出业务逻辑异常，携带状态码和错误消息
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码 */
    private final ResultCode resultCode;

    /**
     * 构造业务异常
     *
     * @param resultCode 错误码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /**
     * 构造业务异常（自定义消息）
     *
     * @param resultCode 错误码
     * @param message    错误消息
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
