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
- Errors: JSON `{status, title, description}`. AI *rejections* (`AiProcessingException`: unclassifiable task, invalid
  first/last name, missing customer) return **404 with a body**; a *failed OpenAI call* (`AiUnavailableException`:
  timeout, 5xx, unreadable answer) returns **502**. The Lufthansa report fails as a whole with that 502 when one category
  cannot be summarised — never partially. An empty report returns **404 with an empty body**.
  Validation / bad UUID / missing query param / `year` outside 2000–2100 / `month` outside 1–12 → 400. `TaskException` (register/complete mismatch, future date, `UNKNOWN`
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

Both release workflows `git push origin main` as `github-actions[bot]`, so they are **incompatible with a PR-only
ruleset on `main`**. The ruleset `main` exists but is disabled on purpose (it was active for a few hours on 2026-09-18
and broke the 1.1.1 release with GH013). That bot cannot be added as a ruleset bypass actor in a user-owned repo — a
bypass would need a deploy key or an admin PAT. A release that fails on the push step is half-published:
`release.yaml` has already pushed the image and the tag (finish with a version-bump PR and
`gh release create vX.Y.Z --generate-notes --verify-tag`); `frontend-release.yaml` pushes `main` *before* tagging, so it
leaves an image without version bump, pinned manifest or tag.

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
- Rollouts are zero-downtime by design, with one replica: `preStop: sleep 10s` keeps the old pod serving while
  ingress-nginx drops it from its upstreams (without it the app's graceful shutdown — which closes the listener within
  milliseconds of SIGTERM — produced a few seconds of 502), then in-flight requests get up to 90 s
  (`spring.lifecycle.timeout-per-shutdown-phase`); `terminationGracePeriodSeconds: 120` must stay above the sum.
  The image has no shell, so hooks must be native actions (`sleep`, `httpGet`), never `exec`.
  The frontend pod has the same `preStop` sleep; its image stops with SIGQUIT, so nginx drains by itself afterwards.
  Any new workload behind this ingress needs the sleep too — a readiness probe does not cover the termination side.
- Before `kubectl apply` of a multi-document manifest read the whole `kubectl diff` — live objects have carried hand-made
  fixes that were missing from the repo (that is how `reporter-ingress` lost its `ingressClassName` once).

## Monitoring (lives in another repo)

Grafana / Prometheus / Loki / Alloy of the cluster are defined in
`D:\programming\smart-home-automation-system\deployment-tools` (`k8s/monitoring/`; **never read `application-secrets.yaml`
there**, nor `secret.yaml` / `secrets.yaml`). Reporter-specific parts:

- Prometheus job `krisoo-pods` scrapes pods annotated `prometheus.io/*` — the backend pod template carries them
  (port **7501**, `/actuator/prometheus`). Metrics are labelled `app="reporter"`, `namespace="krisoo"`.
- Grafana folder **reporter**: *Reporter / Overview*, *Reporter / Runtime*, *Reporter / Logs*, generated by
  `k8s/monitoring/grafana/dashboards/src/gen-reporter-dashboards.js`. The activity numbers there are derived from
  `http_server_requests_seconds_count` by `uri` — **renaming an endpoint silently empties those panels** (and the alert
  rules built on the same metrics), so change the generator together with the controller.
- Useful metric families: `http_server_requests_*` (by uri/method/status/exception), `gen_ai_client_operation_*` and
  `gen_ai_client_token_usage_total` (OpenAI calls and tokens), `okhttp_requests_*` (HTTP status of api.openai.com),
  `spring_data_repository_invocations_*`, `logback_events_total`. No histogram buckets are published.
- Logs are in Loki under `{namespace="krisoo", app="reporter"}` / `app="reporter-frontend"`; the backend logs plain text.
