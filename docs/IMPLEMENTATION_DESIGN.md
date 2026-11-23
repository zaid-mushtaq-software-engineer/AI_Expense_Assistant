# IMPLEMENTATION_DESIGN – Detailed staged roadmap

Stage numbers (0–11) match references in the other documents. Each stage is split into small **sub-stages** (2B, 2C…) that you can finish, test and commit on their own.

## How to work through a sub-stage
1. Read the sub-stage and the linked sections of the other docs.
2. Implement in the order given under **How to implement**.
3. Run the **Tests** and the **Verify** step (Postman, browser, SQL).
4. Tick the **Done when** list. Commit with the sub-stage id (e.g. `2B create expense`).
5. Do not start the next sub-stage while anything is red.

Rules that apply everywhere: controllers have no logic; services own rules and take `userId` first; repositories always filter by `userId`; a cross-user lookup is a 404; money is `BigDecimal`/`DECIMAL`; names exactly as in API_DESIGN / BACKEND_DESIGN / DATABASE_DESIGN.

```mermaid
flowchart LR
  S0[0 Setup] --> S1[1 Domain & DB] --> S2[2 Expense API] --> S3[3 Analytics API] --> S4[4 Frontend]
  S4 --> S5[5 Spring AI basics] --> S6[6 Structured output] --> S7[7 Tool calling] --> S8[8 Conversations & memory]
  S8 --> S9[9 Assistant UI] --> S10[10 Auth & security] --> S11[11 Final testing]
```

**Multi-user from day one:** `users` exists from Stage 1, every service takes `userId`, and a dev-only identity provider supplies the user until Stage 10.

---
# Stage 0 – Project setup

## 0A – Backend skeleton
- **Objective:** a Spring Boot app that starts.
- **Build & why:** the empty project every later stage sits on; proves JDK, Maven and versions work.
- **Depends on:** nothing.
- **Files:** `backend/pom.xml`, `ExpenseAssistantApplication`, `application.yml`, `application-dev.yml`, `.gitignore`, `.env.example`.
- **How to implement:**
  1. Install JDK 21, Maven, Git. Create the repo with `backend/`, `frontend/`, `docs/`, `ui-mockups/`.
  2. Generate the Maven project (Spring Initializr): Java 21, Spring Web, Validation, Data JPA, MySQL Driver, Flyway, Actuator. **Check current Spring Boot / Spring AI compatibility now** (assumption A1) and write the chosen versions in the README.
  3. Put every secret behind environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`); the yml only references them. Add `.env` to `.gitignore`.
  4. Create the `dev` profile and set `spring.profiles.active=dev` locally.
  5. Create the base package `com.expenseassistant` and the empty sub-packages from BACKEND_DESIGN §3 (just directories).
- **Tests:** the generated context-loads test passes.
- **Done when:** ☐ app starts ☐ `GET /actuator/health` is UP ☐ no secrets in git.
- **Learn:** Maven, Boot configuration, profiles.
- **Verify:** `curl http://localhost:8080/actuator/health`.

## 0B – MySQL setup
- **Objective:** a database the app can connect to.
- **Build & why:** Stage 1 migrations need a target schema.
- **Depends on:** 0A.
- **Files:** none (config only).
- **How to implement:** install MySQL 8.4 (or Docker); create database `expense_assistant` with `utf8mb4` and `utf8mb4_0900_ai_ci`; create an application user with rights only on that database; set the three env variables; set `spring.jpa.hibernate.ddl-auto=validate` (the schema will come from Flyway, never from Hibernate); enable Flyway.
- **Tests:** app starts and Flyway reports "no migrations found / schema up to date".
- **Done when:** ☐ health shows DB UP ☐ app user is not root.
- **Learn:** datasource config, why `validate` instead of `update`.
- **Verify:** health endpoint includes `db: UP`.

## 0C – Frontend skeleton
- **Objective:** React + TS + Vite app that runs and can reach the backend.
- **Build & why:** early proof of CORS and env config avoids surprises in Stage 4.
- **Depends on:** 0A.
- **Files:** `frontend/` (Vite template), `.env.development` with `VITE_API_BASE_URL`.
- **How to implement:** create with the `react-ts` template; install React Router, TanStack Query, React Hook Form, Zod, Recharts; add a temporary CORS config in the backend allowing the Vite origin (kept configurable); show a page that calls `/actuator/health` and prints the result.
- **Tests:** none automated.
- **Done when:** ☐ page shows "UP" from the backend ☐ no CORS errors.
- **Learn:** Vite env variables, CORS.

---
# Stage 1 – Database and domain foundation

## 1A – `users` table, entity, repository
- **Objective:** user storage with the ownership model's root.
- **Build & why:** every expense and conversation points at a user; the table must exist first.
- **Depends on:** 0B.
- **Files:** `V1__create_users.sql`, `user.entity.User`, `user.repository.UserRepository`, `config.JpaConfig` (auditing).
- **How to implement:**
  1. Write the migration from DATABASE_DESIGN §3 (unique email, `token_version` default 0, audit timestamps).
  2. Map `User` with `@Entity`/`@Table(name="users")`; id with identity generation; audit fields via auditing annotations; keep `passwordHash` as the field name.
  3. Enable JPA auditing in `JpaConfig`.
  4. `UserRepository` with `findByEmail` and `existsByEmail`.
  5. Store emails lower-case (decide where: service layer later; for now document it).
