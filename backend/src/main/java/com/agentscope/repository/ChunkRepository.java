package com.agentscope.repository;

import com.agentscope.model.entity.DocumentChunk;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends MongoRepository<DocumentChunk, String> {

    List<DocumentChunk> findByDocumentId(String documentId);

    List<DocumentChunk> findByDocumentIdAndDelFlag(String documentId, String delFlag);

    List<DocumentChunk> findByDocumentIdOrderBySequence(String documentId);

    List<DocumentChunk> findByKnowledgeBaseId(String knowledgeBaseId);

    List<DocumentChunk> findByKnowledgeBaseIdAndDelFlag(String knowledgeBaseId, String delFlag);

    List<DocumentChunk> findByIdInAndDelFlag(List<String> ids, String delFlag);

    long countByDocumentId(String documentId);

    long countByDocumentIdAndDelFlag(String documentId, String delFlag);

    long countByKnowledgeBaseId(String knowledgeBaseId);

    List<DocumentChunk> findByEmbedded(Boolean embedded);

    void deleteByDocumentId(String documentId);
}
