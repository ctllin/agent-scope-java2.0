package com.agentscope.service.impl;

import com.agentscope.common.ResultCode;
import com.agentscope.common.exception.BusinessException;
import com.agentscope.model.dto.ChangePasswordRequest;
import com.agentscope.model.dto.CreateUserRequest;
import com.agentscope.model.dto.UpdateUserRequest;
import com.agentscope.model.entity.User;
import com.agentscope.common.enums.UserStatus;
import com.agentscope.model.vo.PageResultVO;
import com.agentscope.model.vo.UserVO;
import com.agentscope.repository.UserRepository;
import com.agentscope.service.UserService;
import com.agentscope.util.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * 创建用户
     */
    @Override
    public UserVO createUser(CreateUserRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "用户名已存在");
        }

        // 构建用户实体
        User user = User.builder()
                .username(request.getUsername())
                .password(PasswordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(UserStatus.ENABLED)
                .root(false)
                .build();

        // 保存用户
        user = userRepository.save(user);

        return convertToVO(user);
    }

    /**
     * 更新用户信息
     */
    @Override
    public UserVO updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        // 更新字段
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus() == 1 ? UserStatus.ENABLED : UserStatus.DISABLED);
        }

        user = userRepository.save(user);
        return convertToVO(user);
    }

    /**
     * 删除用户
     */
    @Override
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        // 不允许删除root用户
        if (user.isRoot()) {
            throw new BusinessException(ResultCode.NO_PERMISSION, "不允许删除root用户");
        }

        userRepository.deleteById(id);
    }

    /**
     * 根据ID获取用户信息
     */
    @Override
    public UserVO getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        return convertToVO(user);
    }

    /**
     * 根据用户名获取用户信息
     */
    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    /**
     * 分页查询用户列表
     */
    @Override
    public PageResultVO<UserVO> listUsers(int page, int size, String keyword) {
        Query query = new Query();
        
        // 如果有搜索关键词，按用户名或昵称模糊查询
        if (keyword != null && !keyword.isBlank()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("username").regex(keyword, "i"),
                    Criteria.where("nickname").regex(keyword, "i")
            ));
        }

        // 执行分页查询
        Pageable pageable = PageRequest.of(page - 1, size);
        query.with(pageable);

        List<User> users = mongoTemplate.find(query, User.class);
        long total = mongoTemplate.count(query, User.class);

        // 转换为VO
        List<UserVO> userVOs = users.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResultVO.<UserVO>builder()
                .total(total)
                .page(page)
                .size(size)
                .records(userVOs)
                .build();
    }

    /**
     * 修改密码
     */
    @Override
    public void changePassword(String id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        // 验证原密码
        if (!PasswordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_PASSWORD_ERROR, "原密码错误");
        }

        // 更新密码
        user.setPassword(PasswordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * 将实体转换为VO
     */
    private UserVO convertToVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .root(user.isRoot())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
