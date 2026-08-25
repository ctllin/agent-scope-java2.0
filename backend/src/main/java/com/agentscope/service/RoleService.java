package com.agentscope.service;

import com.agentscope.model.dto.AssignRolePermissionRequest;
import com.agentscope.model.dto.CreateRoleRequest;
import com.agentscope.model.vo.RoleVO;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 创建角色
     * @param request 创建角色请求
     * @return 角色信息
     */
    RoleVO createRole(CreateRoleRequest request);

    /**
     * 更新角色
     * @param id 角色ID
     * @param request 更新角色请求
     * @return 角色信息
     */
    RoleVO updateRole(String id, CreateRoleRequest request);

    /**
     * 删除角色
     * @param id 角色ID
     */
    void deleteRole(String id);

    /**
     * 获取所有角色列表
     * @return 角色列表
     */
    List<RoleVO> listAllRoles();

    /**
     * 根据ID获取角色信息
     * @param id 角色ID
     * @return 角色信息
     */
    RoleVO getRoleById(String id);

    /**
     * 分配角色权限
     * @param request 权限分配请求
     */
    void assignRolePermission(AssignRolePermissionRequest request);
}