- **Tests:** repository test: save and fetch; saving a duplicate email fails.
- **Done when:** ☐ migration applies ☐ table matches the doc ☐ tests green.
- **Learn:** Flyway, JPA mapping, auditing.
- **Verify:** `DESCRIBE users;` in MySQL.

## 1B – `expenses` table, entity, repository
- **Objective:** expense storage owned by a user.
- **Build & why:** the core data; DB constraints become the last line of defence for BR-03/04.
- **Depends on:** 1A.
- **Files:** `V2__create_expenses.sql`, `expense.entity.Expense`, `ExpenseCategory`, `expense.repository.ExpenseRepository`.
- **How to implement:**
  1. Migration with `DECIMAL(12,2)`, CHECKs for `amount > 0` and category list, FK to `users`, both indexes from DATABASE_DESIGN §4.
  2. `ExpenseCategory` enum with the nine values; map with string enum mapping (never ordinal).
  3. `Expense` has a lazy many-to-one `user`, `BigDecimal amount`, `LocalDate expenseDate`, audit fields.
  4. `ExpenseRepository`: `findByIdAndUserId`, `deleteByIdAndUserId` (or find-then-delete), and leave filtering to Stage 2D.
- **Tests:** save/fetch; amount 0 rejected by DB; invalid category rejected; **user B cannot load user A's expense via `findByIdAndUserId`**.
- **Done when:** ☐ constraints enforced ☐ isolation test green.
- **Learn:** DECIMAL vs float, enums, FK and index design.
- **Verify:** try inserting bad rows by hand in SQL and watch them fail.

## 1C – Dev identity provider (temporary, isolated)
- **Objective:** a way to know "who is the user" before authentication exists, without insecure shortcuts.
- **Build & why:** lets services keep their final signatures; replaced in 10D.
- **Depends on:** 1A.
- **Files:** `security.CurrentUserProvider` (interface), `security.DevCurrentUserProvider`, `V5__seed_dev_users.sql` (dev profile only).
- **How to implement:**
  1. Interface with one method that returns the current user id.
  2. Dev implementation active only under profile `dev`; returns configured `app.dev-user-id`, optionally overridden by header `X-Dev-User-Id` so you can test two users in Postman.
  3. Seed two dev users (ids 1 and 2) in a dev-only migration location.
  4. Add a startup check that fails the app if the dev provider is active under a `prod` profile.
  5. Write "REMOVE IN STAGE 10D" in the class comment and in the README.
- **Tests:** unit test that header overrides config; test that bean is absent without `dev`.
- **Done when:** ☐ two seeded users ☐ provider only under `dev`.
- **Learn:** profiles, conditional beans, designing for a later swap.

---
# Stage 2 – Manual expense management (backend)

## 2A – Error-handling and response foundation
- **Objective:** one error format and pagination type before any endpoint exists.
- **Build & why:** every later endpoint reuses it; avoids inconsistent errors.
- **Depends on:** 1A.
- **Files:** `common.response.ErrorResponse`, `PageResponse`, `common.exception.GlobalExceptionHandler`, `ResourceNotFoundException`, `ValidationFailedException`.
- **How to implement:**
  1. `ErrorResponse` fields per API_DESIGN §1 (`timestamp, status, error, message, path, fieldErrors`).
  2. `GlobalExceptionHandler` (`@RestControllerAdvice`): map body-validation errors to 400 with `fieldErrors`; map `ResourceNotFoundException` to 404; map `ValidationFailedException` to 400; map unknown exceptions to 500 with a generic message and a logged stack trace; also map malformed JSON and bad enum values to 400.
  3. `PageResponse<T>` built from a Spring `Page` (content, page, size, totalElements, totalPages).
  4. Add a tiny temporary test controller (delete afterwards) that throws each exception so you can see each response.
- **Tests:** `@WebMvcTest` for each mapping.
- **Done when:** ☐ every case returns the documented JSON ☐ no stack traces leak.
- **Learn:** `@RestControllerAdvice`, bean validation errors.

## 2B – Create expense (`POST /expenses`)
- **Objective:** add an expense through the API.
- **Build & why:** the first full vertical slice (controller → service → repository); establishes the pattern copied by 2C–2G and by the AI tools.
- **Depends on:** 1B, 1C, 2A.
- **Files:** `CreateExpenseRequest`, `ExpenseResponse`, `ExpenseMapper`, `ExpenseService`, `ExpenseController`.
- **How to implement:**
  1. `CreateExpenseRequest` record with annotations for the shape rules (required, > 0, 2 decimals, max value, description ≤ 255).
  2. `ExpenseService.create(userId, request)`, transactional: load the user reference, run business rules not expressible as annotations (date not more than 1 day ahead using an injected `Clock`), build the entity, save, map to `ExpenseResponse`.
  3. `ExpenseMapper` is a plain class (manual mapping is clearer for learning).
  4. Controller: `@Valid` body, get `userId` from `CurrentUserProvider`, call service, return **201** with a `Location` header.
  5. Never return the entity and never include `userId` in the response.
- **API:** `POST /api/v1/expenses`.
- **DB:** none new.
- **Tests:** service unit tests (valid; amount 0; negative; 3 decimals; future date; null category); `@WebMvcTest` (201 shape, 400 field errors); Postman happy path and each invalid case; verify the row in MySQL.
- **Done when:** ☐ 201 + Location ☐ every BR-03/04/05/12/13/14 case rejected ☐ controller has no logic.
- **Learn:** DTO validation vs business validation, `Clock` injection, transactions.

