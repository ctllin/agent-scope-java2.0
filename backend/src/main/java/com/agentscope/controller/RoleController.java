package com.agentscope.controller;

import com.agentscope.common.Response;
import com.agentscope.model.dto.AssignRolePermissionRequest;
import com.agentscope.model.dto.CreateRoleRequest;
import com.agentscope.model.vo.RoleVO;
import com.agentscope.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 * <p>
 * 处理角色增删改查及权限分配相关请求
 * </p>
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 创建角色
     */
    @PostMapping
    public Response<RoleVO> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleVO result = roleService.createRole(request);
        return Response.success(result);
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    public Response<RoleVO> updateRole(@PathVariable String id, @Valid @RequestBody CreateRoleRequest request) {
        RoleVO result = roleService.updateRole(id, request);
        return Response.success(result);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Response<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return Response.success();
    }

    /**
     * 获取所有角色列表
     */
    @GetMapping
    public Response<List<RoleVO>> listRoles() {
        List<RoleVO> result = roleService.listAllRoles();
        return Response.success(result);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    public Response<RoleVO> getRoleById(@PathVariable String id) {
        RoleVO result = roleService.getRoleById(id);
        return Response.success(result);
    }

    /**
     * 分配角色权限
     */
    @PostMapping("/assign-permission")
    public Response<Void> assignRolePermission(@Valid @RequestBody AssignRolePermissionRequest request) {
        roleService.assignRolePermission(request);
        return Response.success();
    }
}
