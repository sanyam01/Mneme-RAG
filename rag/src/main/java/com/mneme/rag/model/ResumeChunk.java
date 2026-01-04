package com.mneme.rag.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
