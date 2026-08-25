package com.agentscope.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建角色请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /** 角色描述 */
    private String description;

    /** 角色编码 */
    @NotBlank(message = "角色编码不能为空")
    private String code;
}
