package com.mneme.rag.repository;

import com.mneme.rag.model.ResumeChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ResumeRepository extends JpaRepository<ResumeChunk, Long> {

    /**
     * NATIVE INSERT: Bypasses Hibernate's bytea mapping by casting the float[] 
     * directly to the PostgreSQL vector type.
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO resume_chunks (content, file_name, embedding) " +
                   "VALUES (:content, :fileName, CAST(:embedding AS vector))", 
           nativeQuery = true)
    void insertChunk(@Param("content") String content, 
                     @Param("fileName") String fileName, 
                     @Param("embedding") float[] embedding);

    /**
     * NATIVE SEARCH: Returns full objects. 
     * Note: This might still trigger a 'No results' error on the 'embedding' field 
     * if the Hibernate mapping isn't perfect.
     */
    @Query(value = "SELECT * FROM resume_chunks " +
                   "ORDER BY embedding <-> CAST(:queryEmbedding AS vector) " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<ResumeChunk> findSimilarChunks(@Param("queryEmbedding") float[] queryEmbedding, 
                                        @Param("limit") int limit);

    /**
     * SAFER SEARCH: Returns only the text content.
     * This is the recommended way for RAG because it avoids mapping the vector 
     * back into Java objects entirely.
     */
    @Query(value = "SELECT content FROM resume_chunks " +
                   "ORDER BY embedding <-> CAST(:queryEmbedding AS vector) " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<String> findSimilarContent(@Param("queryEmbedding") float[] queryEmbedding, 
                                    @Param("limit") int limit);
}