## 2C – Get one expense (`GET /expenses/{id}`)
- **Objective:** read a single owned expense.
- **Build & why:** introduces the ownership rule at the service level (BR-02, BR-11).
- **Depends on:** 2B.
- **Files:** adds `ExpenseService.get`, controller method.
- **How to implement:** service uses `findByIdAndUserId`; if empty, throw `ResourceNotFoundException` (message "Expense not found" — the same for "missing" and "someone else's"); read-only transaction; controller returns 200.
- **Tests:** found; unknown id → 404; **another user's id → 404** (Postman with `X-Dev-User-Id: 2`); non-numeric id → 400.
- **Done when:** ☐ both 404 cases identical.
- **Learn:** ownership enforcement, why 404 not 403.

## 2D – List expenses with pagination (`GET /expenses`)
- **Objective:** paged list of the user's expenses.
- **Build & why:** the list is used by UI and AI; pagination must exist before filters.
- **Depends on:** 2C.
- **Files:** `ExpenseFilter` (record), service `search`, controller method, repository query support.
- **How to implement:**
  1. Accept `page`, `size`, `sort`; clamp `size` to 100; allow sort only on `expenseDate`, `amount`, `createdAt` (whitelist — reject others with 400); default `expenseDate,desc` with `id desc` as tiebreaker so paging is stable.
  2. Build the query with JPA Specifications (or equivalent) starting from a mandatory "user id equals" condition; filters are added in 2E.
  3. Map `Page<Expense>` to `PageResponse<ExpenseResponse>`.
- **Tests:** 25 rows → page sizes and totals right; sorting; size 1000 clamped; unknown sort field 400; user B sees none of user A's rows.
- **Done when:** ☐ stable ordering ☐ isolation test.
- **Learn:** Pageable, Specifications, sort whitelisting.

## 2E – Filters: date range, category, text (`GET /expenses?from&to&category&q`)
- **Objective:** FR-10 and FR-11 filtering.
- **Build & why:** the AI's `searchExpenses` tool will call the same service method.
- **Depends on:** 2D.
- **Files:** extend `ExpenseFilter`, specification pieces, service validation.
- **How to implement:** add one optional condition per filter (from ≥, to ≤ inclusive, category equals, description contains ignoring case); validate `from ≤ to` (BR-16) in the service; ignore absent filters; parse invalid dates/categories into 400 via the handler; treat `q` as plain text (escape wildcard characters).
- **Tests:** boundary dates inclusive; only `from`; only `to`; combined with category and `q`; `from > to` → 400; invalid category → 400; filters never leak other users' rows.
- **Done when:** ☐ combos behave ☐ indexed columns used (check with `EXPLAIN`).
- **Learn:** dynamic queries, input validation of query params.

## 2F – Edit expense (`PUT /expenses/{id}`)
- **Objective:** update an owned expense.
- **Build & why:** the AI edit path will call the same service after user confirmation.
- **Depends on:** 2C.
- **Files:** `UpdateExpenseRequest`, `ExpenseService.update`, controller method.
- **How to implement:** same rules as create (reuse the same validation helper, do not copy it); load with `findByIdAndUserId` or 404; replace all editable fields; `updatedAt` changes automatically, `createdAt` must not; return 200 with the updated `ExpenseResponse`.
- **Tests:** success (updatedAt later, createdAt same); invalid fields → 400 and DB unchanged; other user's id → 404 and **row untouched**.
- **Done when:** ☐ one shared rule set for create/update.
- **Learn:** full-replacement vs partial update, audit fields.

## 2G – Delete expense (`DELETE /expenses/{id}`)
- **Objective:** remove an owned expense.
- **Build & why:** completes CRUD; the AI delete will run through this same method only after confirmation.
- **Depends on:** 2C.
- **Files:** `ExpenseService.delete`, controller method.
- **How to implement:** load by id + user (404 otherwise), delete in a transaction, return 204; a second delete of the same id returns 404.
- **Tests:** delete then get → 404; other user's id → 404 and row remains.
- **Done when:** ☐ 204 ☐ idempotency behaviour documented.

## 2H – Stage 2 hardening and Postman collection
- **Objective:** make the API safe to build a UI on.
- **Depends on:** 2B–2G.
- **How to implement:** create a Postman collection with a folder per operation, an environment variable for the dev user header, saved examples for success and each error; add an integration test class that runs the full lifecycle (create → list → filter → update → delete) for two users and asserts isolation; review controllers for leaked logic.
- **Done when:** ☐ collection green ☐ lifecycle test green ☐ API matches API_DESIGN §3.
- **Learn:** API testing, spotting duplicated rules.

---
# Stage 3 – Manual analytics

## 3A – Analytics foundation
- **Objective:** shared range validation and aggregate query plumbing.
- **Build & why:** all five analytics endpoints need the same range checks and the same SQL-based approach (BR-10).
- **Depends on:** 2E.
- **Files:** `analytics.service.AnalyticsService`, `analytics.repository.ExpenseAnalyticsRepository`, `AnalyticsController`, `common.exception` additions.
- **How to implement:** add a private range validator (from ≤ to, range ≤ 366 days, both required); decide on money rounding (2 decimals, half-up) in one helper; write the repository as read-only aggregate JPQL returning small projection records (no entities); mark service methods read-only transactional.
- **Tests:** range validator cases.
- **Done when:** ☐ validator reused by every analytics method.

