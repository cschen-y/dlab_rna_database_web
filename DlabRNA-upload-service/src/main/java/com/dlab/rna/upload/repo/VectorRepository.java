package com.dlab.rna.upload.repo;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@Slf4j
public class VectorRepository {
    private final JdbcTemplate jdbc;

    public VectorRepository(JdbcTemplate vectorJdbcTemplate) {
        this.jdbc = vectorJdbcTemplate;
    }

    public void upsertChunk(String fileId, String objectType, String chunkId,
                            String content, int startPos, int chunkSize, String sha256) {

        String sql = """
        insert into kb_chunks(
            file_id, object_type, chunk_id, content, start_pos, chunk_size, sha256, created_at
        ) values(?,?,?,?,?,?,?,?)
        on conflict(file_id, object_type, chunk_id)
        do update set
            content   = excluded.content,
            start_pos = excluded.start_pos,
            chunk_size = excluded.chunk_size,
            sha256    = excluded.sha256
        """;

        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fileId);
            ps.setString(2, objectType);
            ps.setString(3, chunkId);
            ps.setString(4, content);
            ps.setInt(5, startPos);
            ps.setInt(6, chunkSize);
            ps.setString(7, sha256);
            ps.setTimestamp(8, Timestamp.from(Instant.now()));
            return ps;
        });
    }


    public void upsertVector(String fileId, String object, String chunkId, List<Double> embedding, String model, int dim) throws Exception {
        String sql = "insert into kb_vectors(file_id, object, chunk_id, embedding, model, dim, created_at) values(?,?,?,?,?,?,?) on conflict(file_id, object, chunk_id) do update set embedding=excluded.embedding, model=excluded.model, dim=excluded.dim";
        PGobject vector = new PGobject();
        vector.setType("vector");
        vector.setValue(toVectorString(embedding));
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fileId);
            ps.setString(2, object);
            ps.setString(3, chunkId);
            ps.setObject(4, vector);
            ps.setString(5, model);
            ps.setInt(6, dim);
            ps.setTimestamp(7, Timestamp.from(Instant.now()));
            return ps;
        });
    }

    public String getChunkContent(String fileId, String objectType, String chunkId) {
        String sql = "select content from kb_chunks where file_id=? and object_type=? and chunk_id=?";
        return jdbc.queryForObject(sql, new Object[]{fileId, objectType, chunkId}, String.class);
    }

    private String toVectorString(List<Double> embedding) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding.get(i));
        }
        sb.append(']');
        return sb.toString();
    }
}