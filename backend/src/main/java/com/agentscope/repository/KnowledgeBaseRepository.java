package com.agentscope.repository;

import com.agentscope.model.entity.KnowledgeBase;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库数据访问层
 */
@Repository
public interface KnowledgeBaseRepository extends MongoRepository<KnowledgeBase, String> {

    /**
     * 根据创建者ID查找知识库列表
     * @param creatorId 创建者ID
     * @return 知识库列表
     */
    List<KnowledgeBase> findByCreatorId(String creatorId);

    /**
     * 根据知识库名称查找
     * @param name 知识库名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}
