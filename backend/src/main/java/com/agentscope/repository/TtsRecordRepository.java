package com.agentscope.repository;

import com.agentscope.model.entity.TtsRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 语音合成记录数据访问层
 */
@Repository
public interface TtsRecordRepository extends MongoRepository<TtsRecord, String> {
}
