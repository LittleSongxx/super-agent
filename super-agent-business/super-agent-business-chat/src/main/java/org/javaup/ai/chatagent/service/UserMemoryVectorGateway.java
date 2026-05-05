package org.javaup.ai.chatagent.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.javaup.ai.chatagent.data.SuperAgentUserMemory;
import org.javaup.ai.chatagent.model.memory.UserMemoryItem;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UserMemoryVectorGateway {

    private static final String TABLE_NAME = "public.super_agent_user_memory_embedding";

    private final ObjectProvider<JdbcTemplate> pgVectorJdbcTemplateProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.embedding.options.model:}")
    private String embeddingModelName;

    public UserMemoryVectorGateway(@Qualifier("documentManagePgVectorJdbcTemplate") ObjectProvider<JdbcTemplate> pgVectorJdbcTemplateProvider,
                                   ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                   ObjectMapper objectMapper) {
        this.pgVectorJdbcTemplateProvider = pgVectorJdbcTemplateProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.objectMapper = objectMapper;
    }

    public void upsertMemory(SuperAgentUserMemory memory) {
        if (memory == null || memory.getId() == null || StrUtil.isBlank(memory.getMemoryText())) {
            return;
        }
        JdbcTemplate jdbcTemplate = pgVectorJdbcTemplateProvider.getIfAvailable();
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (jdbcTemplate == null || embeddingModel == null) {
            return;
        }
        float[] embedding = embeddingModel.embed(memory.getMemoryText());
        String sql = """
            INSERT INTO %s
            (memory_id, tenant_id, user_id, memory_type, memory_key, memory_text, source_conversation_id,
             source_exchange_id, importance, confidence, embedding_model, metadata_json, embedding, create_time, edit_time, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS vector), NOW(), NOW(), ?)
            ON CONFLICT (memory_id) DO UPDATE SET
                tenant_id = EXCLUDED.tenant_id,
                user_id = EXCLUDED.user_id,
                memory_type = EXCLUDED.memory_type,
                memory_key = EXCLUDED.memory_key,
                memory_text = EXCLUDED.memory_text,
                source_conversation_id = EXCLUDED.source_conversation_id,
                source_exchange_id = EXCLUDED.source_exchange_id,
                importance = EXCLUDED.importance,
                confidence = EXCLUDED.confidence,
                embedding_model = EXCLUDED.embedding_model,
                metadata_json = EXCLUDED.metadata_json,
                embedding = EXCLUDED.embedding,
                edit_time = NOW(),
                status = EXCLUDED.status
            """.formatted(TABLE_NAME);
        jdbcTemplate.update(sql,
            memory.getId(),
            memory.getTenantId(),
            memory.getUserId(),
            memory.getMemoryType(),
            memory.getMemoryKey(),
            memory.getMemoryText(),
            memory.getSourceConversationId(),
            memory.getSourceExchangeId(),
            defaultInteger(memory.getImportance()),
            defaultBigDecimal(memory.getConfidence()),
            resolveEmbeddingModelName(),
            buildMetadataJson(memory),
            toVectorLiteral(embedding),
            1
        );
    }

    public List<UserMemoryItem> recall(String tenantId, String userId, String query, int topK) {
        JdbcTemplate jdbcTemplate = pgVectorJdbcTemplateProvider.getIfAvailable();
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (jdbcTemplate == null || embeddingModel == null || StrUtil.isBlank(tenantId) || StrUtil.isBlank(userId) || StrUtil.isBlank(query) || topK <= 0) {
            return List.of();
        }
        try {
            String vectorLiteral = toVectorLiteral(embeddingModel.embed(query));
            String sql = """
                SELECT memory_id, memory_type, memory_key, memory_text, source_conversation_id, source_exchange_id,
                       importance, confidence, 1 - (embedding <=> CAST(? AS vector)) AS similarity
                FROM %s
                WHERE tenant_id = ? AND user_id = ? AND status = 1
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """.formatted(TABLE_NAME);
            return jdbcTemplate.query(sql, (rs, rowNum) -> UserMemoryItem.builder()
                .memoryId(rs.getLong("memory_id"))
                .memoryType(rs.getString("memory_type"))
                .memoryKey(rs.getString("memory_key"))
                .memoryText(rs.getString("memory_text"))
                .sourceConversationId(rs.getString("source_conversation_id"))
                .sourceExchangeId((Long) rs.getObject("source_exchange_id"))
                .importance(rs.getInt("importance"))
                .confidence(rs.getBigDecimal("confidence"))
                .similarity(rs.getDouble("similarity"))
                .build(), vectorLiteral, tenantId, userId, vectorLiteral, topK);
        }
        catch (RuntimeException exception) {
            log.warn("用户记忆向量召回失败, tenantId={}, userId={}: {}", tenantId, userId, exception.getMessage());
            return List.of();
        }
    }

    private String buildMetadataJson(SuperAgentUserMemory memory) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tenantId", memory.getTenantId());
        metadata.put("userId", memory.getUserId());
        metadata.put("memoryType", memory.getMemoryType());
        metadata.put("memoryKey", memory.getMemoryKey());
        metadata.put("sourceConversationId", memory.getSourceConversationId());
        metadata.put("sourceExchangeId", memory.getSourceExchangeId());
        try {
            return objectMapper.writeValueAsString(metadata);
        }
        catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化用户记忆 metadata 失败。", exception);
        }
    }

    private String toVectorLiteral(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalStateException("EmbeddingModel 返回了空向量。");
        }
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append(embedding[index]);
        }
        builder.append("]");
        return builder.toString();
    }

    private String resolveEmbeddingModelName() {
        return StrUtil.isNotBlank(embeddingModelName) ? embeddingModelName : "default";
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
