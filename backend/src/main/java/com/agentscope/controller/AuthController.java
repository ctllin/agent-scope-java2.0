package com.agentscope.controller;

import com.agentscope.common.Response;
import com.agentscope.model.dto.LoginRequest;
import com.agentscope.model.vo.LoginVO;
import com.agentscope.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * <p>
 * 处理用户登录、注册等认证相关请求
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Response<LoginVO> login(@RequestBody LoginRequest request) {
        LoginVO result = authService.login(request);
        return Response.success(result);
    }

    /**
     * 快捷登录
     * <p>
     * 双击空白区域触发，使用root账号登录
     * 仅在开发环境启用
     * </p>
     */
    @PostMapping("/quick-login")
    public Response<LoginVO> quickLogin() {
        LoginVO result = authService.quickLogin();
        return Response.success(result);
    }

    /**
     * 根据token获取用户信息
     */
    @GetMapping("/current")
    public Response<LoginVO> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        LoginVO result = authService.getUserByToken(token);
        return Response.success(result);
    }
}
