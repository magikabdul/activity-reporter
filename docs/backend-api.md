# Reporter backend — API reference

Base URL: `http://<host>:7500/reporter` (in cluster: `https://reporter.home.cholewa.dev/reporter`).
All responses are `application/json`. No authentication. No CORS headers.

Dates are ISO `yyyy-mm-dd` (`LocalDate`). "Today" is evaluated in `reporter.time-zone` (default `Europe/Warsaw`), not in the
container's UTC clock. Two kinds of ids exist: a registration has a **UUID** (in-memory, until `complete`), a stored task has
a **numeric** database id.

Actuator is **not** under this base URL: `http://<pod>:7501/actuator/health/{liveness,readiness}` and `/actuator/prometheus`
(own management port, not exposed by the Service/Ingress).

## Lufthansa — `lufthansa/api/LufthansaController.java`

### `POST /lufthansa/tasks:register`
Request `CreateTaskRequest`:

| field | type | validation |
|---|---|---|
| `description` | string | `@NotEmpty`, `@Size(min=10, max=255)` |
| `createdAt` | date? | optional, day the work was done; default today; future → 400 |

AI (`CategorizeService`) corrects the Polish text and assigns a `TaskCategory`. Category `UNKNOWN` → 404 `AI processing error`.

Response `CreatedTaskResponse` (nulls omitted):

| field | type | notes |
|---|---|---|
| `id` | UUID | only on register |
| `createdAt` | date | |
| `category` | `TaskCategory` (enum name) | |
| `description` | string | AI-corrected |

### `POST /lufthansa/tasks:complete/{taskId}`
No body. Persists the in-memory task. Response: `CreatedTaskResponse` **without `id`**.
400 `Task processing error` when: `Task not registered yet`, `Task not found`, `Invalid task id…`, `Task category is not determined`.

### `GET /lufthansa/tasks?year={int}&month={int}`
Stored tasks of the month ordered by `created_at, id`. Empty month → **`200 []`** (unlike the report endpoints).

Response `TaskResponse[]`: `id` (number), `createdAt`, `category`, `description`.

### `PUT /lufthansa/tasks/{id}`
Manual correction — **no AI involved**. Request `UpdateTaskRequest`:

| field | type | validation |
|---|---|---|
| `createdAt` | date | `@NotNull`; future → 400 `Task processing error` |
| `category` | `TaskCategory` | `@NotNull`; `UNKNOWN` → 400 `Task processing error` |
| `description` | string | `@NotEmpty`, `@Size(min=10, max=500)` — wider than on create, AI-corrected text may exceed 255 |

Response: the updated `TaskResponse`. Unknown id → 404 `Task not found` (with body).

### `DELETE /lufthansa/tasks/{id}`
`204 No Content`. Unknown id → 404 `Task not found` (with body).

### `GET /lufthansa/report?year={int}&month={int}`
Both params required (missing → 400). For each of the 7 categories fetches that month's descriptions and asks OpenAI
for one ≤500-char Polish summary; categories with no data are dropped. Nothing at all → **404, empty body**.

Response `ReportResponse[]`:

| field | type | notes |
|---|---|---|
| `name` | string | category name with `_` → space, e.g. `SOFTWARE DEVELOPMENT` |
| `description` | string | Polish category description (see enum) |
| `summary` | string | AI synthesis |

### Enum `TaskCategory`

| value | description (PL) |
|---|---|
| `SOFTWARE_DEVELOPMENT` | Projektowanie i testowanie oprogramowania w odgórnie ustalonym czasie zgodnie wymaganiami biznesowymi. |
| `CONSULTING_AND_TRAINING` | Udzielenie konsultacji oraz przeprowadzanie szkoleń w zakresie wytwarzania oprogramowania. |
| `DOCUMENTATION` | Przygotowywanie dokumentacji technicznej i projektowej związanej z wytwarzanym oprogramowaniem w formie i na zasadach obowiązujących u Zlecającego. |
| `CODE_ANALYSIS_AND_REFINEMENT` | Analiza wymagań, tworzenie i doskonalenie kodu źródłowego i weryfikacja powstających funkcjonalności. |
| `BUG_FIXING_AND_MAINTENANCE` | Rozwiązywanie błędów i wprowadzanie zmian kodzie źródłowym. |
| `TECHNOLOGY_SELECTION` | Dobór technologii do rozwiązań na podstawie znajomości trendów rozwoju oprogramowania i różnych technologii. |
| `ARCHITECTURE_DESIGN` | Analiza i projektowanie architektury oprogramowania w zleconych projektach. |
| `UNKNOWN` | Nie można przypisać zadania do żadnej z dostępnych kategorii. (never persisted, never in reports) |