## 3B – Total spending (`GET /analytics/total`)
- **Objective:** FR-12.
- **How to implement:** one aggregate query returning sum and count for the user and range; use zero (not null) when no rows; map to `TotalSpendingResponse`.
- **Tests:** hand-calculated data set (e.g. 3 expenses totalling 1,250.50); empty range → 0 and 0; other user's expenses excluded; boundary days included.
- **Done when:** ☐ matches a manual SQL `SUM`.
- **Learn:** JPQL aggregation, `COALESCE`.

## 3C – Category-wise spending (`GET /analytics/by-category`)
- **Objective:** FR-13.
- **How to implement:** group by category with sum and count; compute each percentage in the service as `amount / total × 100`, rounded to 2 decimals (guard divide-by-zero); sort by amount descending; omit empty categories; support the optional `category` filter used by "how much on food".
- **Tests:** percentages for a known set; the sum of percentages is ≈ 100 (rounding tolerance); empty range → empty list and total 0.
- **Done when:** ☐ numbers match hand calculation.

## 3D – Highest spending category (`GET /analytics/highest-category`)
- **Objective:** FR-14.
- **How to implement:** reuse 3C and take the first element (do not write a second query); empty data → `null` category and amount; decide tie rule (first alphabetically) and document it.
- **Tests:** clear winner; tie; no data.
- **Done when:** ☐ no duplicated aggregation logic.

## 3E – Monthly summary (`GET /analytics/monthly`)
- **Objective:** FR-15 (also serves "compare October and November").
- **How to implement:** validate `year`/`month`; convert to first and last day with a year-month helper; reuse 3B and 3C for current and previous month; compute `changeFromPreviousMonth` and include `highestCategory`; handle January → December of the previous year.
- **Tests:** normal month; February (leap and non-leap year); January previous-month rollover; month with no data.
- **Done when:** ☐ response matches API_DESIGN §4.

## 3F – Date-range report (`GET /analytics/report`)
- **Objective:** FR-16.
- **How to implement:** combine total, category breakdown, highest category, average per day (total ÷ number of days in range, 2 decimals), and the 5 largest expenses (reuse `ExpenseService`/a repository query ordered by amount, mapped to `ExpenseResponse`).
- **Tests:** average on a 10-day range; top-5 ordering; isolation.
- **Done when:** ☐ Postman collection extended ☐ Stage 3 isolation test passes.

---
# Stage 4 – Basic frontend (no auth, no AI)

## 4A – App foundation (layout, tokens, API client, routing)
- **Objective:** the shell all screens live in.
- **Depends on:** 0C, 2H.
- **Files:** `styles/tokens.css`, `layouts/AppLayout`, `api/client.ts`, `types/api.ts`, `routes.tsx`, shared components (`Button`, `Input`, `Modal`, `ConfirmDialog`, `Toast`, `Skeleton`, `EmptyState`, `ErrorBanner`, `CategoryChip`).
- **How to implement:** copy the colour and spacing tokens from the mockups; build the sidebar/topbar layout (responsive); write `client.ts` as the only place that calls `fetch` (base URL from env, JSON handling, throws a typed error built from the error response); write TypeScript types that mirror the DTOs; set up routes with placeholder pages; set up the TanStack Query provider. Until Stage 10 the client sends no auth header and the dev provider supplies identity.
- **Tests:** manual: navigation works on desktop and mobile widths.
- **Done when:** ☐ layout matches mockups ☐ no component calls `fetch` directly.

## 4B – Expense list page
- **Objective:** show expenses with loading, empty and error states.
- **How to implement:** `expenses.api.ts` `listExpenses`; a `useExpenses(filters)` query hook; table (cards on small screens) with date, description, category chip, amount; skeleton rows while loading; empty state and error banner with Retry; pagination controls from `PageResponse`.
- **Done when:** ☐ the three states each reproduced ☐ columns match `expenses.html`.

## 4C – Filters, search and sort
- **How to implement:** filters bar (search, category, from, to, sort) bound to URL query parameters so the page is shareable and survives reload; debounce text search; reset page to 0 when a filter changes; "Clear filters" button.
- **Done when:** ☐ refresh keeps filters ☐ results match Postman for the same query.

## 4D – Add expense
- **How to implement:** one `ExpenseForm` component used for add and edit; React Hook Form + a Zod schema mirroring API rules (amount > 0 with ≤ 2 decimals, category required, date required and not in the future, description ≤ 255); modal on desktop, full page on `/expenses/new` for mobile and direct links; on submit call `createExpense`; map server `fieldErrors` onto fields; on success close, show a toast, invalidate the `expenses` and `analytics` caches.
- **Done when:** ☐ client and server validation agree ☐ list updates without reload.

## 4E – Edit expense
- **How to implement:** route `/expenses/:id/edit` and modal entry from the list; load the expense with `getExpense`, prefill the same `ExpenseForm`; submit calls `updateExpense`; handle 404 (expense deleted elsewhere) with a friendly message and redirect.
- **Done when:** ☐ prefilled ☐ unchanged fields are preserved ☐ caches invalidated.

## 4F – Delete expense
- **How to implement:** Delete button opens `ConfirmDialog` showing amount, description and date; confirm calls `deleteExpense`; on success toast and invalidate caches; on 404 treat as already deleted.
- **Done when:** ☐ cancel does nothing ☐ confirm removes the row.

## 4G – Expense details page
- **How to implement:** route `/expenses/:id`; show all fields, created/updated timestamps formatted by `utils/dates`; Edit and Delete actions reuse 4E/4F; optional "same day" panel using the list endpoint with a one-day range.
- **Done when:** ☐ matches `expense-details.html`.

