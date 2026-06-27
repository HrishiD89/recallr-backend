# Recallr Backend

## Overview
Spring Boot 3.3.0 / Java 21 backend for a bookmarking app with AI-powered RAG search. Users save URLs, content is extracted and embedded (768d via Gemini), and they can ask natural-language questions against their bookmarks.

## Project Structure

```
com.recallr
├── RecallrBackendApplication.java    # @SpringBootApplication + @EnableAsync
├── GlobalExceptionHandler.java       # @RestControllerAdvice
├── client/
│   ├── GeminiApiClient.java          # Interface: embedText(), generateAnswer()
│   ├── GeminiClient.java             # WebClient-based implementation
│   └── dto/                          # Request/response records for Gemini API
├── config/
│   ├── GeminiProperties.java         # @ConfigurationProperties(prefix="gemini")
│   ├── RestClientConfig.java         # RestTemplate bean
│   ├── RsaKeyProperties.java         # record binding rsa.public-key / rsa.private-key
│   ├── SecurityConfig.java           # SecurityFilterChain + AuthenticationManager
│   ├── TsidGenerator.java            # Hibernate ID generator (TSID)
│   ├── WebClientConfig.java          # WebClient bean for Gemini
│   └── security/
│       ├── CorsConfig.java           # CORS from cors.allowed-origins
│       ├── CustomUserDetailsService.java  # Loads user by username
│       └── JwtConfig.java            # JwtEncoder, JwtDecoder, PasswordEncoder
├── controller/
│   ├── AuthController.java           # /api/v1 (signup, signin, greeting)
│   ├── ContentController.java        # /api/v1/content (CRUD)
│   ├── RagController.java            # /api/v1/rag/query
│   ├── ShareController.java          # /api/v1/share (generate, view, revoke)
│   └── TestController.java           # /test/** (debug endpoints, no auth)
├── dto/
│   ├── ContentMetadata.java          # type, embedUrl, title, thumbnail, author, description
│   ├── ExtractionResult.java         # text, extractedVia
│   ├── RagSearchResult.java          # contentId, title, url, chunkIndex, text, distance
│   ├── request/
│   │   ├── AuthRequest.java          # username, password (validated)
│   │   ├── ContentRequestDTO.java    # url
│   │   └── RagQueryRequest.java      # query, topK
│   └── response/
│       ├── AuthResponse.java         # token, message
│       ├── ContentResponseDTO.java   # id, url, embedUrl, title, thumbnailUrl, type, processingStatus, read, createdAt, author, description, wordCount
│       ├── RagQueryResponse.java     # answer, sources
│       └── RagSourceDTO.java         # contentId, title, url, chunkIndex, distance
├── events/
│   └── ContentCreatedEvent.java      # Fired after content URL is saved
├── model/
│   ├── Content.java                  # contents table
│   ├── ContentType.java              # YOUTUBE, ARTICLE, OTHER
│   ├── ExtractedDocument.java        # extracted_documents table
│   ├── ProcessingStatus.java         # PENDING, PROCESSING, READY, FAILED
│   ├── Tag.java                      # tags table
│   ├── User.java                     # users table
│   ├── UserQuota.java                # user_quotas table
│   └── UserTier.java                 # FREE, PRO
├── repository/
│   ├── ContentRepository.java
│   ├── ExtractedDocumentRepository.java
│   ├── TagRepository.java
│   ├── UserQuotaRepository.java
│   └── UserRepository.java
└── service/
    ├── content/
    │   ├── ContentManagementService.java   # Orchestrates save/findAll/delete/toggleRead
    │   └── ContentMetadataResolver.java    # URL type detection + metadata scraping
    ├── embedding/
    │   ├── ChunkEmbeddingService.java      # @Async listener: chunks + embeds content
    │   └── TextChunker.java               # 1400-chunk / 200-overlap
    ├── extraction/
    │   ├── TextExtractionService.java      # Routes by ContentType
    │   └── strategy/
    │       ├── ExtractionStrategy.java               # Interface
    │       ├── ArticleExtractionStrategy.java         # Fallback: Jsoup -> Jina -> Tinyfish
    │       ├── JsoupExtractionStrategy.java           # <p> tag scraper
    │       ├── JinaExtractionStrategy.java            # r.jina.ai reader
    │       ├── TinyfishExtractionStrategy.java        # fetch.tinyfish.ai
    │       └── YoutubeTranscriptStrategy.java         # youtube-transcript-api
    ├── quota/
    │   └── QuotaService.java              # Daily limit: 20 bookmarks / 10 RAG queries (FREE)
    └── rag/
        ├── RagQueryService.java           # Full RAG pipeline orchestration
        ├── RagSearchService.java          # pgvector cosine search (<=> operator)
        └── prompt/
            └── RagPromptBuilder.java      # Citation-aware prompt construction
```

## Architecture

### Auth Flow
1. User signs up (POST /api/v1/signup) — password is BCrypt-hashed
2. User signs in (POST /api/v1/signin) — returns signed JWT (RSA-256)
3. Subsequent requests include `Authorization: Bearer <jwt>` — validated by OAuth2 resource server
4. JWT contains `sub` (username), `iat`, `exp` — no custom claims

