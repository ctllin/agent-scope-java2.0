package com.agentscope.service.impl;

import com.agentscope.common.ResultCode;
import com.agentscope.common.exception.BusinessException;
import com.agentscope.config.QuickLoginConfig;
import com.agentscope.model.dto.LoginRequest;
import com.agentscope.model.entity.User;
import com.agentscope.model.vo.LoginVO;
import com.agentscope.model.vo.UserVO;
import com.agentscope.service.AuthService;
import com.agentscope.service.UserService;
import com.agentscope.util.JwtUtil;
import com.agentscope.util.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final QuickLoginConfig quickLoginConfig;

    /**
     * 用户登录
     */
    @Override
    public LoginVO login(LoginRequest request) {
        // 根据用户名查找用户
        User user = userService.getUserByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_PASSWORD_ERROR);
        }

        // 验证密码
        if (!PasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_PASSWORD_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() == com.agentscope.common.enums.UserStatus.DISABLED) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 构建响应
        UserVO userVO = UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .root(user.isRoot())
                .createdAt(user.getCreatedAt())
                .build();

        return LoginVO.builder()
                .token(token)
                .user(userVO)
                .build();
    }

    /**
     * 快捷登录
     */
    @Override
    public LoginVO quickLogin() {
        // 检查快捷登录是否启用
        if (!quickLoginConfig.isEnabled()) {
            throw new BusinessException(ResultCode.QUICK_LOGIN_DISABLED);
        }

        // 使用预设的root账号登录
        LoginRequest request = LoginRequest.builder()
                .username(quickLoginConfig.getUsername())
                .password(quickLoginConfig.getPassword())
                .build();

        return login(request);
    }

    /**
     * 根据token获取用户信息
     */
    @Override
    public LoginVO getUserByToken(String token) {
        // 解析token
        String userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 获取用户信息
        UserVO userVO = userService.getUserById(userId);

        return LoginVO.builder()
                .token(token)
                .user(userVO)
                .build();
    }
}
