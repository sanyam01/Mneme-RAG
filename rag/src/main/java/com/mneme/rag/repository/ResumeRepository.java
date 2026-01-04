package com.mneme.rag.repository;

import com.mneme.rag.model.ResumeChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<ResumeChunk, Long> {

  /**
   * Similarity Search using Euclidean distance (<->). 1. We take the query embedding from the user.
   * 2. We compare it against all stored embeddings in the table. 3. We sort by the smallest
   * distance (highest similarity). 4. We limit to the top 'limit' results to avoid overwhelming the
   * AI.
   */
  @Query(
      value =
          "SELECT * FROM resume_chunks "
              + "ORDER BY embedding <-> CAST(:queryEmbedding AS vector) "
              + "LIMIT :limit",
      nativeQuery = true)
  List<ResumeChunk> findSimilarChunks(
      @Param("queryEmbedding") float[] queryEmbedding, @Param("limit") int limit);
}
