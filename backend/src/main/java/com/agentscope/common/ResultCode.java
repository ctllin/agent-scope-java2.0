package com.agentscope.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码枚举
 * <p>
 * 定义所有可能的业务状态码和对应的消息。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "success"),
    
    /** 请求参数错误 */
    BAD_REQUEST(400, "bad request"),
    
    /** 未授权，需要登录 */
    UNAUTHORIZED(401, "unauthorized"),
    
    /** 无权限访问 */
    FORBIDDEN(403, "forbidden"),
    
    /** 资源不存在 */
    NOT_FOUND(404, "not found"),
    
    /** 服务器内部错误 */
    INTERNAL_ERROR(500, "internal error"),
    
    /** 用户不存在 */
    USER_NOT_FOUND(1001, "user not found"),
    
    /** 用户名或密码错误 */
    USERNAME_PASSWORD_ERROR(1002, "username or password error"),
    
    /** 用户已被禁用 */
    USER_DISABLED(1003, "user disabled"),
    
    /** Token无效或已过期 */
    TOKEN_INVALID(1004, "token invalid or expired"),
    
    /** 无操作权限 */
    NO_PERMISSION(1005, "no permission"),
    
    /** 数据已存在 */
    DATA_ALREADY_EXISTS(1006, "data already exists"),
    
    /** 数据不存在 */
    DATA_NOT_FOUND(1007, "data not found"),
    
    /** 快捷登录未启用 */
    QUICK_LOGIN_DISABLED(1008, "quick login is disabled");

    /** 状态码 */
    private final int code;
    
    /** 状态消息 */
    private final String message;
}
