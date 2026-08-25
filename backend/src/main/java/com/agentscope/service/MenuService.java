package com.agentscope.service;

import com.agentscope.model.dto.CreateMenuRequest;
import com.agentscope.model.vo.MenuVO;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface MenuService {

    /**
     * 创建菜单
     * @param request 创建菜单请求
     * @return 菜单信息
     */
    MenuVO createMenu(CreateMenuRequest request);

    /**
     * 更新菜单
     * @param id 菜单ID
     * @param request 更新菜单请求
     * @return 菜单信息
     */
    MenuVO updateMenu(String id, CreateMenuRequest request);

    /**
     * 删除菜单
     * @param id 菜单ID
     */
    void deleteMenu(String id);

    /**
     * 获取菜单树形结构
     * @return 菜单树
     */
    List<MenuVO> getMenuTree();

    /**
     * 根据角色ID获取菜单树
     * @param roleId 角色ID
     * @return 菜单树
     */
    List<MenuVO> getMenuTreeByRoleId(String roleId);
}
