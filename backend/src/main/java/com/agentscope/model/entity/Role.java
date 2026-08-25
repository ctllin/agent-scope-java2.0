package com.agentscope.model.entity;

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
import java.util.List;

/**
 * 角色实体类
 * <p>
 * 存储角色信息，支持角色分配菜单和按钮权限
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roles")
public class Role {

    /** 角色ID */
    @Id
    private String id;

    /** 角色名称（唯一） */
    @Indexed(unique = true)
    private String name;

    /** 角色描述 */
    private String description;

    /** 角色编码（如：ADMIN, USER） */
    @Indexed(unique = true)
    private String code;

    /** 拥有的菜单ID列表 */
    private List<String> menuIds;

    /** 拥有的按钮权限ID列表 */
    private List<String> buttonIds;

    /** 创建时间 */
    @CreatedDate
    private LocalDateTime createdAt;

    /** 更新时间 */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
