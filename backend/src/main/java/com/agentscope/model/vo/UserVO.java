package com.agentscope.model.vo;

import com.agentscope.common.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /** 用户ID */
    private String id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 用户状态 */
    private UserStatus status;

    /** 是否为root用户 */
    private boolean root;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
