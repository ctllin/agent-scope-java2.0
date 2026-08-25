package com.agentscope.repository;

import com.agentscope.model.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 文档数据访问层
 */
@Repository
public interface DocumentRepository extends MongoRepository<KnowledgeDocument, String> {

    /**
     * 根据知识库ID查找文档列表
     */
    java.util.List<KnowledgeDocument> findByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 统计知识库下的文档数量
     */
    long countByKnowledgeBaseId(String knowledgeBaseId);

    /**
     * 按知识库ID和文件名查找文档（用于上传时检测重名）
     */
    java.util.List<KnowledgeDocument> findByKnowledgeBaseIdAndName(String knowledgeBaseId, String name);
}
