package com.agentscope.repository;

import com.agentscope.model.entity.AsrRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * 语音识别记录数据访问层
 */
@Repository
public interface AsrRecordRepository extends MongoRepository<AsrRecord, String> {
}
