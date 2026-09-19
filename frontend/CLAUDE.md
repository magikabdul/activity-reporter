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
`Confirm & save` (`tasks:complete`) and the Tasks page's Save / Delete write to the production DB — do not use them with
test data. For UI work on those flows run a throw-away mock API and point `VITE_API_PROXY` at it.

## Structure

```
src/api/            fetch client (ApiError), per-company endpoints, DTO types
src/theme/          tokens.css (all colours), companies.ts (routes/metadata, themeForPath)
src/components/ui/  the UI kit — Button/ButtonLink, Field (TextField/TextAreaField/SelectField), Card, Badge, Alert, Toast,
                    tones, Dialog/ConfirmDialog, Kbd/ShortcutHint, Table, EmptyState, Skeleton
src/components/     layout/ (AppShell, Sidebar, PageHeader), MonthPicker, ReportToolbar
src/features/tasks/ shared register -> review -> complete flow (useTaskFlow, Steps, TaskReview, TaskSaved, TaskAlerts),
                    shared form fields (fields.tsx), the month's task list (MonthlyTasksView, useMonthlyTasks,
                    EditFormActions) and cache sync after a change (useTaskChangeSync)
src/features/{lufthansa,trecom,home}/   pages (NewTask, Tasks, Report), zod schemas, trecom/TaskFields.tsx, trecom/summaries.ts
src/lib/            notify (toasts), export (csv/xlsx), clipboard, period, date, storage, suggestions, pendingTask,
                    reportCache, useShortcut, validation
nginx/ Dockerfile        (Kubernetes manifests: ../k8s/frontend/)
```

## Styling rules (keep the UI uniform)

- Colours come **only** from CSS tokens in `src/theme/tokens.css`, exposed as Tailwind colours in `src/index.css`
  (`bg-surface`, `bg-surface-2`, `border-border`, `text-text`, `text-muted`, `bg-accent`, `text-accent-fg`,
  `bg-accent-soft`, `danger`, `danger-fg`, `success`). Never put hex values or Tailwind palette colours (`bg-blue-500`) in components.
- Company look = palette only. `AppShell` sets `data-company="lufthansa|trecom|neutral"` on `<html>` from the route;
  `[data-company=…]` blocks override the tokens. Any element can scope a palette the same way (Home cards, sidebar dots).
  Lufthansa: navy `#05164D` + yellow `#FFAD00`. Trecom: navy `#011437` + sky blue `#2E9CEB` (sampled from trecom.pl).
- Shapes/spacing/typography are identical everywhere: radii `rounded-card` / `rounded-control`, controls `h-10`, small `h-8`.
- Feedback has one visual language, defined in `components/ui/tones.ts` (icon + colours per tone) and used by both
  `<Alert>` and `<Toast>`. Rule: a problem that belongs to a form or a page → inline `<Alert>` where it happened (validation,
  AI rejection, failed load/save inside a dialog); the _result of an action_ (saved, updated, deleted, copied, exported) →
  `notify.success/info/error` from `lib/notify.tsx`. Success toasts expire (timer bar), error toasts stay until dismissed.
  Never call `sonner` directly.
- Modals: `Dialog` / `ConfirmDialog` (native `<dialog>`: focus trap, Esc, backdrop). No `window.confirm`/`alert`.
  Initial focus goes to `[data-autofocus]` or the first field — React's `autoFocus` is ignored by `showModal()`.
- Dates: never render a bare `<input type="date">` — its text follows the OS short-date format, which the app cannot control
  (a Windows format like `ddd, dd.MM.yyyy` shows in Chrome as `, 03.09.2026`). Use `DateField`: it displays the value with
  `formatIsoDate` and only borrows the native calendar popup (`showPicker()`) from a visually hidden input that holds the value.
- Tables: cells are vertically centred (`Td`), badges never wrap. Logo: `components/ui/Logo.tsx` (token-coloured SVG; keep
  `public/favicon.svg` in sync).
- Always build from `components/ui`. A link that looks like a button is `ButtonLink` (shares `buttonClasses`).
  Add a new primitive to the kit rather than styling ad hoc in a page.
- Print: `@media print` in `index.css` swaps tokens to a light sheet; hide chrome with `no-print`, use `print:block` for print-only text.

## Behaviour worth knowing

- **Task flow** (`useTaskFlow`): `register` → AI result shown → `complete` persists. The pending task (id + user input) lives in
  `sessionStorage` (`reporter.<company>.pendingTask`). "Edit again" just abandons it — there is no discard endpoint; the next
  register overwrites the backend's single in-memory slot. `complete` answering 400 means the slot was lost → form is restored
  with the user's input and `StaleTaskAlert` is shown.
- **Tasks page** (`MonthlyTasksView`): lists the stored tasks of a month; Edit opens a dialog with the same field
  components as "New task" (`features/tasks/fields.tsx`, `trecom/TaskFields.tsx`) and saves with `PUT` — manual, no AI,
  description up to 500 chars; Delete asks via `ConfirmDialog`. Both companies only provide columns, API functions and their
  edit form.
- **After any task change** call the function from `useTaskChangeSync(company)` with every date involved: it invalidates
  `['<company>-tasks', 'YYYY-MM']` (Tasks page + Home stats share this key), the Trecom report, and flags the cached
  Lufthansa report as stale (`lib/reportCache.ts`) so the report page shows "Tasks changed…" with Regenerate.
- **Task date**: `createdAt` field (default today, not in the future) on create and edit; the backend resolves "today" in
  Europe/Warsaw.
- **Shortcuts**: `useSubmitShortcut` binds Ctrl/⌘+Enter to the primary action of the current step (register, confirm,
  add another, save dialog); hints via `<ShortcutHint>`.
- **Errors** (`api/client.ts`): backend body `{status,title,description}` → `ApiError`. 404 **with** body = AI rejection (shown
  inline as "AI could not accept this task"); 404 **without** body = empty report (`isEmptyResult` → empty state).
  `204` resolves to `undefined` (DELETE).
- **Lufthansa report** costs OpenAI calls on every GET → query is `enabled: false`, fetched only by the Generate/Regenerate
  button, cached in `localStorage` (`reporter.lufthansa.report.YYYY-MM`). Trecom report is cheap and loads automatically.
- **Suggestions**: no dictionary endpoint, so customers/salesmen are remembered in `localStorage`
  (`reporter.trecom.suggestions`) after a save and learned from fetched reports.
- **Trecom `notes`** are AI-corrected and persisted since backend 1.1.0; the review step shows them like the description.
- Validation mirrors the backend DTOs (`features/*/schema.ts`, `lib/validation.ts`) — keep them in sync with `docs/backend-api.md`.

## Deployment

Image `magikabdul/reporter-frontend` (multi-stage: node build → `nginx-unprivileged`, port 8080, `/healthz`, SPA fallback).
nginx serves static files only; the Ingress routes `/reporter` to the backend service, so API calls are same-origin.

```bash
docker build -t magikabdul/reporter-frontend:<version> frontend/
kubectl apply -f k8s/frontend/          # from the repo root: certificate, deployment+service, ingress (namespace krisoo)
```

Releases: GitHub Actions `Frontend Release` (`workflow_dispatch`, patch/minor/major) bumps `package.json`, pins the new tag in
`../k8s/frontend/deployment.yaml`, pushes the image (+`latest`), tags `frontend-vX.Y.Z`. Applying manifests to the cluster stays manual.
URL: https://reporter.home.cholewa.dev (needs a local DNS record → 10.78.20.201).
