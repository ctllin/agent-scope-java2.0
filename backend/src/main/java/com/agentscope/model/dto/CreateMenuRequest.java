package com.agentscope.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建菜单请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMenuRequest {

    /** 父菜单ID */
    private String parentId;

    /** 菜单名称 */
    @NotBlank(message = "菜单名称不能为空")
    private String name;

    /** 菜单路径 */
    private String path;

    /** 菜单图标 */
    private String icon;

    /** 菜单类型（0-目录，1-菜单，2-按钮） */
    private Integer type;

    /** 排序号 */
    private Integer sort;

    /** 是否可见 */
    private Boolean visible;

    /** 权限标识 */
    private String permission;
}