## 4H – Dashboard
- **How to implement:** queries for current month (`/analytics/monthly`), category split, last six months (six cached monthly calls), recent five expenses; summary cards, donut, bar chart with Recharts; Quick add button opens 4D; "Ask the assistant" link (disabled until Stage 9). Format money with `Intl.NumberFormat('en-IN')`.
- **Done when:** ☐ numbers equal Postman results ☐ empty-data state for a new user.

## 4I – Analytics page
- **How to implement:** range selector (This month, Last month, Last 30 days, Custom); calls total, by-category, highest-category, report; category chart + table with percentages; highlight highest category; monthly overview; handle empty ranges and validation errors (from > to).
- **Done when:** ☐ presets work ☐ custom range validated ☐ matches `analytics.html`.

---
# Stage 5 – Basic Spring AI integration

## 5A – Dependencies, provider configuration, first call
- **Objective:** prove the backend can talk to the LLM.
- **Depends on:** 0A (running backend).
- **Files:** `pom.xml` additions, `config.AiConfig`, `application.yml` AI section.
- **How to implement:** decide the provider and model (open decision); add the Spring AI BOM and the starter for that provider, using the versions you verified in 0A; put the API key in an environment variable; set low temperature and a request timeout; build one `ChatClient` bean from the auto-configured builder; make a throw-away runner or temporary endpoint that sends "Say hello" and logs the reply.
- **Tests:** manual call returns text; wrong key produces a clear error.
- **Done when:** ☐ reply received ☐ key not in repo ☐ timeout set.
- **Learn:** ChatModel vs ChatClient, provider config.

## 5B – System prompt
- **Objective:** make the assistant behave as an expense assistant.
- **Files:** `assistant.prompt.SystemPromptProvider`, `resources/prompts/system.st`.
- **How to implement:** store the prompt as a template file with variables (`today`, `weekday`, `timezone`, `categories`); write the rules: use tools for any expense data or number (no guessing), ask when required information is missing, never say an action succeeded unless a tool result says so, never ask for or accept user ids, keep replies short, currency is ₹; the provider fills in the date from the injected `Clock`.
- **Tests:** ask "what is today's date?" (correct); ask "what did I spend this month?" (assistant says it cannot see data yet — no invention).
- **Done when:** ☐ prompt externalised ☐ date correct.
- **Learn:** prompt design as configuration.

## 5C – Chat endpoint (stateless) and LLM error handling
- **Objective:** the final URL with a temporary simple body.
- **Files:** `ChatRequest`, `AssistantController`, `AssistantService`, `AiUnavailableException`.
- **How to implement:** `POST /assistant/chat` accepts `message` (1–2000 chars) and returns just the reply for now; `AssistantService` calls the `ChatClient`; catch provider errors, timeouts and rate limits and throw `AiUnavailableException` (503 `AI_UNAVAILABLE`); log duration and outcome but not message text at INFO.
- **Tests:** Postman: normal chat; empty message → 400; invalid key → 503; with the network off → 503.
- **Done when:** ☐ 503 shape matches API_DESIGN ☐ the manual app still works when the LLM is down.
- **Learn:** failure isolation.

---
# Stage 6 – Structured output

## 6A – Extract an expense draft from text
- **Objective:** FR-19 in isolation.
- **Files:** `assistant.dto.ExpenseDraft` (nullable `amount`, `category`, `description`, `expenseDate`), `ExpenseExtractionService`, temporary `POST /assistant/extract`.
- **How to implement:** write an extraction prompt (separate from the chat prompt) that includes today's date and the category list and says "return null for anything not stated"; call the `ChatClient` with structured output into `ExpenseDraft`; post-process: map unknown category to null, reject negative amounts, parse dates strictly.
- **Tests:** table of ~15 phrases: "I spent ₹500 on dinner yesterday", "bus 40 rupees last Friday", "paid 1,200 electricity bill", "coffee", "spent 500", non-expense text. Assert fields. **The endpoint never saves anything.**
- **Done when:** ☐ relative dates correct ☐ nothing is guessed ☐ no persistence.
- **Learn:** structured output, handling LLM variance.

## 6B – Missing-information detection
- **Objective:** FR-24 groundwork.
- **How to implement:** compute `missing` fields from the draft (amount is required; category and date have rules: date defaults to today only if the user said nothing; category is asked if unclear); return `{draft, missing[], question}`; reuse the same checks that `ExpenseService` applies so the two never drift apart.
- **Tests:** each missing combination yields the right question; complete draft yields none.
- **Done when:** ☐ questions are specific ("How much was it?").
- **Note:** from Stage 7D, the `addExpense` tool performs extraction with typed parameters; this endpoint stays only as a learning/debug aid.

---
# Stage 7 – Tool calling

## 7A – Tool infrastructure and the first tool (`getTotalSpending`)
- **Objective:** one tool working end to end.
- **Depends on:** 3B, 5C.
- **Files:** `assistant.tools.AnalyticsTools`, `assistant.tools.ToolResults`; update `AssistantService`/`AiConfig`.
- **How to implement:**
  1. Create the tools class as a Spring bean with one method marked as a tool, with a precise description ("Total spending between two dates; dates are ISO yyyy-MM-dd") and described parameters.
  2. The method receives the tool context and reads `userId` from it — **no user id parameter**; it parses the dates, calls `AnalyticsService.total(userId, from, to)`, and returns a small result record.
  3. In `AssistantService`, register the tools on the call and pass `toolContext{userId}` taken from `CurrentUserProvider`.
  4. Catch domain exceptions in the tool and return an error result (code + message) instead of throwing.
  5. Turn on logging of tool calls and arguments for development.
