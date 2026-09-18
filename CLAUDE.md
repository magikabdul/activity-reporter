# Reporter

Activity/work-log reporter for a contractor working for two clients: **Lufthansa** and **Trecom**.
The user registers a performed task in natural (Polish) language, OpenAI cleans/validates the text
(and, for Lufthansa, assigns a contractual work category), the user confirms, and the record is stored.
Monthly reports are generated per `year` + `month`.

- Backend: this repo root (Gradle 9.7, Java 21, Spring Boot 4.1.1, **WebFlux**, R2DBC + PostgreSQL, Flyway, Spring AI 2.0 (GA) + OpenAI `gpt-4o`, MapStruct, Lombok)
- Frontend: `frontend/` (React + Vite + TypeScript) — see `frontend/CLAUDE.md`
- Full API reference (DTOs, validation, errors, DB schema): `docs/backend-api.md`

## Hard rules

- **Never read `k8s-secrets.yaml` or `env`** (both gitignored, contain production credentials). Do not open, grep,
  cat or `@`-mention them. Key *names* are visible in `k8s-deployment.yaml` — that is enough.
  `.claude/settings.json` denies reading them.
- The database is a **production** managed Postgres. `tasks:complete`, `PUT /tasks/{id}` and `DELETE /tasks/{id}` change real
  rows — never call them with test data without the user's consent. `tasks:register` is safe (in-memory only) but costs OpenAI tokens.
- `GET /lufthansa/report` calls OpenAI once per category on **every** request — do not poll or spam it.

## Backend layout

Root package `cloud.cholewa.reporter` (`src/main/java/...`):

| Package | Contents |
|---|---|
| `config` | `AppConfig` (ObjectMapper, `Clock` in `reporter.time-zone`), `TaskDateResolver` (task date: requested or today, never future), `DatabaseConfig` (manual R2DBC ConnectionFactory, `sslMode=REQUIRE`), `ErrorHandlerConfig` |
| `error` | `GlobalErrorWebExceptionHandler` (no ControllerAdvice), `error.processor.*`, `ErrorMessage` |
| `lufthansa` | `api`, `service` (`LufthansaService`, `CategorizeService`, `LufthansaReportService`), `repository`, `mapper`, `model` |
| `trecom` | `api`, `service` (`TrecomService`, `ContentQualityService`, `TrecomPrompt`), `repository`, `mapper`, `model` |

The two companies are fully independent sibling packages (own controller, DTOs, table). No shared abstraction, no tenant enum.
Flyway migrations: `src/main/resources/db/migration` (`V1` table `trecom`, `V2` table `lufthansa`).

## API summary

Base path `/reporter` (`spring.webflux.base-path`), port `7500`. No auth, **no CORS**, no OpenAPI.
Actuator (`health` with liveness/readiness, `prometheus`) listens on its **own port `7501`** without the base path — it is
not part of the Service/Ingress, only the k8s probes use it.

| Company | Endpoint | Request | Response |
|---|---|---|---|
| lufthansa | `POST /lufthansa/tasks:register` | `{description, createdAt?}` (10–255) | `{id, createdAt, category, description}` |
| lufthansa | `POST /lufthansa/tasks:complete/{taskId}` | – | same without id |
| both | `GET /{company}/tasks?year&month` | – | stored tasks of the month with numeric `id` + `createdAt`; empty month → `200 []` |
| both | `PUT /{company}/tasks/{id}` | all fields incl. `createdAt` (description ≤ 500) | updated task — **manual edit, no AI** |
| both | `DELETE /{company}/tasks/{id}` | – | `204` |
| lufthansa | `GET /lufthansa/report?year&month` | – | `[{name, description, summary}]` (AI summary per category) |
| trecom | `POST /trecom/tasks:register` | `{customer, description, hoursSpent, salesman{firstName,lastName}, notes?, createdAt?}` | `{id, createdAt, customer, description, hoursSpent, salesman, notes}` |
| trecom | `POST /trecom/tasks:complete/{taskId}` | – | same without id |
| trecom | `GET /trecom/report?year&month` | – | `[{createdAt, company, description, salesman, hoursSpent}]` |

