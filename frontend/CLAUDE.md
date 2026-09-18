# Reporter frontend

Dark-mode SPA for the Reporter backend (see `../CLAUDE.md` and `../docs/backend-api.md`).
Single user, LAN only, **no authentication**. UI language: English (task content is Polish).

Stack: React 19, Vite, TypeScript (strict), Tailwind CSS v4, React Router, TanStack Query,
react-hook-form + zod, sonner, lucide-react, write-excel-file. Node 24, npm.

## Commands

```bash
npm run dev         # http://localhost:5173, proxies /reporter -> VITE_API_PROXY (default http://10.78.20.201)
npm run lint        # eslint + prettier --check
npm run typecheck
npm test            # vitest (jsdom)
npm run build
npm run format
```

The dev proxy talks to the **production** backend. `tasks:register` is harmless (in-memory) but
`Confirm & save` (`tasks:complete`) writes to the production DB — do not click it with test data.

## Structure

```
src/api/            fetch client (ApiError), per-company endpoints, DTO types
src/theme/          tokens.css (all colours), companies.ts (routes/metadata, themeForPath)
src/components/ui/  the UI kit — Button/ButtonLink, Field (TextField/TextAreaField), Card, Badge, Alert, Table, EmptyState, Skeleton
src/components/     layout/ (AppShell, Sidebar, PageHeader), MonthPicker, ReportToolbar
src/features/tasks/ shared register -> review -> complete flow (useTaskFlow, Steps, TaskReview, TaskSaved, TaskAlerts)
src/features/{lufthansa,trecom,home}/   pages, zod schemas, trecom/summaries.ts
src/lib/            export (csv/xlsx), clipboard, period, storage, suggestions, pendingTask, validation
nginx/ Dockerfile k8s/
```

## Styling rules (keep the UI uniform)

- Colours come **only** from CSS tokens in `src/theme/tokens.css`, exposed as Tailwind colours in `src/index.css`
  (`bg-surface`, `bg-surface-2`, `border-border`, `text-text`, `text-muted`, `bg-accent`, `text-accent-fg`,
  `bg-accent-soft`, `danger`, `success`). Never put hex values or Tailwind palette colours (`bg-blue-500`) in components.
- Company look = palette only. `AppShell` sets `data-company="lufthansa|trecom|neutral"` on `<html>` from the route;
  `[data-company=…]` blocks override the tokens. Any element can scope a palette the same way (Home cards, sidebar dots).
  Lufthansa: navy `#05164D` + yellow `#FFAD00`. Trecom: navy `#011437` + sky blue `#2E9CEB` (sampled from trecom.pl).
- Shapes/spacing/typography are identical everywhere: radii `rounded-card` / `rounded-control`, controls `h-10`, small `h-8`.
- Always build from `components/ui`. A link that looks like a button is `ButtonLink` (shares `buttonClasses`).
  Add a new primitive to the kit rather than styling ad hoc in a page.
- Print: `@media print` in `index.css` swaps tokens to a light sheet; hide chrome with `no-print`, use `print:block` for print-only text.

## Behaviour worth knowing

- **Task flow** (`useTaskFlow`): `register` → AI result shown → `complete` persists. The pending task (id + user input) lives in
  `sessionStorage` (`reporter.<company>.pendingTask`). "Edit again" just abandons it — there is no discard endpoint; the next
  register overwrites the backend's single in-memory slot. `complete` answering 400 means the slot was lost → form is restored
  with the user's input and `StaleTaskAlert` is shown.
- **Errors** (`api/client.ts`): backend body `{status,title,description}` → `ApiError`. 404 **with** body = AI rejection (shown
  inline as "AI could not accept this task"); 404 **without** body = empty report (`isEmptyResult` → empty state).
- **Lufthansa report** costs OpenAI calls on every GET → query is `enabled: false`, fetched only by the Generate/Regenerate
  button, cached in `localStorage` (`reporter.lufthansa.report.YYYY-MM`). Trecom report is cheap and loads automatically.
- **Suggestions**: no dictionary endpoint, so customers/salesmen are remembered in `localStorage`
  (`reporter.trecom.suggestions`) after a save and learned from fetched reports.
- **Trecom `notes`**: sent to the API but the backend currently never persists it (`ContentQualityService.process` does not
  copy it onto the task). The review step says so. Fixing it is a backend change.
- Validation mirrors the backend DTOs (`features/*/schema.ts`, `lib/validation.ts`) — keep them in sync with `docs/backend-api.md`.

## Deployment

Image `magikabdul/reporter-frontend` (multi-stage: node build → `nginx-unprivileged`, port 8080, `/healthz`, SPA fallback).
nginx serves static files only; the Ingress routes `/reporter` to the backend service, so API calls are same-origin.

```bash
docker build -t magikabdul/reporter-frontend:<version> frontend/
kubectl apply -f frontend/k8s/          # certificate, deployment+service, ingress (namespace krisoo)
```

Releases: GitHub Actions `Frontend Release` (`workflow_dispatch`, patch/minor/major) bumps `package.json`, pins the new tag in
`k8s/deployment.yaml`, pushes the image (+`latest`), tags `frontend-vX.Y.Z`. Applying manifests to the cluster stays manual.
URL: https://reporter.home.cholewa.dev (needs a local DNS record → 10.78.20.201).
