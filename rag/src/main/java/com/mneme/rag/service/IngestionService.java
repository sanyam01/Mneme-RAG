package com.mneme.rag.service;

import com.mneme.rag.model.ResumeChunk;
import com.mneme.rag.repository.ResumeRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class IngestionService {

  private final ResumeRepository repository;
  private final EmbeddingModel embeddingModel;

  public void ingest(MultipartFile file) throws IOException {
    // 1. Save MultipartFile to a temporary location for Tika to read
    Path tempFile = Files.createTempFile("upload-", file.getOriginalFilename());
    file.transferTo(tempFile);

    try {
      // 2. Parse PDF to Text using Apache Tika
      Document document =
          FileSystemDocumentLoader.loadDocument(tempFile, new ApacheTikaDocumentParser());

          // DEBUG 1: Check if "Spring Boot" exists in the raw text at all
        String rawText = document.text();
        System.out.println("--- RAW PDF TEXT START ---");
        System.out.println(rawText);
        System.out.println("--- RAW PDF TEXT END ---");

        if (!rawText.toLowerCase().contains("springboot")) {
            System.err.println("CRITICAL: 'Spring Boot' not found in raw PDF extraction!");
        }

      // 3. Split Text into manageable Chunks (300 chars with 30 char overlap)
      var splitter = DocumentSplitters.recursive(1000, 30);
      List<TextSegment> segments = splitter.split(document);

      // 4. For each chunk: Embed it and Save to Postgres
      for (TextSegment segment : segments) {
        // Call Ollama (nomic-embed-text) to get the vector
        float[] vector = embeddingModel.embed(segment.text()).content().vector();

        // Build our Entity and Save
        ResumeChunk chunk =
            ResumeChunk.builder()
                .content(segment.text())
                .fileName(file.getOriginalFilename())
                .embedding(vector)
                .build();

        repository.insertChunk(
    segment.text(), 
    file.getOriginalFilename(), 
    vector
);
      }
    } finally {
      Files.deleteIfExists(tempFile); // Clean up temp file
    }
  }
}
