package com.agentscope.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** JWT令牌 */
    private String token;

    /** 用户信息 */
    private UserVO user;
}
