# Project Requirements: Mneme-RAG
## Production-Grade Multi-Tenant RAG System

### 1. Project Overview
A retrieval-augmented generation (RAG) platform. The system enables multiple users to upload private documents and query them via an LLM, ensuring strict data isolation, real-time streaming, and automated infrastructure.

---

### 2. Functional Requirements

#### 2.1 Multi-Tenant Ingestion (The "Write" Path)
- **Document Support:** Support for `.pdf` file formats.
- **Tenant Isolation:** Every document must be associated with a `tenant_id`.
- **Text Processing:**
    - Clean and normalize extracted text.
    - Implement a "Recursive Character Splitter" (500-800 character chunks).
    - Maintain a 10-15% overlap between chunks to prevent context loss.
- **Vectorization:** Generate embeddings using a standardized model (e.g., `all-MiniLM-L6-v2`).
- **Persistence:** Store chunks and vectors in PostgreSQL using the `pgvector` extension.

#### 2.2 Conversational Retrieval (The "Read" Path)
- **Semantic Search:** Convert user queries into vectors and perform cosine similarity searches.
- **Metadata Filtering:** **MUST** filter all database queries by `tenant_id` at the database level to prevent cross-user data leakage.
- **Context Augmentation:** Retrieve the Top-K (3-5) most relevant chunks to feed into the LLM prompt.
- **Persistent Chat Memory:** - Store chat history in Postgres.
    - Provide the LLM with a "rolling window" of the last 10 messages for conversational context.

#### 2.3 Generation & UX
- **Grounded Responses:** Use System Prompts to force the LLM to answer only based on provided context (preventing hallucinations).
- **Streaming UI:** Implement Server-Sent Events (SSE) to stream text tokens from the Java backend to the React frontend.

---

### 3. Technical & Non-Functional Requirements

#### 3.1 Backend (Java/Spring Boot)
- **Concurrency:** Use **Java 21 Virtual Threads (Project Loom)** to handle high-volume streaming connections efficiently.
- **Abstraction:** Use **LangChain4j** to ensure the LLM provider (Ollama vs. vLLM) is interchangeable via configuration.
- **Security:** Implement a simulated or real JWT-based authentication filter to extract `tenant_id` from request headers.

#### 3.2 AI Engine (vLLM / Ollama)
- **API Compatibility:** Must use an OpenAI-compatible REST API.
- **Local Inference:** Host the LLM (e.g., Qwen 2.5 or Llama 3) locally to ensure data privacy and zero API costs.

#### 3.3 Infrastructure (Docker)
- **Orchestration:** Use `docker-compose` to manage Backend, Frontend, Postgres, and the AI Engine.
- **One-Click Setup:** Implement an **Init-Container** pattern to automatically pull LLM model weights on the first run.
- **Database Optimization:** Enable **HNSW (Hierarchical Navigable Small World)** indexing on vector columns for high-performance retrieval.

#### 3.4 CI/CD (GitHub Actions)
- **Automated Testing:** Run unit tests for chunking logic and integration tests for pgvector using **Testcontainers**.
- **Multi-Stage Builds:** Dockerfiles must separate the build environment from the runtime environment to keep image sizes < 300MB.
- **Quality Gates:** Implement linting and security scans for both Java and React code.

---

### 4. System Constraints
- **Latency:** Semantic search + Context prep must take < 300ms.
- **Accuracy:** The system must cite the source filename for every generated answer.
- **Reliability:** Postgres must serve as the single source of truth for both Vectors and Chat History.

---

### 5. Tech Stack Summary
- **Frontend:** React, Tailwind CSS, Lucide Icons.
- **Backend:** Java 21, Spring Boot 3.x, LangChain4j.
- **Database:** PostgreSQL + pgvector.
- **Inference:** Ollama or vLLM (Qwen 2.5 7B model).
- **DevOps:** Docker, GitHub Actions, Maven.

## 🏗 System Architecture

The project implements a decoupled, three-tier architecture to ensure clear separation of concerns and robust data boundaries.

### 1. The Client Layer (React Frontend)
A lightweight frontend focusing on **Interaction and Presentation**:
* **Multi-Format Ingestion**: Supports uploading `.pdf` and `.md` files for ingestion.
* **JWT-Based Security**: Automatically attaches an authentication token (containing the `tenant_id`) to every request header to enforce security at the gateway.
* **Real-time UX**: Implements a **Server-Sent Events (SSE)** listener to render incoming LLM tokens instantly, providing a fluid chat experience.
* **Thin State**: Maintains only the current UI state; all long-term conversational history is managed by the backend to ensure consistency across sessions.

### 2. The Service Layer (Spring Boot Orchestrator)
The core logic center where **Contextual Orchestration** and **State Management** reside:
* **Security Filter**: Extracts the `tenant_id` from secure JWTs to provide row-level isolation in all database transactions.
* **Persistent Chat Memory**: Retrieves the last 10 messages from **Postgres** based on `chat_id` and `tenant_id` to maintain context during the conversation.
* **Retrieval Service**: Vectorizes user queries and performs semantic searches on the **pgvector** store, utilizing strict metadata filtering (`WHERE tenant_id = ?`).
* **Prompt Augmentation**: Merges retrieved document context, conversation history, and the latest user question into a single, grounded prompt for the LLM.

### 3. The Inference & Data Layer (Backend Services)
* **Postgres + pgvector**: A unified source of truth for persistent document vectors, associated metadata, and chat history.
* **Local Inference (Qwen 2.5)**: Uses **Ollama** or **vLLM** to generate responses locally, ensuring high privacy and zero external API costs.

---

## 🔄 Detailed Data Flow (The "Read" Path)



1.  **Request & Auth**: The user sends a chat message. The system pulls the `tenant_id` from a secure JWT to verify identity.
2.  **Semantic Retrieval**: Spring Boot converts the query into a vector and searches **pgvector** for the most relevant private documents.
3.  **Memory Load**: The system fetches the rolling conversation window from the `Chat History` table.
4.  **Generation**: The **Qwen 2.5** model processes the augmented prompt (Context + History + Question).
5.  **SSE Streaming**: Each generated token is streamed back to the React UI in real-time.