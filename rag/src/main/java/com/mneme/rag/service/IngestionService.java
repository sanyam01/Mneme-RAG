package com.mneme.rag.service;

import com.mneme.rag.repository.ResumeRepository;
import com.mneme.rag.model.ResumeChunk;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import java.io.IOException;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.List;
import java.nio.file.Files;


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
            Document document = FileSystemDocumentLoader.loadDocument(tempFile, new ApacheTikaDocumentParser());

            // 3. Split Text into manageable Chunks (300 chars with 30 char overlap)
            var splitter = DocumentSplitters.recursive(300, 30);
            List<TextSegment> segments = splitter.split(document);

            // 4. For each chunk: Embed it and Save to Postgres
            for (TextSegment segment : segments) {
                // Call Ollama (nomic-embed-text) to get the vector
                float[] vector = embeddingModel.embed(segment.text()).content().vector();

                // Build our Entity and Save
                ResumeChunk chunk = ResumeChunk.builder()
                        .content(segment.text())
                        .fileName(file.getOriginalFilename())
                        .embedding(vector)
                        .build();

                repository.save(chunk);
            }
        } finally {
            Files.deleteIfExists(tempFile); // Clean up temp file
        }
    }
}
