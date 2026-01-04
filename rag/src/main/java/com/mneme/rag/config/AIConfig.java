package com.mneme.rag.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

  @Value("${rag.ollama.base-url}")
  private String ollamaBaseUrl;

  // This creates the "Speaker" (Llama 3.2)
  @Bean
  public ChatLanguageModel chatLanguageModel() {
    return OllamaChatModel.builder()
        .baseUrl(ollamaBaseUrl)
        .modelName("llama3.2") // Hardcoding for now, we can move to YAML later
        .timeout(Duration.ofSeconds(60))
        .build();
  }

  // This creates the "Embedder" (Nomic)
  @Bean
  public EmbeddingModel embeddingModel() {
    return OllamaEmbeddingModel.builder()
        .baseUrl(ollamaBaseUrl)
        .modelName("nomic-embed-text") // This is the model that generates the float[]
        .timeout(Duration.ofSeconds(60))
        .build();
  }
}
