package com.agentscope.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色信息视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    /** 角色ID */
    private String id;

    /** 角色名称 */
    private String name;

    /** 角色描述 */
    private String description;

    /** 角色编码 */
    private String code;

    /** 拥有的菜单ID列表 */
    private List<String> menuIds;

    /** 拥有的按钮权限ID列表 */
    private List<String> buttonIds;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
