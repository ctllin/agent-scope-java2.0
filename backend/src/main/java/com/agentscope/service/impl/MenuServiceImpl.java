package com.agentscope.service.impl;

import com.agentscope.common.ResultCode;
import com.agentscope.common.enums.MenuType;
import com.agentscope.common.exception.BusinessException;
import com.agentscope.model.dto.CreateMenuRequest;
import com.agentscope.model.entity.Menu;
import com.agentscope.model.entity.Role;
import com.agentscope.model.vo.MenuVO;
import com.agentscope.repository.MenuRepository;
import com.agentscope.repository.RoleRepository;
import com.agentscope.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务实现类
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;

    /**
     * 创建菜单
     */
    @Override
    public MenuVO createMenu(CreateMenuRequest request) {
        // 构建菜单实体
        Menu menu = Menu.builder()
                .parentId(request.getParentId())
                .name(request.getName())
                .path(request.getPath())
                .icon(request.getIcon())
                .type(MenuType.values()[request.getType()])
                .sort(request.getSort() != null ? request.getSort() : 0)
                .visible(request.getVisible() != null ? request.getVisible() : true)
                .permission(request.getPermission())
                .build();

        // 保存菜单
        menu = menuRepository.save(menu);

        return convertToVO(menu);
    }

    /**
     * 更新菜单
     */
    @Override
    public MenuVO updateMenu(String id, CreateMenuRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "菜单不存在"));

        // 更新字段
        menu.setParentId(request.getParentId());
        menu.setName(request.getName());
        menu.setPath(request.getPath());
        menu.setIcon(request.getIcon());
        if (request.getType() != null) {
            menu.setType(MenuType.values()[request.getType()]);
        }
        if (request.getSort() != null) {
            menu.setSort(request.getSort());
        }
        if (request.getVisible() != null) {
            menu.setVisible(request.getVisible());
        }
        menu.setPermission(request.getPermission());

        menu = menuRepository.save(menu);
        return convertToVO(menu);
    }

    /**
     * 删除菜单
     */
    @Override
    public void deleteMenu(String id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "菜单不存在"));

        // 检查是否有子菜单
        List<Menu> children = menuRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "存在子菜单，无法删除");
        }

        menuRepository.deleteById(id);
    }

    /**
     * 获取菜单树形结构
     */
    @Override
    public List<MenuVO> getMenuTree() {
        // 获取所有菜单
        List<Menu> allMenus = menuRepository.findAll();
        
        // 构建菜单树
        return buildMenuTree(allMenus, null);
    }

    /**
     * 根据角色ID获取菜单树
     */
    @Override
    public List<MenuVO> getMenuTreeByRoleId(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在"));

        // 获取角色拥有的菜单ID
        List<String> menuIds = role.getMenuIds();
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }

        // 获取所有菜单
        List<Menu> allMenus = menuRepository.findAll();
        
        // 过滤出角色拥有的菜单
        List<Menu> roleMenus = allMenus.stream()
                .filter(menu -> menuIds.contains(menu.getId()))
                .collect(Collectors.toList());

        // 构建菜单树
        return buildMenuTree(roleMenus, null);
    }

    /**
     * 递归构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<Menu> allMenus, String parentId) {
        // 按父ID分组
        Map<String, List<Menu>> menuMap = allMenus.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() != null ? menu.getParentId() : ""));

        // 获取当前层级的菜单
        List<Menu> currentLevelMenus = menuMap.getOrDefault(parentId != null ? parentId : "", List.of());

        // 按排序号排序
        currentLevelMenus.sort((a, b) -> {
            Integer sortA = a.getSort() != null ? a.getSort() : 0;
            Integer sortB = b.getSort() != null ? b.getSort() : 0;
            return sortA.compareTo(sortB);
        });

        // 递归构建子菜单
        return currentLevelMenus.stream()
                .map(menu -> {
                    MenuVO vo = convertToVO(menu);
                    vo.setChildren(buildMenuTree(allMenus, menu.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将实体转换为VO
     */
    private MenuVO convertToVO(Menu menu) {
        return MenuVO.builder()
                .id(menu.getId())
                .parentId(menu.getParentId())
                .name(menu.getName())
                .path(menu.getPath())
                .icon(menu.getIcon())
                .type(menu.getType())
                .sort(menu.getSort())
                .visible(menu.isVisible())
                .permission(menu.getPermission())
                .createdAt(menu.getCreatedAt())
                .children(new ArrayList<>())
                .build();
    }
}
