package com.agentscope.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户角色分配请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserRoleRequest {

    /** 用户ID */
    private String userId;

    /** 角色ID列表 */
    private java.util.List<String> roleIds;
}
