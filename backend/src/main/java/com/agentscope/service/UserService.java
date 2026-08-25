package com.agentscope.service;

import com.agentscope.model.dto.ChangePasswordRequest;
import com.agentscope.model.dto.CreateUserRequest;
import com.agentscope.model.dto.UpdateUserRequest;
import com.agentscope.model.entity.User;
import com.agentscope.model.vo.PageResultVO;
import com.agentscope.model.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 创建用户
     * @param request 创建用户请求
     * @return 用户信息
     */
    UserVO createUser(CreateUserRequest request);

    /**
     * 更新用户信息
     * @param id 用户ID
     * @param request 更新用户请求
     * @return 用户信息
     */
    UserVO updateUser(String id, UpdateUserRequest request);

    /**
     * 删除用户
     * @param id 用户ID
     */
    void deleteUser(String id);

    /**
     * 根据ID获取用户信息
     * @param id 用户ID
     * @return 用户信息
     */
    UserVO getUserById(String id);

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 分页查询用户列表
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @return 分页结果
     */
    PageResultVO<UserVO> listUsers(int page, int size, String keyword);

    /**
     * 修改密码
     * @param id 用户ID
     * @param request 修改密码请求
     */
    void changePassword(String id, ChangePasswordRequest request);
}
