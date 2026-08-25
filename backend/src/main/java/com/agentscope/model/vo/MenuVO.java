package com.agentscope.model.vo;

import com.agentscope.common.enums.MenuType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单信息视图对象（树形结构）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuVO {

    /** 菜单ID */
    private String id;

    /** 父菜单ID */
    private String parentId;

    /** 菜单名称 */
    private String name;

    /** 菜单路径 */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 菜单类型 */
    private MenuType type;

    /** 排序号 */
    private Integer sort;

    /** 是否可见 */
    private boolean visible;

    /** 权限标识 */
    private String permission;

    /** 子菜单列表 */
    private List<MenuVO> children;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
