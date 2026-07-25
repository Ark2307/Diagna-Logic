# Diagna-Logic

A meeting-intelligence app over Google Research's **MISeD** dataset (information-seeking
dialogs grounded in real meeting transcripts). It has three parts:

- **MongoDB** — 225 meetings and 432 dialogs, each with its transcript, speaker stats, and
  gold question/answer turns with citation ranges.
- **Backend** (Spring Boot / Java 17) — REST APIs over that data, plus a model-agnostic LLM
  layer powering meeting summarization and a **RAG chat bot hard-scoped to one meeting at a
  time** (see [How the RAG chat works](#how-the-rag-chat-works) below).
- **Frontend** (Vite + React + Untitled UI) — a console for browsing meetings/dialogs,
  reading transcripts with citation highlighting, and talking to a meeting through a chat
  flyout.

## Prerequisites

- **MongoDB** running and reachable — this project assumes an already-populated `diagna`
  database (see [Data](#data) below).
- **Java 17** and Maven (or the bundled `./backend/mvnw`)
- **Node.js 20+** and npm
- An `OPENAI_API_KEY` — optional. Without one, the app runs fully offline against a
  deterministic **mock** LLM/embedding provider (see [Model-agnostic LLM layer](#model-agnostic-llm-layer)).

## Data

The `diagna` MongoDB database holds two collections ingested once from the raw MISeD JSONL
files (`meetings`, `dialogs` — 225 / 432 documents), plus three collections the backend
manages itself at runtime (`meeting_chunks`, `chat_conversations`, `llm_invocations`).

The one-time transform (raw JSONL → Mongo documents) was a Python script that has since been
removed from this repo — the ingested data is the deliverable, not the script that produced
it. It's still recoverable from an earlier commit (`git log --all -- tools/`) if the MISeD
format ever needs to be re-ingested from scratch or a data refresh is needed later.

Point the backend at your own MongoDB via `MONGODB_URI` (see [Environment variables](#environment-variables)).
If you're setting this up somewhere without an existing populated `diagna` database, a
`docker-compose.yml` is included to run plain `mongo:7` locally (`docker compose up -d
mongo`) — you'd just need a populated database to point it at.

## Quickstart

```bash
# 1. Backend (reads MONGODB_URI, defaults to mongodb://localhost:27017/diagna)
cd backend
./mvnw spring-boot:run
# -> http://localhost:8080, springdoc UI at /swagger-ui.html

# 2. Frontend
cd frontend
npm install
npm run dev
# -> http://localhost:5173, proxies /api to the backend
```

Or via the Makefile: `make backend`, `make frontend`.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/diagna` | Backend's Mongo connection |
| `LLM_DEFAULT_PROVIDER` | `mock` | Which registered provider answers a request when none is specified — `mock` or `openai` |
| `OPENAI_API_KEY` | _(empty)_ | Enables the OpenAI provider; required only if you set `LLM_DEFAULT_PROVIDER=openai` or pass `"provider": "openai"` per-request |
| `LLM_OPENAI_MODEL` | `gpt-4.1-mini` | Chat/generation model |
| `LLM_OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Embedding model for RAG |
| `LLM_OPENAI_EMBEDDING_DIMENSIONS` | `1536` | Embedding vector size |
| `VITE_BACKEND_URL` | `http://localhost:8080` | Frontend dev-server proxy target, if the backend isn't on 8080 |

RAG tuning (chunk size, top-K, relevance floor, context budget) lives in
`backend/src/main/resources/application.yml` under `diagna.rag` — see the inline comments
there for what each knob does and how the defaults were calibrated.

## API reference

All endpoints are under `/api/v1`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/meetings` | Paginated list, filterable by `corpus,domain,split,speaker,q,minSegments` |
| GET | `/meetings/{id}` | Meeting metadata + speaker rollups (`?includeTranscript=true` to inline it) |
| GET | `/meetings/{id}/transcript` | Paged transcript slice — `?from&to` |
| GET | `/meetings/{id}/speakers` | Per-speaker segment/char rollups |
| GET | `/meetings/{id}/dialogs` | Dialogs belonging to this meeting |
| GET | `/dialogs` | Paginated list, filterable by `meetingId,split,corpus,queryType,hasUnanswerable,minTurns` |
| GET | `/dialogs/{id}` | A dialog's turns (`?resolveAttributions=true` inlines cited segments) |
| GET | `/dialogs/{id}/turns/{turnIndex}/attribution` | Resolve one turn's citation ranges to transcript segments |
| GET | `/search` | Full-text search — `q, scope=transcripts\|dialogs\|all, limit` |
| GET | `/stats` | Corpus-wide rollup: totals, distributions, unanswerable rate, attribution coverage |
| GET | `/stats/meetings/{id}` | Per-meeting stats |
| POST | `/ai/chat` | RAG chat — `{meetingId, message, conversationId?, provider?, model?, topK?}` |
| POST | `/ai/generate` | Meeting summarization/minutes/decisions/etc — `{meetingId, task, instructions?, provider?, model?}` |
| GET | `/ai/meetings/{id}/conversations` | List chat threads for a meeting |
| GET / DELETE | `/ai/conversations/{id}` | Reload / clear a chat thread |

Full request/response shapes: `/swagger-ui.html` while the backend is running.

## How the RAG chat works

Kept deliberately simple — no vector database, no separate indexing service.

1. **Data source**: each meeting's transcript is split into overlapping ~400-token chunks
   (`ChunkBuilder`), embedded, and stored in the `meeting_chunks` Mongo collection — one
   document per chunk, with the embedding as a float array alongside its `meetingId` and the
   transcript segment range it covers. This happens **lazily**: the first chat question
   against a meeting triggers `EmbeddingIndexService` to embed and store that meeting's
   chunks; every question after that is a pure read. There's no separate "build the index"
   step to run.

2. **Semantic search**: a question is embedded with the same provider, then compared against
   every chunk belonging to that one meeting using cosine similarity — computed directly in
   the JVM, not via a vector database or ANN index. This works because retrieval is always
   scoped to a single meeting, so the candidate set is tiny (23 chunks at the median, ~90 at
   the largest) — an exact brute-force scan is sub-millisecond and, unlike an ANN index, has
   no approximation error.

3. **Fusion**: that semantic ranking is combined with a MongoDB `$text` keyword search over
   the same meeting's chunks (catches proper nouns and jargon embeddings tend to blur),
   merged via reciprocal rank fusion.

4. **Scoping**: `meetingId` is a required parameter everywhere in this pipeline, not an
   optional filter — a query is structurally incapable of reaching another meeting's data.
   If the best fused score is below a configured floor, the request is answered
   `unanswerable` without ever calling the LLM. Every citation the model returns is verified
   against what was actually retrieved before being shown to the user; an answer with no
   valid citations is forced to `unanswerable` too. See `ScopeGuard` for the full contract.

**Roadmap**: at this corpus size (hundreds of meetings, tens of chunks each) exact cosine +
lexical fusion is both fast and precise, so a re-ranker would only reorder an already-correct
candidate set. If the corpus grows enough that this stops holding, add a **cross-encoder
re-ranking** stage in `MeetingRetriever` — re-score the top ~20-30 fused candidates with a
model that reads the question and each passage together, then truncate to `topK`. Left as a
`TODO` there rather than built now, since it isn't needed yet.

## Model-agnostic LLM layer

Every LLM/embedding call goes through an abstract `LlmProvider`/`LlmQuery`/`LlmResult` (and
`EmbeddingProvider`) contract. `AbstractLlmProvider` fixes the flow — validate, call the
vendor SDK, parse the response, cache, record usage — so adding a new vendor (Gemini, Claude)
means implementing one method (`doComplete`) that maps its SDK response into a neutral
`LlmCompletion`; nothing in the controllers, services, prompts, or RAG pipeline changes.

Two providers ship today:
- **`mock`** (default) — deterministic and keyless, so the whole app runs and tests pass with
  zero external dependency or cost.
- **`openai`** — real OpenAI chat + embedding models, enabled by setting `OPENAI_API_KEY`.

## Testing

```bash
cd backend && ./mvnw test      # unit tests
cd frontend && npm run test    # Vitest
cd frontend && npm run build   # tsc -b + vite build
```

## Troubleshooting

- **Backend can't reach Mongo**: confirm `MONGODB_URI` (or the default
  `mongodb://localhost:27017/diagna`) points at a reachable instance with the `diagna`
  database populated — `mongosh mongodb://localhost:27017/diagna --eval
  "db.meetings.countDocuments()"` should return `225`.
- **Chat always says out of scope**: the mock embedding provider is a crude hashing-trick
  bag-of-words, not real semantic embeddings — it occasionally under-scores a genuinely
  in-scope but lexically-sparse question. Set `OPENAI_API_KEY` and `LLM_DEFAULT_PROVIDER=openai`
  for real embeddings, or lower `diagna.rag.min-relevance` in `application.yml`.
- **Frontend can't reach the API**: set `VITE_BACKEND_URL` if the backend isn't on port 8080.
