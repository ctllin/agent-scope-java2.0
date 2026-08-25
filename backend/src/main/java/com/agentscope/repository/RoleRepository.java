package com.agentscope.repository;

import com.agentscope.model.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色数据访问层
 */
@Repository
public interface RoleRepository extends MongoRepository<Role, String> {

    /**
     * 根据角色编码查找角色
     * @param code 角色编码
     * @return 角色信息
     */
    Optional<Role> findByCode(String code);

    /**
     * 检查角色编码是否存在
     * @param code 角色编码
     * @return 是否存在
     */
    boolean existsByCode(String code);
}
