package com.agentscope.service.impl;

import com.agentscope.service.MilvusService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.GetCollectionStatsReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.response.GetCollectionStatsResp;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Milvus服务实现类
 * <p>
 * 使用Milvus SDK 3.0.5 v2 API实现向量数据库操作
 * </p>
 */
@Slf4j
@Service
public class MilvusServiceImpl implements MilvusService {

    private final MilvusClientV2 milvusClient;
    private final Gson gson = new Gson();

    public MilvusServiceImpl(@Lazy MilvusClientV2 milvusClient) {
        this.milvusClient = milvusClient;
    }

    /** 主键字段名 */
    private static final String FIELD_ID = "id";
    /** 知识库ID字段名 */
    private static final String FIELD_KB_ID = "knowledge_base_id";
    /** 文档ID字段名 */
    private static final String FIELD_DOC_ID = "document_id";
    /** 分块ID字段名 */
    private static final String FIELD_CHUNK_ID = "chunk_id";
    /** 文本内容字段名 */
    private static final String FIELD_CONTENT = "content";
    /** 向量字段名 */
    private static final String FIELD_VECTOR = "vector";

    /**
     * 创建Collection
     */
    @Override
    public void createCollection(String collectionName, int dimension) {
        try {
            if (hasCollection(collectionName)) {
                log.info("Collection已存在，跳过创建: {}", collectionName);
                return;
            }

            // 创建Schema
            CreateCollectionReq.CollectionSchema schema = milvusClient.createSchema();

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_ID)
                    .dataType(DataType.Int64)
                    .isPrimaryKey(true)
                    .autoID(true)
                    .description("主键ID")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_KB_ID)
                    .dataType(DataType.VarChar)
                    .maxLength(64)
                    .description("知识库ID")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_DOC_ID)
                    .dataType(DataType.VarChar)
                    .maxLength(64)
                    .description("文档ID")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_CHUNK_ID)
                    .dataType(DataType.VarChar)
                    .maxLength(64)
                    .description("分块ID")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_CONTENT)
                    .dataType(DataType.VarChar)
                    .maxLength(65535)
                    .description("文本内容")
                    .build());

            schema.addField(AddFieldReq.builder()
                    .fieldName(FIELD_VECTOR)
                    .dataType(DataType.FloatVector)
                    .dimension(dimension)
                    .description("向量")
                    .build());

            // 创建索引
            IndexParam indexParam = IndexParam.builder()
                    .fieldName(FIELD_VECTOR)
                    .metricType(IndexParam.MetricType.COSINE)
                    .build();

            // 创建Collection
            CreateCollectionReq createRequest = CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(schema)
                    .indexParams(Collections.singletonList(indexParam))
                    .build();

            milvusClient.createCollection(createRequest);
            log.info("Milvus Collection创建成功: {}, 维度: {}", collectionName, dimension);

        } catch (Exception e) {
            log.error("创建Milvus Collection失败: {}", collectionName, e);
            throw new RuntimeException("创建Milvus Collection失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除Collection
     */
    @Override
    public void dropCollection(String collectionName) {
        try {
            if (!hasCollection(collectionName)) {
                log.info("Collection不存在，跳过删除: {}", collectionName);
                return;
            }

            milvusClient.dropCollection(DropCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());

            log.info("Milvus Collection删除成功: {}", collectionName);

        } catch (Exception e) {
            log.error("删除Milvus Collection失败: {}", collectionName, e);
            throw new RuntimeException("删除Milvus Collection失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查Collection是否存在
     */
    @Override
    public boolean hasCollection(String collectionName) {
        try {
            Boolean exists = milvusClient.hasCollection(HasCollectionReq.builder()
                    .collectionName(collectionName)
                    .build());
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("检查Collection是否存在失败: {}", collectionName, e);
            return false;
        }
    }

    /**
     * 获取Collection中的向量数量
     */
    @Override
    public long getVectorCount(String collectionName) {
        try {
            if (!hasCollection(collectionName)) {
                return 0;
            }

            GetCollectionStatsResp stats = milvusClient.getCollectionStats(
                    GetCollectionStatsReq.builder()
                            .collectionName(collectionName)
                            .build());

            String rowCountStr = stats.getStats().get("row_count");
            return rowCountStr != null ? Long.parseLong(rowCountStr) : 0;

        } catch (Exception e) {
            log.error("获取向量数量失败: {}", collectionName, e);
            return 0;
        }
    }

    /**
     * 插入向量数据
     */
    @Override
    public void insertVectors(String collectionName, String knowledgeBaseId, String documentId,
                              List<String> contents, List<float[]> embeddings) {
        insertVectors(collectionName, knowledgeBaseId, documentId, null, contents, embeddings);
    }

    /**
     * 插入向量数据（带分块ID）
     */
    public void insertVectors(String collectionName, String knowledgeBaseId, String documentId,
                              String chunkId, List<String> contents, List<float[]> embeddings) {
        try {
            if (contents == null || contents.isEmpty()) {
                log.warn("插入向量数据为空，跳过: collection={}", collectionName);
                return;
            }

            if (contents.size() != embeddings.size()) {
                throw new IllegalArgumentException("文本内容和向量数量不匹配");
            }

            // 构建插入数据
            List<JsonObject> rows = new ArrayList<>();
            for (int i = 0; i < contents.size(); i++) {
                JsonObject row = new JsonObject();
                row.addProperty(FIELD_KB_ID, knowledgeBaseId);
                row.addProperty(FIELD_DOC_ID, documentId);
                row.addProperty(FIELD_CHUNK_ID, chunkId != null ? chunkId : "");
                row.addProperty(FIELD_CONTENT, contents.get(i));
                row.add(FIELD_VECTOR, gson.toJsonTree(floatArrayToList(embeddings.get(i))));
                rows.add(row);
            }

            // 执行插入
            milvusClient.insert(InsertReq.builder()
                    .collectionName(collectionName)
                    .data(rows)
                    .build());

            log.info("向量数据插入成功: collection={}, 知识库={}, 文档={}, 数量={}",
                    collectionName, knowledgeBaseId, documentId, contents.size());

        } catch (Exception e) {
            log.error("插入向量数据失败: collection={}", collectionName, e);
            throw new RuntimeException("插入向量数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 相似性搜索
     */
    @Override
    public List<Map<String, Object>> search(String collectionName, float[] queryVector, int topK, String filter) {
        try {
            if (!hasCollection(collectionName)) {
                log.warn("Collection不存在，跳过搜索: {}", collectionName);
                return Collections.emptyList();
            }

            var searchBuilder = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(Collections.singletonList(new FloatVec(queryVector)))
                    .annsField(FIELD_VECTOR)
                    .limit(topK)
                    .outputFields(List.of(FIELD_KB_ID, FIELD_DOC_ID, FIELD_CHUNK_ID, FIELD_CONTENT));

            if (filter != null && !filter.isBlank()) {
                searchBuilder.filter(filter);
            }

            SearchResp response = milvusClient.search(searchBuilder.build());

            // 解析搜索结果
            List<Map<String, Object>> results = new ArrayList<>();
            if (response.getSearchResults() != null && !response.getSearchResults().isEmpty()) {
                List<SearchResp.SearchResult> searchResults = response.getSearchResults().get(0);
                for (SearchResp.SearchResult result : searchResults) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", result.getId());
                    item.put("score", result.getScore());
                    item.put("knowledge_base_id", result.getEntity().get(FIELD_KB_ID));
                    item.put("document_id", result.getEntity().get(FIELD_DOC_ID));
                    item.put("chunk_id", result.getEntity().get(FIELD_CHUNK_ID));
                    item.put("content", result.getEntity().get(FIELD_CONTENT));
                    results.add(item);
                }
            }

            log.info("向量搜索完成: collection={}, 结果数={}", collectionName, results.size());
            return results;

        } catch (Exception e) {
            log.error("向量搜索失败: collection={}", collectionName, e);
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按知识库ID删除所有向量
     */
    @Override
    public void deleteByKnowledgeBaseId(String collectionName, String knowledgeBaseId) {
        try {
            if (!hasCollection(collectionName)) {
                log.info("Collection不存在，跳过删除: {}", collectionName);
                return;
            }

            String filter = FIELD_KB_ID + " = \"" + knowledgeBaseId + "\"";
            milvusClient.delete(DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter(filter)
                    .build());

            log.info("按知识库ID删除向量成功: collection={}, knowledgeBaseId={}", collectionName, knowledgeBaseId);

        } catch (Exception e) {
            log.error("按知识库ID删除向量失败: collection={}", collectionName, e);
            throw new RuntimeException("按知识库ID删除向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按文档ID删除所有向量
     */
    @Override
    public void deleteByDocumentId(String collectionName, String documentId) {
        try {
            if (!hasCollection(collectionName)) {
                log.info("Collection不存在，跳过删除: {}", collectionName);
                return;
            }

            String filter = FIELD_DOC_ID + " == '" + documentId + "'";
            milvusClient.delete(DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter(filter)
                    .build());

            log.info("按文档ID删除向量成功: collection={}, documentId={}", collectionName, documentId);

        } catch (Exception e) {
            log.error("按文档ID删除向量失败: collection={}", collectionName, e);
            throw new RuntimeException("按文档ID删除向量失败: " + e.getMessage(), e);
        }
    }

    /**
     * float数组转List<Float>
     */
    private List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    /**
     * 按分块ID列表删除向量
     */
    @Override
    public void deleteByChunkIds(String collectionName, List<String> chunkIds) {
        try {
            if (!hasCollection(collectionName)) {
                log.info("Collection不存在，跳过删除: {}", collectionName);
                return;
            }
            if (chunkIds == null || chunkIds.isEmpty()) return;

            StringJoiner joiner = new StringJoiner(", ");
            for (String id : chunkIds) {
                joiner.add("\"" + id + "\"");
            }
            String filter = FIELD_CHUNK_ID + " in [" + joiner + "]";

            milvusClient.delete(DeleteReq.builder()
                    .collectionName(collectionName)
                    .filter(filter)
                    .build());

            log.info("按分块ID删除向量成功: collection={}, chunkIds={}", collectionName, chunkIds.size());

        } catch (Exception e) {
            log.error("按分块ID删除向量失败: collection={}", collectionName, e);
            throw new RuntimeException("按分块ID删除向量失败: " + e.getMessage(), e);
        }
    }
}
