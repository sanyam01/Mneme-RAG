package com.mneme.rag.service;

import com.mneme.rag.model.ResumeChunk;
import com.mneme.rag.repository.ResumeRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ResumeRepository repository;
  private final EmbeddingModel embeddingModel;
  private final ChatLanguageModel chatModel;

  public String chat(String userQuestion) {
    // 1. Convert user question to a vector
    float[] queryVector = embeddingModel.embed(userQuestion).content().vector();

    // 2. Retrieve top 5 most relevant chunks from Postgres
    List<ResumeChunk> topChunks = repository.findSimilarChunks(queryVector, 5);

    // 3. Create the Context String
    String context =
        topChunks.stream().map(ResumeChunk::getContent).collect(Collectors.joining("\n\n---\n\n"));

    // 4. Build the Prompt
    String prompt =
        String.format(
            """
            You are a helpful assistant specialized in analyzing resumes.
            Use the following context provided from candidate resumes to answer the question.
            If the context doesn't contain the answer, say that you don't know based on the documents provided.

            CONTEXT:
            %s

            USER QUESTION:
            %s

            ANSWER:
            """,
            context, userQuestion);

    // 5. Generate response from Llama 3.2
    return chatModel.generate(prompt);
  }
}
