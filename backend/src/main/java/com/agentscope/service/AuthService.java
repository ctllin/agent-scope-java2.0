package com.agentscope.service;

import com.agentscope.model.dto.LoginRequest;
import com.agentscope.model.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应（包含token）
     */
    LoginVO login(LoginRequest request);

    /**
     * 快捷登录（root账号）
     * @return 登录响应（包含token）
     */
    LoginVO quickLogin();

    /**
     * 根据token获取用户信息
     * @param token JWT令牌
     * @return 用户信息
     */
    LoginVO getUserByToken(String token);
}
