package com.mneme.rag.controller;

import com.mneme.rag.service.ChatService;
import com.mneme.rag.service.IngestionService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class RAGController {

  private final IngestionService ingestionService;
  private final ChatService chatService;

  @PostMapping("/upload")
  public ResponseEntity<String> uploadResume(@RequestParam("file") MultipartFile file) {
    try {
      ingestionService.ingest(file);
      return ResponseEntity.ok(
          "Resume ingested and vectorized successfully: " + file.getOriginalFilename());
    } catch (IOException e) {
      return ResponseEntity.internalServerError()
          .body("Failed to process resume: " + e.getMessage());
    }
  }

  // Endpoint to ask a question
  @GetMapping("/chat")
  public ResponseEntity<String> chat(@RequestParam("question") String question) {
    String answer = chatService.chat(question);
    return ResponseEntity.ok(answer);
  }
}
