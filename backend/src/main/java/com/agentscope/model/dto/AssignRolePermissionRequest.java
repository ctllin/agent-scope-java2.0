package com.agentscope.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色权限分配请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRolePermissionRequest {

    /** 角色ID */
    private String roleId;

    /** 菜单ID列表 */
    private List<String> menuIds;

    /** 按钮权限ID列表 */
    private List<String> buttonIds;
}
