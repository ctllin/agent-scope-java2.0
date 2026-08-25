package com.agentscope.repository;

import com.agentscope.model.entity.XFileRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * x-file-storage 文件登记数据访问层
 */
@Repository
public interface XFileRecordRepository extends MongoRepository<XFileRecord, String> {

    Optional<XFileRecord> findFirstByFilenameOrderByIdDesc(String filename);

    Optional<XFileRecord> findFirstByUrlOrderByIdDesc(String url);

    List<XFileRecord> findAllByStorageKey(String storageKey);

    List<XFileRecord> findAllByStorageKeyStartingWith(String prefix);

    void deleteAllByStorageKey(String storageKey);
}
