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