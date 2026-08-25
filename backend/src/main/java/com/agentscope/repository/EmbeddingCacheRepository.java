package com.agentscope.repository;

import com.agentscope.model.entity.EmbeddingCache;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmbeddingCacheRepository extends MongoRepository<EmbeddingCache, String> {

    Optional<EmbeddingCache> findByTextHash(String textHash);
}