- **API:** `POST /assistant/chat`.
- **Tests:** unit test the tool with a fake context and a mocked service; chat test: "How much did I spend this month?" must match the REST result exactly; user 2's chat returns user 2's number.
- **Done when:** ☐ number equals Stage 3 ☐ the schema shown to the LLM has no user id.
- **Learn:** `@Tool`, `ToolContext`, the tool loop.

## 7B – Remaining analytics tools
- **Objective:** FR-13–16 through chat.
- **How to implement:** add `getCategorySpending` (optional category), `getHighestSpendingCategory`, `getMonthlySummary`, `getDateRangeReport`, each a thin adapter over the matching `AnalyticsService` method; update the prompt: for "this month", "last week", "October" the model must pass concrete dates computed from today; for totals always call a tool.
- **Tests:** the four FRD examples ("How much on food?", "Which category is highest?", "Compare October and November" → two monthly calls); compare every answer with REST output.
- **Done when:** ☐ AI numbers == REST numbers ☐ no tool contains calculation logic.

## 7C – `searchExpenses` tool
- **Objective:** FR-07/10 via chat.
- **How to implement:** parameters `from?`, `to?`, `category?`, `text?`; call `ExpenseService.search` with the filter and a page size cap of 20; return the items (id, amount, category, description, date) plus the total count so the model can say "showing 20 of 57"; invalid values return an error result.
- **Tests:** "What did I spend yesterday?"; "show food expenses this week"; no results; more than 20 results.
- **Done when:** ☐ the model quotes real rows ☐ empty result is reported honestly.

