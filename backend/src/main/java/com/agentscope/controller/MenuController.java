package com.agentscope.controller;

import com.agentscope.common.Response;
import com.agentscope.model.dto.CreateMenuRequest;
import com.agentscope.model.vo.MenuVO;
import com.agentscope.service.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 * <p>
 * 处理菜单增删改查相关请求
 * </p>
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /**
     * 创建菜单
     */
    @PostMapping
    public Response<MenuVO> createMenu(@Valid @RequestBody CreateMenuRequest request) {
        MenuVO result = menuService.createMenu(request);
        return Response.success(result);
    }

    /**
     * 更新菜单
     */
    @PutMapping("/{id}")
    public Response<MenuVO> updateMenu(@PathVariable String id, @Valid @RequestBody CreateMenuRequest request) {
        MenuVO result = menuService.updateMenu(id, request);
        return Response.success(result);
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public Response<Void> deleteMenu(@PathVariable String id) {
        menuService.deleteMenu(id);
        return Response.success();
    }

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    public Response<List<MenuVO>> getMenuTree() {
        List<MenuVO> result = menuService.getMenuTree();
        return Response.success(result);
    }

    /**
     * 根据角色ID获取菜单树
     */
    @GetMapping("/tree/{roleId}")
    public Response<List<MenuVO>> getMenuTreeByRoleId(@PathVariable String roleId) {
        List<MenuVO> result = menuService.getMenuTreeByRoleId(roleId);
        return Response.success(result);
    }
}
