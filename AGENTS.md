# Agent-Scope Java 2.0

AI knowledge base platform: Spring Boot backend + Vue 3 frontend + MongoDB + Milvus + local ONNX embeddings.

## Quick Start

```bash
# Backend (requires MongoDB on localhost:27017)
cd backend && mvn spring-boot:run

# Frontend (separate terminal)
cd frontend && npm install && npm run dev
```

- Frontend: `http://localhost:3000` (Vite dev server)
- Backend API: `http://localhost:8080/api/*`
- Vite proxies `/api` to `localhost:8080` (configured in `frontend/vite.config.ts`)
- Quick login: double-click blank area on login page (root/123456)

## Build Commands

```bash
# Backend
cd backend && mvn compile              # Compile only
cd backend && mvn clean package        # Build JAR

# Frontend
cd frontend && npx vue-tsc --noEmit    # Type check
cd frontend && npm run build           # Production build
```

## Architecture

**Backend** (Spring Boot 3.5.16, Java 21):
- Entry: `AgentScopeApplication.java`
- Config: single `application.yml` (no profiles)
- Auth: JWT + quick-login toggle (`app.quick-login.enabled`)
- MongoDB: `127.0.0.1:27017/test`
- Milvus: optional, `milvus.enabled=true`, port 19530
- GLM API: env vars `GLM_API_KEY` / `GLM_BASE_URL` / `GLM_MODEL` (fallback defaults in yml)
- Embedding: local ONNX model at `/home/software/AI/Xenova/bge-small-zh-v1.5/`
- File storage: `/data/agent-scope/files/{knowledgeBaseId}/`

**Frontend** (Vue 3 + TypeScript + Vite + Element Plus):
- API layer: `src/api/index.ts` (all functions), `src/utils/request.ts` (axios with interceptors)
- Router: `src/router/index.ts`
- Stores: Pinia (`src/store/user.ts`)
- Types: `src/types/index.ts`

**Key Controllers:**
- `AuthController` — `/api/auth` (login, quick-login, current user)
- `KnowledgeBaseController` — `/api/knowledge-bases` (CRUD, upload, split, embed, search, chunks)
- `ChatController` — `/api/chat` (sessions + messages)
- `UserController` — `/api/users`

## Gotchas

- `backend/start.sh` sets `OPENAI_API_KEY` env var (required by AgentScope harness even though GLM is used)
- No automated tests exist — `mvn test` finds no test classes
- Frontend has `package-lock.json` committed — use `npm ci` for reproducible installs
- `TraceContextFilter` is `@Order(HIGHEST_PRECEDENCE)` — runs before all other filters
- `MilvusConfig` is `@ConditionalOnProperty` + `@Lazy` — disabled entirely when `milvus.enabled=false`, connected on first use otherwise
- `DataInitConfig` auto-creates root user on startup if not exists
- Backend runs on port 8080; frontend on port 3000 with proxy
