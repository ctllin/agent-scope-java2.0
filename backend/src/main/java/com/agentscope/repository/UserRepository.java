package com.agentscope.repository;

import com.agentscope.model.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户信息
     */
    Optional<User> findByUsername(String username);

    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
}