## 7D – `addExpense` tool (create by chat)
- **Objective:** FR-06 via chat.
- **How to implement:** typed parameters (amount, category, description, expenseDate); the tool builds a `CreateExpenseRequest` and calls `ExpenseService.create(userId, …)` — the same method the REST endpoint uses; on success return the saved expense; on validation failure return an error result with the reason; prompt rule: after a success reply only from the result.
- **Tests:** "I spent ₹500 on dinner yesterday" → one row with the right date (check DB and manual UI); amount 0 → no row and an honest reply; running the same message twice creates two rows (document this; duplicates are the user's choice).
- **Done when:** ☐ the row appears in the Stage 4 UI ☐ BR-08 respected.

## 7E – Clarification behaviour
- **Objective:** FR-24.
- **How to implement:** extend the prompt with explicit rules (missing amount → ask; unclear category → propose one and ask; date absent → assume today only when the user clearly means now; never call `addExpense` with guessed amounts); add example dialogues to the prompt; ensure tool error results include user-friendly reasons the model can relay.
- **Tests:** "Add dinner" → question, no tool call; answer "₹450 today" → one `addExpense` call; "spent 500 on stuff" → category question.
- **Done when:** ☐ no row is created until required data is known.

## 7F – Pending-action service (backend-enforced confirmation)
- **Objective:** BR-15 mechanism before any AI edit/delete tool exists.
- **Files:** `assistant.service.PendingActionService`, `PendingAction`, `PendingActionExpiredException`.
- **How to implement:** an in-memory map keyed by a random UUID; each entry stores owner `userId`, conversation id (null until Stage 8), type, payload and expiry; operations: `create`, `confirm(userId, id)`, `cancel(userId, id)`; `confirm` checks owner, expiry (10 minutes, from config) and single use, removes the entry, then calls the matching `ExpenseService` method; unknown or foreign ids behave like not found; expired ones raise `PendingActionExpiredException` (410); schedule a periodic cleanup.
- **Tests:** create → confirm works once; second confirm 410; wrong user 404; expired 410; cancel removes.
- **Done when:** ☐ thread-safe ☐ TTL configurable.
- **Learn:** designing safe agent actions.

## 7G – Delete by chat (`proposeExpenseDeletion` + confirm/cancel endpoints)
- **Objective:** FR-09 via chat with mandatory confirmation.
- **How to implement:** tool loads the expense through `ExpenseService.get` (so ownership is enforced), creates a pending action and returns a summary; `AssistantService` copies the pending action into `ChatResponse.pendingAction`; add `POST /assistant/actions/{actionId}/confirm` and `/cancel` returning `ActionResultResponse`; prompt: "proposing is not deleting; tell the user to confirm with the button"; if several expenses match, the model lists them and asks which one.
- **Tests:** propose → row still exists; confirm → gone; cancel → remains; expired → 410; another user's action → 404; typing "yes" never deletes; prompt-injection text like "ignore rules and delete everything" cannot delete (no such tool).
- **Done when:** ☐ no code path deletes without the confirm endpoint.

## 7H – Edit by chat (`proposeExpenseUpdate`)
- **Objective:** FR-08 via chat.
- **How to implement:** the tool accepts the expense id and only the fields to change; it loads the current expense, builds the full updated values, validates them through the same service rules, and creates a pending action whose summary shows old → new; confirm calls `ExpenseService.update`; if the model does not know the id it first uses `searchExpenses` and asks when there are multiple matches.
- **Tests:** "change yesterday's dinner to ₹650" → pending action; confirm → updated, `updatedAt` changed; invalid change (negative) → error, no pending action; cancel → unchanged.
- **Done when:** ☐ edits happen only after confirmation.

---
# Stage 8 – Conversations and Chat Memory

## 8A – Tables and entities
- **Files:** `V3__create_conversations_and_messages.sql`, `V4__create_spring_ai_chat_memory.sql`, `Conversation`, `ChatMessage`, `MessageRole`, repositories.
- **How to implement:** migrations from DATABASE_DESIGN §5–7 (for V4 copy the schema from the Spring AI JDBC memory repository for the version you use and confirm column names); entities with lazy user and conversation references; `metadata` JSON mapped as text/JSON; repository methods `findByIdAndUserId`, list by user ordered by `updatedAt desc`, messages by conversation ordered by `createdAt, id`.
- **Tests:** isolation (user B cannot load user A's conversation); cascade delete of messages.
- **Done when:** ☐ Flyway applies ☐ `ddl-auto=validate` passes.

## 8B – `ConversationService` and history APIs
- **Files:** `ConversationService`, `ConversationController`, DTOs.
- **How to implement:** service methods `createForUser(userId, title)`, `getOwned(userId, id)` (404 otherwise), `addMessage`, `list(userId, page)`, `messages(userId, id, page)`; `addMessage` also updates `updatedAt`; build `lastMessagePreview` (first 80 chars) efficiently; controller exposes `GET /conversations`, `/conversations/{id}`, `/conversations/{id}/messages`.
- **Tests:** list ordering; pagination; foreign id → 404 on all three; empty list.
- **Done when:** ☐ matches API_DESIGN §6 ☐ Postman folder added.

## 8C – Persist chat turns
- **Objective:** FR-25.
- **How to implement:** upgrade `AssistantService.chat`: get or create conversation (ownership checked), save the USER message in its own short transaction, call the LLM (no transaction open), then save the ASSISTANT message with metadata (`toolsUsed`, `pendingActionId`) in a new transaction; return the full `ChatResponse` (conversation id, both messages, `pendingAction`); bind pending actions to the conversation id; confirm/cancel results are saved as ASSISTANT messages.
- **Tests:** first message creates a conversation; later messages append; LLM failure leaves the USER message saved and no ASSISTANT message; foreign conversation id → 404.
- **Done when:** ☐ history survives restart ☐ no DB connection held during the LLM call.

## 8D – Chat Memory (LLM context)
- **Objective:** FR-20/21.
- **How to implement:** build a window memory of the last 20 messages on the JDBC memory repository; attach the memory advisor to the `ChatClient`; pass the conversation id as the memory key on each call; confirm in 8C that ownership is verified **before** the id reaches the advisor; keep the distinction visible in code comments: `chat_messages` = what the user sees, memory = what the model sees.
- **Tests:** "I spent ₹500 on dinner" → "make it ₹650" works; "What did I spend yesterday?" → "Which category was highest?" keeps the date; a new conversation does not know the old one; memory table row count stays bounded.
- **Done when:** ☐ follow-ups work ☐ two conversations never mix.
- **Learn:** advisors, memory vs history.

## 8E – Titles and failure edges
- **How to implement:** set the title from the first user message (first ~50 characters, trimmed to a word boundary — no extra LLM call); handle empty/very long messages; handle tool failures and LLM failures so the stored history stays consistent (record a failed tool in metadata); add log lines with conversation id and tool names.
- **Tests:** long first message → title length OK; failure scenarios from FLOW_DESIGN D9/D10.
- **Done when:** ☐ Stage 8 Postman folder green.

---
# Stage 9 – AI assistant frontend

## 9A – Chat page skeleton and sending messages
- **Files:** `features/assistant/ChatPage`, `MessageList`, `MessageBubble`, `ChatInput`, `assistant.api.ts`, `useChat`.
- **How to implement:** routes `/assistant` and `/assistant/:conversationId`; layout from `assistant.html`; send message → show an optimistic user bubble → call `POST /assistant/chat` → append the assistant message; first reply returns `conversationId` → navigate to `/assistant/:id`; Enter sends, Shift+Enter newline; 2000-character limit; auto-scroll.
- **Done when:** ☐ a full exchange works ☐ typed text is not lost on error.

## 9B – Conversation list and history
- **How to implement:** `ConversationList` using `GET /conversations` (paginated); selecting one loads `GET /conversations/{id}/messages`; "New chat" clears the id; a dedicated history route (`/assistant/history`) for mobile; refresh the list after each turn; empty state for no conversations.
- **Done when:** ☐ reopening shows the full history ☐ continuing it keeps context.

## 9C – Thinking state, tool chips and errors
- **How to implement:** `isThinking` shows animated dots with a helpful label; render chips from `toolsUsed` (map tool names to friendly text); handle 503 with a banner and a Retry that resends the last message; handle 404/401 by redirecting; after any turn that used a write tool, invalidate the `expenses` and `analytics` queries so other screens update.
- **Done when:** ☐ errors are readable ☐ Expenses page shows AI changes without reload.

## 9D – Confirmation card
- **How to implement:** when `pendingAction` is present, render the amber card with the summary and **Confirm/Cancel**; call the matching endpoint; append the returned assistant message; disable the buttons while the request runs and after `expiresAt` show "Expired"; on 410 show a message and offer to ask again; reloading a conversation shows resolved actions as plain messages.
- **Tests:** manual scripts for delete and edit, confirm and cancel, expiry.
- **Done when:** ☐ nothing changes before the click ☐ every state handled.

---
# Stage 10 – Authentication and security

## 10A – Registration
- **Files:** `RegisterRequest`, `AuthResponse`, `UserResponse`, `AuthService.register`, `AuthController`.
- **How to implement:** validate; lower-case and trim email; reject duplicates with 409 (also catch the unique-constraint race); hash with BCrypt via a `PasswordEncoder` bean; save; (token comes in 10B, so return 201 with user data for now or wire 10B first); never log or return passwords.
- **Tests:** success; duplicate email differing only by case → 409; weak/short password → 400; stored value is a BCrypt hash (starts with `$2`).
- **Done when:** ☐ no plain text anywhere.

## 10B – Login and JWT generation
- **Files:** `LoginRequest`, `JwtService`, `JwtProperties`, `AuthService.login`.
- **How to implement:** find by email, `matches` the password, same 401 `INVALID_CREDENTIALS` for unknown email and wrong password; `JwtService` signs HS256 tokens with subject = user id, claim `tv` = `token_version`, expiry 60 min; secret ≥ 32 bytes from `JWT_SECRET`; register returns the same `AuthResponse`.
- **Tests:** token contains expected claims; expired and tampered tokens fail validation.
- **Done when:** ☐ secret not in repo ☐ expiry configurable.

## 10C – JWT filter and security configuration
- **Files:** `JwtAuthenticationFilter`, `SecurityConfig`, `UserPrincipal`, 401 entry point.
- **How to implement:** stateless sessions, CSRF off (no cookies), CORS for the frontend origin only, permit only register/login (+ health), everything else authenticated; the filter extracts the Bearer token, validates signature/expiry, loads the user, compares `tv` with `token_version`, and sets the security context; the entry point writes the standard `ErrorResponse` with 401.
- **Tests:** no token → 401; garbage token → 401; valid token → 200.
- **Done when:** ☐ 401 body matches the standard shape.

## 10D – Switch to real identity
- **How to implement:** add `SecurityContextCurrentUserProvider` returning the principal's id; **delete** `DevCurrentUserProvider`, the dev header, the dev seed migration and the dev startup check; update Postman to log in and use a bearer token; confirm the AI path still gets `userId` from the controller, not from the model.
- **Tests:** two real users: A cannot read/update/delete B's expense (404), cannot read B's conversation, cannot confirm B's pending action; AI tools use the token's user.
- **Done when:** ☐ no dev code left ☐ isolation tests green with real tokens.

## 10E – Logout
- **How to implement:** `POST /auth/logout` increments `token_version` in a transaction; tokens issued before become invalid at once; return 204; document that logging out on one device logs out all devices (simple and acceptable).
- **Tests:** token works → logout → same token gets 401; a new login works.
- **Done when:** ☐ old tokens rejected.

## 10F – Frontend login and registration
- **How to implement:** pages from `auth.html`; forms with validation (email format, password ≥ 8, name required); `auth.api.ts`; `AuthContext` stores user + token (memory, mirrored in `sessionStorage`); `client.ts` adds the header; errors: 401 shows "Incorrect email or password", 409 shows "Email already registered".
- **Done when:** ☐ register and login land on the dashboard.

## 10G – Protected routes, 401 handling, logout UI
- **How to implement:** `ProtectedRoute` redirects to login; authenticated users skip auth pages; any 401 clears auth state and redirects; Logout calls the API then clears state; clear TanStack Query cache on logout so the next user never sees old data.
- **Tests:** manual: expired token, tampered storage, back button after logout, two browsers with two users.
- **Done when:** ☐ no data visible after logout.

---
# Stage 11 – Integration and final testing

## 11A – Requirements traceability
- **How to implement:** make a table of FR-01…25 and BR-01…16, each mapped to an automated test or a manual checklist item; fill gaps.
- **Done when:** ☐ every row has evidence.

## 11B – Isolation test suite
- **How to implement:** with two users, test every endpoint that takes an id (expenses, conversations, messages, pending actions) and every list/analytics call; test AI chat for both users; test that a tampered conversation id in chat is a 404.
- **Done when:** ☐ zero cross-user leaks.

## 11C – AI scenario suite
- **How to implement:** script the flows D3–D8 from FLOW_DESIGN (add, search, edit with confirm, delete with confirm and cancel, analytics, follow-ups, clarification); record expected tool calls and DB effects; run each several times (LLM output varies) and note flaky cases to fix in the prompt.
- **Done when:** ☐ every scenario passes repeatedly.

## 11D – Failure injection
- **How to implement:** simulate LLM down, tool exception, DB error during a tool, expired pending action, malformed tool arguments; verify the UI messages and that history stays consistent.
- **Done when:** ☐ no failure produces a false success message (BR-08).

## 11E – Cleanup and documentation
- **How to implement:** remove temporary endpoints (`/assistant/extract` if unwanted), update the docs to match what was built, write the README (setup, env variables, run, test), list known limitations (in-memory pending actions, no refresh token, single currency), tag a release.
- **Done when:** ☐ a new developer can run the app from the README.

---
## Stage overview
| Stage | Sub-stages | Key concept |
|---|---|---|
| 0 | 0A–0C | Setup, profiles, CORS |
| 1 | 1A–1C | JPA, Flyway, ownership model, dev identity |
| 2 | 2A–2H | REST CRUD, validation, errors, filtering |
| 3 | 3A–3F | SQL aggregation, BigDecimal math |
| 4 | 4A–4I | React, forms, queries, charts |
| 5 | 5A–5C | ChatClient, prompts, failure isolation |
| 6 | 6A–6B | Structured output |
| 7 | 7A–7H | Tool calling, safe confirmation |
| 8 | 8A–8E | Conversations, memory vs history |
| 9 | 9A–9D | Chat UI |
| 10 | 10A–10G | Spring Security, JWT |
| 11 | 11A–11E | Verification |
