package com.mneme.rag.service;

import com.mneme.rag.repository.ResumeRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ResumeRepository repository;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;

    /**
     * Conducts a RAG-based chat by:
     * 1. Vectorizing the user's question.
     * 2. Retrieving only the text content (not vectors) from the database.
     * 3. Constructing a prompt with that context for the LLM.
     */
    public String chat(String userQuestion) {
        // 1. Convert user question to a vector
        // This uses your nomic-embed-text model
        float[] queryVector = embeddingModel.embed(userQuestion).content().vector();

        // 2. Retrieve top 5 most relevant TEXT CHUNKS only (Safe Method)
        // We use findSimilarContent because it returns List<String>.
        // This avoids fetching the 'vector' column which causes the Hibernate mapping crash.
        List<String> topChunksContent = repository.findSimilarContent(queryVector, 5);

        // 3. Handle case where no context is found
        if (topChunksContent.isEmpty()) {
            return "I couldn't find any information in the resumes related to your question.";
        }

        // 4. Create the Context String by joining the retrieved text blocks
        String context = String.join("\n\n---\n\n", topChunksContent);

        // 5. Build the Prompt for Llama 3.2
        String prompt = String.format(
            """
            You are a helpful assistant specialized in analyzing resumes. 
            Use the following context provided from candidate resumes to answer the question. 
            If the context doesn't contain the answer, say that you don't know based on the documents provided.

            CONTEXT FROM RESUMES:
            %s

            USER QUESTION:
            %s

            ANSWER:
            """,
            context, userQuestion);

        // 6. Generate the grounded response from the LLM
        return chatModel.generate(prompt);
    }
}