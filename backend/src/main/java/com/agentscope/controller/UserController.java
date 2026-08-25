package com.agentscope.controller;

import com.agentscope.common.Response;
import com.agentscope.model.dto.ChangePasswordRequest;
import com.agentscope.model.dto.CreateUserRequest;
import com.agentscope.model.dto.UpdateUserRequest;
import com.agentscope.model.vo.PageResultVO;
import com.agentscope.model.vo.UserVO;
import com.agentscope.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 * <p>
 * 处理用户增删改查相关请求
 * </p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    public Response<UserVO> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserVO result = userService.createUser(request);
        return Response.success(result);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Response<UserVO> updateUser(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        UserVO result = userService.updateUser(id, request);
        return Response.success(result);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Response<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return Response.success();
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public Response<UserVO> getUserById(@PathVariable String id) {
        UserVO result = userService.getUserById(id);
        return Response.success(result);
    }

    /**
     * 分页查询用户列表
     */
    @GetMapping
    public Response<PageResultVO<UserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        PageResultVO<UserVO> result = userService.listUsers(page, size, keyword);
        return Response.success(result);
    }

    /**
     * 修改密码
     */
    @PutMapping("/{id}/password")
    public Response<Void> changePassword(@PathVariable String id, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return Response.success();
    }
}
