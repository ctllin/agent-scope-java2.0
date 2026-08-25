package com.agentscope.model.entity;

import com.agentscope.common.enums.MenuType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 菜单实体类
 * <p>
 * 存储菜单和按钮权限信息，支持树形结构
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "menus")
public class Menu {

    /** 菜单ID */
    @Id
    private String id;

    /** 父菜单ID（顶级菜单为null） */
    private String parentId;

    /** 菜单名称 */
    private String name;

    /** 菜单路径（前端路由路径） */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 菜单类型（目录、菜单、按钮） */
    private MenuType type;

    /** 排序号 */
    private Integer sort;

    /** 是否可见 */
    private boolean visible;

    /** 权限标识（如：user:add, user:delete） */
    private String permission;

    /** 创建时间 */
    @CreatedDate
    private LocalDateTime createdAt;

    /** 更新时间 */
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