`tasks:register` / `tasks:complete` contain a **literal colon** (Google AIP custom-method style).

### Quirks worth knowing

- `register` does **not** persist. The registered task lives in one mutable field on the singleton service
  (`processedTask`), one per company, global. A second register overwrites the first; only `complete` writes to DB
  There is no discard endpoint. The registration id is a UUID; stored tasks have a numeric DB id.
- Task dates: optional `createdAt` on register (default: today in `reporter.time-zone`, `Europe/Warsaw`; the container clock
  is UTC, so `LocalDate.now()` without the `Clock` bean is wrong around midnight). Future dates → 400.
- Errors: JSON `{status, title, description}`. AI failures (`AiProcessingException`: unclassifiable task, invalid
  first/last name, missing customer) return **404 with a body**. An empty report returns **404 with an empty body**.
  Validation / bad UUID / missing query param → 400. `TaskException` (register/complete mismatch, future date, `UNKNOWN`
  category) → 400. `TaskNotFoundException` (update/delete of a missing id) → **404 with a body**. Framework
  `ResponseStatusException`s (unknown path, wrong method) keep their own status. The processor map in
  `GlobalErrorWebExceptionHandler` is keyed by the **exact** exception class — a new exception type must be registered there.
- Trecom `notes` are AI-corrected inside the `Mono.zip` of `ContentQualityService.process` (absent notes travel as an empty
  `Optional`) and persisted. `customer` is upper-cased by the service (on update too).
- `spring.profiles.active: reporter` has no matching config file.
- There are no tests against a real database (no Testcontainers; `DatabaseConfig` hardcodes `sslMode=REQUIRE`).

## Commands

```bash
./gradlew clean build          # build + tests (JDK 21)
./gradlew bootRun              # needs env vars: database-host/-port/-name/-user/-password, flyway-url, OPENAI_API_KEY
./gradlew bootRun --args='--spring.flyway.enabled=false'   # smoke start without a database (actuator, validation, routing)
./gradlew bootBuildImage --imageName=magikabdul/reporter:<version>
```

CI: `.github/workflows/ci.yaml` (build on every push), `sonar.yaml`, `release.yaml` (manual `workflow_dispatch`;
sed-bumps `version` in `build.gradle`, tags `vX.Y.Z`, pushes `magikabdul/reporter:X.Y.Z` + `latest` to Docker Hub).
Frontend has its own `frontend-ci.yaml` / `frontend-release.yaml` (tags `frontend-vX.Y.Z`, image `magikabdul/reporter-frontend`).
Nothing in CI deploys to Kubernetes — manifests are applied manually.

## Kubernetes (local cluster, kubectl context `home`)

- 3-node cluster, ingress-nginx (`ingressClassName: nginx`, LoadBalancer IP `10.78.20.201`), cert-manager with
  `ClusterIssuer letsencrypt-dns` (DNS-01). TLS pattern on this cluster: a `Certificate` resource per namespace.
- Namespace `krisoo`: Deployment `reporter` (image `magikabdul/reporter:<version>`, probes on management port 7501,
  `TZ=Europe/Warsaw`), Service `reporter-service:7500`,
  Ingress `reporter-ingress` (host-less, no TLS, path `/reporter`) — manifests in repo root (`k8s-namespace.yaml`, `k8s-deployment.yaml`).
- Frontend: manifests in `frontend/k8s/`. Host **`https://reporter.home.cholewa.dev`**: `/` → `reporter-frontend`,
  `/reporter` → `reporter-service:7500` (same origin, so no CORS needed). Own certificate
  `reporter-home-cholewa-dev-tls` (single-host, not the wildcard — avoids Let's Encrypt duplicate-certificate limits).
- Local DNS has explicit records per host (no wildcard): new hosts need a record → `10.78.20.201`.