### Content Pipeline
```
POST /api/v1/content (url)
  └─ ContentManagementService.save()
       ├─ ContentMetadataResolver.resolve(url) → ContentType
       ├─ Save Content entity (status=PENDING)
       └─ Publish ContentCreatedEvent
              └─ @Async @TransactionalEventListener
                   └─ ChunkEmbeddingService.processContent()
                        ├─ TextExtractionService.extract(url, contentType)
                        │    ├─ YOUTUBE → YoutubeTranscriptStrategy
                        │    └─ ARTICLE → ArticleExtractionStrategy
                        │         └─ Chain: Jsoup → Jina → Tinyfish
                        ├─ Save ExtractedDocument
                        ├─ TextChunker.chunk(rawText) → List<String>
                        └─ For each chunk:
                             └─ GeminiClient.embedText(chunk) → float[768]
                                  └─ INSERT INTO content_chunks (..., embedding vector)
```

### RAG Query Pipeline
```
POST /api/v1/rag/query (query, topK)
  └─ RagQueryService.query()
       ├─ GeminiClient.embedText(query) → float[768]
       ├─ RagSearchService.search(userId, embedding, topK) → List<RagSearchResult>
       │    └─ Raw SQL: SELECT ..., embedding <=> ? AS distance
       ├─ Filter: distance <= 0.42
       ├─ RagPromptBuilder.build(query, results) → prompt string
       ├─ GeminiClient.generateAnswer(prompt) → answer
       └─ Parse answer for cited sources → RagQueryResponse
```

### Quota System
- **FREE tier**: 20 bookmark saves/day, 10 RAG queries/day
- **PRO tier**: unlimited
- Quotas reset daily (keyed by `(user_id, quota_date)`)

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/v1/signup | — | Register new user |
| POST | /api/v1/signin | — | Authenticate, get JWT |
| GET | /api/v1/greeting | JWT | Health check |
| POST | /api/v1/content | JWT | Save URL |
| GET | /api/v1/content | JWT | List user's bookmarks |
| DELETE | /api/v1/content/{id} | JWT | Delete bookmark |
| PATCH | /api/v1/content/{id}/read | JWT | Toggle read status |
| POST | /api/v1/rag/query | JWT | Query bookmarks via RAG |
| POST | /api/v1/share/generate | JWT | Generate share token |
| GET | /api/v1/share/{token} | — | View shared bookmarks |
| DELETE | /api/v1/share/revoke | JWT | Revoke share token |

## Key Dependencies

| Dependency | Purpose |
|------------|---------|
| Spring Boot 3.3.0 | Framework |
| spring-security + oauth2-resource-server | JWT auth (RSA-256) |
| spring-boot-starter-webflux | WebClient for Gemini API calls |
| PostgreSQL + pgvector | Storage + vector similarity search |
| Flyway | DB migrations |
| Jsoup | HTML content extraction |
| youtube-transcript-api | YouTube transcript fetching |
| TSID Creator | Time-sorted unique IDs |

## Database (PostgreSQL + pgvector)

### Migrations
| Migration | Changes |
|-----------|---------|
| V1 | users, tags, contents, content_tags, refresh_token + indexes |
| V2 | pgvector extension, content_chunks table (vector(768) + HNSW), processing_status |
| V3 | extracted_documents table |
| V4 | tier column on users |
| V5 | user_quotas table |
| V6 | extracted_via column |
| V7 | author, description, word_count columns |
| V8 | Drop refresh_token table |

### Key Tables
- **users**: id (TSID), username, password (BCrypt), share_token, tier
- **contents**: id, user_id, url, type, processing_status, read, metadata fields
- **content_chunks**: id, content_id, chunk_index, chunk_text, embedding vector(768)
- **extracted_documents**: id, content_id, raw_text, source_kind, extracted_via
- **user_quotas**: id, user_id, quota_date, bookmark_saves_used, rag_queries_used

## Configuration

### Profiles
- **dev** (default): Reads credentials from env vars (`DB_URL`, `GEMINI_API_KEY`, etc.)
- **local**: Inline credentials for local PostgreSQL

### Key Properties
| Prefix | Properties |
|--------|------------|
| `rsa` | public-key, private-key (classpath:certs/*.pem) |
| `gemini` | api-key, base-url, embedding-model, generation-model, embedding-dimensions |
| `cors` | allowed-origins (comma-separated) |
| `jina` / `tinyfish` | api-key (fallback scrapers) |

## Known Issue
- `ContentMetadataResolver` references `ContentType.TWITTER` / `INSTAGRAM` but the enum only defines `YOUTUBE, ARTICLE, OTHER`. TWITTER/INSTAGRAM are handled as `OTHER` in that region.
- `GeminiClient.generateAnswer()` hardcodes the model path instead of reading from `GeminiProperties.generationModel`.
- Tag API endpoints are unimplemented (schema + repository exist).
- `/test/**` endpoints are publicly accessible (no auth).
- `application-local.properties` contains a hardcoded Gemini API key.
- Only 1 test file (`TextChunkerTest.java`) — low coverage.