## Trecom — `trecom/api/TrecomController.java`

### `POST /trecom/tasks:register`
Request `CreateTaskRequest`:

| field | type | validation |
|---|---|---|
| `customer` | string | `@NotNull` (DB: VARCHAR(100)) |
| `description` | string | `@NotNull`, `@Size(min=10, max=255)` |
| `hoursSpent` | int | `@Min(1)` |
| `salesman.firstName` | string | `@NotEmpty`, `^[A-Z\p{Lu}][a-z\p{Ll}]*$` |
| `salesman.lastName` | string | `@NotEmpty`, `^[A-Z\p{Lu}][a-z\p{Ll}]*(-[A-Z\p{Lu}][a-z\p{Ll}]*)?$` |
| `notes` | string? | optional, AI-corrected like the description |
| `createdAt` | date? | optional, day the work was done; default today; future → 400 |

AI (`ContentQualityService`, prompts in `TrecomPrompt`) validates/corrects first name, last name and description in
parallel (plus the notes when present); `customer` is upper-cased. AI rejecting a name → 404 `AI processing error` (`Provided word is not a firstname: X`).

Response `CreatedTaskResponse` (nulls omitted): `id` (UUID, register only), `createdAt`, `customer`, `description`,
`hoursSpent`, `salesman{firstName,lastName}`, `notes` (AI-corrected).

### `POST /trecom/tasks:complete/{taskId}`
No body. Persists. Response as above without `id`. 400 on unknown/mismatched id.

### `GET /trecom/tasks?year={int}&month={int}`
Stored tasks of the month ordered by `created_at, id`. Empty month → **`200 []`**.

Response `TaskResponse[]`: `id` (number), `createdAt`, `customer`, `description`, `hoursSpent`,
`salesman{firstName,lastName}`, `notes` (nullable).

### `PUT /trecom/tasks/{id}`
Manual correction — **no AI involved**. Request `UpdateTaskRequest`: `createdAt` (`@NotNull`, not in the future),
`customer` (`@NotBlank`, `@Size(max=100)`, stored upper-cased), `description` (`@Size(min=10, max=500)`),
`hoursSpent` (`@Min(1)`), `salesman` (`@Valid`, same name patterns as on create), `notes` (nullable — `null` clears them).

Response: the updated `TaskResponse`. Unknown id → 404 `Task not found` (with body).

### `DELETE /trecom/tasks/{id}`
`204 No Content`. Unknown id → 404 `Task not found` (with body).

### `GET /trecom/report?year={int}&month={int}`
Raw listing, no AI. Empty list → **404, empty body**.

Response `ReportResponse[]` (nulls serialized):

| field | type | notes |
|---|---|---|
| `createdAt` | string | ISO date `2026-02-09` |
| `company` | string | `customer_name` |
| `description` | string | |
| `salesman` | string | `"FirstName LastName"` |
| `hoursSpent` | int | |

## Errors — `error/GlobalErrorWebExceptionHandler.java`

Body `ErrorMessage`: `{ "status": int, "title": string, "description": string }`.

| Cause | HTTP | title | description |
|---|---|---|---|
| Bean validation (`WebExchangeBindException`) | 400 | `invalid request content` | `"<field> <message>, …"` |
| `TaskException` | 400 | `Task processing error` | message |
| `ServerWebInputException` (bad UUID, missing param, bad JSON) | 400 | reason | cause message |
| `AiProcessingException` | **404** | `AI processing error` | message |
| `TaskNotFoundException` (update/delete) | **404** | `Task not found` | `Task with id N does not exist` |
| other `ResponseStatusException` (unknown path, wrong method…) | its own status | status text | reason |
| `NotImplementedException` | 501 | `Not implemented` | |
| other | 500 | exception message | `Exception of type X occurred` |

Report "not found" is `ResponseEntity.notFound()` → **no body**. Distinguish 404s by body presence (and `title`).
The handler looks processors up by the **exact** exception class, so a new exception type has to be registered in its map.

## Database (Flyway, `src/main/resources/db/migration`)

`trecom`: `id BIGSERIAL PK`, `created_at DATE NOT NULL DEFAULT NOW()`, `customer_name VARCHAR(100)`, `description TEXT`,
`hours_spent NUMERIC(6,2) CHECK >= 0`, `salesperson_last_name VARCHAR(100)`, `salesperson_first_name VARCHAR(100)`, `notes TEXT NULL`.

`lufthansa`: `id BIGSERIAL PK`, `created_at DATE NOT NULL`, `category VARCHAR(64)` (CHECK: 7 values, no UNKNOWN), `description TEXT`.

No relations between the tables.
