package com.recallr.service.rag;

import com.recallr.dto.RagSearchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RagSearchService {

    private final JdbcTemplate jdbcTemplate;

    public RagSearchService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RagSearchResult> search(Long userId, float[] queryEmbedding, int topK) {
        String vectorString = Arrays.toString(queryEmbedding);

        String sql = """
                SELECT cc.content_id,
                       c.title,
                       c.url,
                       cc.chunk_index,
                       cc.text,
                       cc.embedding <=> CAST(? AS vector) AS distance
                FROM content_chunks cc
                JOIN contents c ON c.id = cc.content_id
                WHERE cc.user_id = ?
                ORDER BY cc.embedding <=> CAST(? AS vector)
                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new RagSearchResult(
                        rs.getLong("content_id"),
                        rs.getString("title"),
                        rs.getString("url"),
                        rs.getInt("chunk_index"),
                        rs.getString("text"),
                        rs.getDouble("distance")
                ),
                vectorString,
                userId,
                vectorString,
                topK
        );
    }
}