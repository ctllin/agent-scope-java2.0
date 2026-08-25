package com.agentscope.service.impl;

import com.agentscope.common.ResultCode;
import com.agentscope.common.exception.BusinessException;
import com.agentscope.model.dto.AssignRolePermissionRequest;
import com.agentscope.model.dto.CreateRoleRequest;
import com.agentscope.model.entity.Role;
import com.agentscope.model.vo.RoleVO;
import com.agentscope.repository.RoleRepository;
import com.agentscope.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    /**
     * 创建角色
     */
    @Override
    public RoleVO createRole(CreateRoleRequest request) {
        // 检查角色编码是否已存在
        if (roleRepository.existsByCode(request.getCode())) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "角色编码已存在");
        }

        // 构建角色实体
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .code(request.getCode())
                .menuIds(List.of())
                .buttonIds(List.of())
                .build();

        // 保存角色
        role = roleRepository.save(role);

        return convertToVO(role);
    }

    /**
     * 更新角色
     */
    @Override
    public RoleVO updateRole(String id, CreateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在"));

        // 更新字段
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setCode(request.getCode());

        role = roleRepository.save(role);
        return convertToVO(role);
    }

    /**
     * 删除角色
     */
    @Override
    public void deleteRole(String id) {
        if (!roleRepository.existsById(id)) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        roleRepository.deleteById(id);
    }

    /**
     * 获取所有角色列表
     */
    @Override
    public List<RoleVO> listAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取角色信息
     */
    @Override
    public RoleVO getRoleById(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在"));
        return convertToVO(role);
    }

    /**
     * 分配角色权限
     */
    @Override
    public void assignRolePermission(AssignRolePermissionRequest request) {
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在"));

        // 更新菜单和按钮权限
        role.setMenuIds(request.getMenuIds());
        role.setButtonIds(request.getButtonIds());

        roleRepository.save(role);
    }

    /**
     * 将实体转换为VO
     */
    private RoleVO convertToVO(Role role) {
        return RoleVO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .code(role.getCode())
                .menuIds(role.getMenuIds())
                .buttonIds(role.getButtonIds())
                .createdAt(role.getCreatedAt())
                .build();
    }
}
