# Reporter

Activity/work-log reporter for a contractor working for two clients: **Lufthansa** and **Trecom**.
The user registers a performed task in natural (Polish) language, OpenAI cleans/validates the text
(and, for Lufthansa, assigns a contractual work category), the user confirms, and the record is stored.
Monthly reports are generated per `year` + `month`.

- Backend: this repo root (Gradle, Java 21, Spring Boot 4.0.2, **WebFlux**, R2DBC + PostgreSQL, Flyway, Spring AI + OpenAI `gpt-4o`, MapStruct, Lombok)
- Frontend: `frontend/` (React + Vite + TypeScript) — see `frontend/CLAUDE.md`
- Full API reference (DTOs, validation, errors, DB schema): `docs/backend-api.md`

## Hard rules

- **Never read `k8s-secrets.yaml` or `env`** (both gitignored, contain production credentials). Do not open, grep,
  cat or `@`-mention them. Key *names* are visible in `k8s-deployment.yaml` — that is enough.
  `.claude/settings.json` denies reading them.
- The database is a **production** managed Postgres. `tasks:complete` writes real rows — never call it with test data
  without the user's consent. `tasks:register` is safe (in-memory only) but costs OpenAI tokens.
- `GET /lufthansa/report` calls OpenAI once per category on **every** request — do not poll or spam it.

## Backend layout

Root package `cloud.cholewa.reporter` (`src/main/java/...`):

| Package | Contents |
|---|---|
| `config` | `AppConfig` (ObjectMapper), `DatabaseConfig` (manual R2DBC ConnectionFactory, `sslMode=REQUIRE`), `ErrorHandlerConfig` |
| `error` | `GlobalErrorWebExceptionHandler` (no ControllerAdvice), `error.processor.*`, `ErrorMessage` |
| `lufthansa` | `api`, `service` (`LufthansaService`, `CategorizeService`, `LufthansaReportService`), `repository`, `mapper`, `model` |
| `trecom` | `api`, `service` (`TrecomService`, `ContentQualityService`, `TrecomPrompt`), `repository`, `mapper`, `model` |

The two companies are fully independent sibling packages (own controller, DTOs, table). No shared abstraction, no tenant enum.
Flyway migrations: `src/main/resources/db/migration` (`V1` table `trecom`, `V2` table `lufthansa`).

## API summary

Base path `/reporter` (`spring.webflux.base-path`), port `7500`. No auth, **no CORS**, no actuator, no OpenAPI.

| Company | Endpoint | Request | Response |
|---|---|---|---|
| lufthansa | `POST /lufthansa/tasks:register` | `{description}` (10–255) | `{id, category, description}` |
| lufthansa | `POST /lufthansa/tasks:complete/{taskId}` | – | `{category, description}` (no id) |
| lufthansa | `GET /lufthansa/report?year&month` | – | `[{name, description, summary}]` (AI summary per category) |
| trecom | `POST /trecom/tasks:register` | `{customer, description, hoursSpent, salesman{firstName,lastName}, notes?}` | `{id, customer, description, hoursSpent, salesman}` |
| trecom | `POST /trecom/tasks:complete/{taskId}` | – | same without id |
| trecom | `GET /trecom/report?year&month` | – | `[{createdAt, company, description, salesman, hoursSpent}]` |

`tasks:register` / `tasks:complete` contain a **literal colon** (Google AIP custom-method style).

### Quirks worth knowing

- `register` does **not** persist. The registered task lives in one mutable field on the singleton service
  (`processedTask`), one per company, global. A second register overwrites the first; only `complete` writes to DB
  (`createdAt = LocalDate.now()`). There is no discard/list/edit/delete endpoint.
- Errors: JSON `{status, title, description}`. AI failures (`AiProcessingException`: unclassifiable task, invalid
  first/last name, missing customer) return **404 with a body**. An empty report returns **404 with an empty body**.
  Validation / bad UUID / missing query param → 400. `TaskException` (task not found / wrong id) → 400.
- Trecom `notes` is accepted but **never persisted**: `ContentQualityService.process` runs the notes prompt as a detached
  `subscribe()` and never copies the result onto the `Task` (known bug, costs an OpenAI call for nothing).
  `customer` is upper-cased by the service.
- `spring.profiles.active: reporter` has no matching config file. Actuator starter is commented out in `build.gradle`.

## Commands

```bash
./gradlew clean build          # build + tests (JDK 21)
./gradlew bootRun              # needs env vars: database-host/-port/-name/-user/-password, flyway-url, OPENAI_API_KEY
./gradlew bootBuildImage --imageName=magikabdul/reporter:<version>
```

CI: `.github/workflows/ci.yaml` (build on every push), `sonar.yaml`, `release.yaml` (manual `workflow_dispatch`;
sed-bumps `version` in `build.gradle`, tags `vX.Y.Z`, pushes `magikabdul/reporter:X.Y.Z` + `latest` to Docker Hub).
Frontend has its own `frontend-ci.yaml` / `frontend-release.yaml` (tags `frontend-vX.Y.Z`, image `magikabdul/reporter-frontend`).
Nothing in CI deploys to Kubernetes — manifests are applied manually.

## Kubernetes (local cluster, kubectl context `home`)

- 3-node cluster, ingress-nginx (`ingressClassName: nginx`, LoadBalancer IP `10.78.20.201`), cert-manager with
  `ClusterIssuer letsencrypt-dns` (DNS-01). TLS pattern on this cluster: a `Certificate` resource per namespace.
- Namespace `krisoo`: Deployment `reporter` (image `magikabdul/reporter:<version>`), Service `reporter-service:7500`,
  Ingress `reporter-ingress` (host-less, no TLS, path `/reporter`) — manifests in repo root (`k8s-namespace.yaml`, `k8s-deployment.yaml`).
- Frontend: manifests in `frontend/k8s/`. Host **`https://reporter.home.cholewa.dev`**: `/` → `reporter-frontend`,
  `/reporter` → `reporter-service:7500` (same origin, so no CORS needed). Own certificate
  `reporter-home-cholewa-dev-tls` (single-host, not the wildcard — avoids Let's Encrypt duplicate-certificate limits).
- Local DNS has explicit records per host (no wildcard): new hosts need a record → `10.78.20.201`.
