package com.agentscope.model.entity;

import com.agentscope.common.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * <p>
 * 存储用户基本信息
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    /** 用户ID */
    @Id
    private String id;

    /** 用户名（唯一） */
    @Indexed(unique = true)
    private String username;

    /** 密码（加密存储） */
    private String password;

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
    @CreatedDate
    private LocalDateTime createdAt;

    /** 更新时间 */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
