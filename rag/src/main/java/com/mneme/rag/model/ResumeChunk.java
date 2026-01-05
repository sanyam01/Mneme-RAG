package com.mneme.rag.model;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resume_chunks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeChunk {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "TEXT")
  private String content;

  private String fileName;

  @Column(name = "embedding", columnDefinition = "vector(768)")
  private float[] embedding;
}
