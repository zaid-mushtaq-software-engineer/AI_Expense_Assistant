# API_DESIGN – AI Expense Assistant

## 1. Conventions
| Topic | Convention |
|---|---|
| Base path | `/api/v1` (version in URL; breaking changes → `/api/v2`) |
| Format | JSON, `Content-Type: application/json`, UTF-8 |
| Success bodies | The resource itself (no wrapper envelope). Lists use `PageResponse<T>` |
| Dates | `LocalDate` as `yyyy-MM-dd`; timestamps ISO-8601 UTC, e.g. `2026-11-23T08:15:30Z` |
| Money | JSON number with 2 decimals, e.g. `500.00`; currency is implicitly INR |
| Enums | Upper-case strings (`FOOD`) |
| JWT transport | `Authorization: Bearer <token>` header. Not in cookies/URLs. Stored by SPA in memory (+ `sessionStorage` for refresh of page) |
| Auth | All endpoints need JWT except `register` and `login` |
| IDs | Numeric `Long` |
| Ownership | Other users' resources return **404** (BR-11) |

### Pagination
Query: `page` (0-based, default 0), `size` (default 20, max 100), `sort` (e.g. `expenseDate,desc`; allowed: `expenseDate`, `amount`, `createdAt`).
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 57, "totalPages": 3 }
```

### Global error response
```json
{
  "timestamp": "2026-11-23T08:15:30Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/expenses",
  "fieldErrors": [ { "field": "amount", "message": "must be greater than 0" } ]
}
```
`fieldErrors` is present only for validation errors.

| Status | `error` code | When |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Bad body/params |
| 401 | `UNAUTHORIZED` | Missing/invalid/expired token |
| 401 | `INVALID_CREDENTIALS` | Wrong email/password |
| 404 | `NOT_FOUND` | Missing or not owned |
| 409 | `EMAIL_ALREADY_EXISTS` | Registration conflict |
| 410 | `ACTION_EXPIRED` | Pending AI action expired/used |
| 500 | `INTERNAL_ERROR` | Unexpected |
| 503 | `AI_UNAVAILABLE` | LLM failed/timeout |

## 2. Authentication APIs

### POST `/auth/register`
Auth: none. Purpose: create account.
Request `RegisterRequest`:
```json
{ "name": "Asha Rao", "email": "asha@example.com", "password": "Str0ng#Pass1" }
```
Validation: `name` 1–100 chars; `email` valid, ≤255, unique (case-insensitive, stored lower-case); `password` 8–72 chars (BCrypt limit).
Response `201` `AuthResponse`:
```json
{ "accessToken": "eyJhbGciOi...", "tokenType": "Bearer", "expiresIn": 3600,
  "user": { "id": 101, "name": "Asha Rao", "email": "asha@example.com" } }
```
Errors: 400, 409.

### POST `/auth/login`
Auth: none. Request `LoginRequest`: `{ "email": "...", "password": "..." }`.
Response `200` `AuthResponse` (same as above). Errors: 400, 401 `INVALID_CREDENTIALS` (same message for unknown email and wrong password).

### POST `/auth/logout`
Auth: required. Body: none. Increments the user's `token_version` so existing tokens stop working. Response `204`. Errors: 401.

## 3. Expense APIs
Categories: `FOOD, TRANSPORTATION, SHOPPING, ENTERTAINMENT, HEALTH, EDUCATION, BILLS, TRAVEL, OTHER`.

`ExpenseResponse`:
```json
{ "id": 5012, "amount": 500.00, "category": "FOOD", "description": "Dinner",
  "expenseDate": "2026-11-22", "createdAt": "2026-11-23T08:15:30Z", "updatedAt": "2026-11-23T08:15:30Z" }
```
(The user reference is implicit; `userId` is not exposed.)

### POST `/expenses`
Auth: required. Request `CreateExpenseRequest`:
```json
{ "amount": 500.00, "category": "FOOD", "description": "Dinner", "expenseDate": "2026-11-22" }
```
Validation: `amount` required, > 0, ≤ 9999999.99, 2 decimals; `category` required, valid enum; `description` optional ≤ 255; `expenseDate` required, valid, ≤ today+1.
Response `201` `ExpenseResponse` + `Location: /api/v1/expenses/5012`. Errors: 400, 401.

### GET `/expenses/{id}`
Path: `id`. Response `200` `ExpenseResponse`. Errors: 401, 404.

### GET `/expenses`  (list + filter by date range)
Query: `from`, `to` (inclusive dates, both optional; if both, `from` ≤ `to`), `category` (optional), `q` (optional, case-insensitive match on description), `page`, `size`, `sort` (default `expenseDate,desc`).
Response `200` `PageResponse<ExpenseResponse>`. Errors: 400, 401.

### PUT `/expenses/{id}`
Request `UpdateExpenseRequest` (full replacement; same fields and rules as create). Response `200` `ExpenseResponse`. Errors: 400, 401, 404.

### DELETE `/expenses/{id}`
Response `204`. Errors: 401, 404. (The manual UI shows its own confirm dialog.)

## 4. Analytics APIs
All `GET`, auth required, read-only. `from`/`to` required, inclusive, `from` ≤ `to`, range ≤ 366 days.

### GET `/analytics/total?from=2026-11-01&to=2026-11-30`
`TotalSpendingResponse`:
```json
{ "from": "2026-11-01", "to": "2026-11-30", "totalAmount": 38450.00, "expenseCount": 42 }
```

### GET `/analytics/by-category?from=&to=`
Optional `category` filter (e.g. "how much on food").
`CategorySpendingResponse`:
```json
{ "from": "2026-11-01", "to": "2026-11-30", "totalAmount": 38450.00,
  "categories": [ { "category": "BILLS", "amount": 12500.00, "percentage": 32.51, "expenseCount": 4 },
                  { "category": "FOOD", "amount": 9800.00, "percentage": 25.49, "expenseCount": 18 } ] }
```
Sorted by amount descending; categories with no spending omitted.

### GET `/analytics/highest-category?from=&to=`
`HighestCategoryResponse`: `{ "from": "...", "to": "...", "category": "BILLS", "amount": 12500.00 }`. If no expenses: `category` and `amount` are `null`.

### GET `/analytics/monthly?year=2026&month=11`
Params: `year` 2000–2100, `month` 1–12.
`MonthlySummaryResponse`:
```json
{ "year": 2026, "month": 11, "totalAmount": 38450.00, "expenseCount": 42,
  "highestCategory": "BILLS", "previousMonthTotal": 41200.00, "changeFromPreviousMonth": -2750.00,
  "categories": [ { "category": "BILLS", "amount": 12500.00, "percentage": 32.51, "expenseCount": 4 } ] }
```

### GET `/analytics/report?from=&to=`
`DateRangeReportResponse`: total, count, average per day, category breakdown, highest category, plus `topExpenses` (5 largest `ExpenseResponse`).
```json
{ "from": "2026-11-01", "to": "2026-11-23", "totalAmount": 36200.00, "expenseCount": 39,
  "averagePerDay": 1573.91, "highestCategory": "BILLS", "categories": [ ... ], "topExpenses": [ ... ] }
```

## 5. AI Assistant APIs

### POST `/assistant/chat`
Purpose: send a message. Creates a conversation if `conversationId` is omitted; otherwise continues it.
Request `ChatRequest`: `{ "conversationId": 12, "message": "I spent ₹500 on dinner yesterday" }`
Validation: `message` 1–2000 chars, not blank; `conversationId` optional, must belong to user.
Response `200` `ChatResponse`:
```json
{ "conversationId": 12, "conversationTitle": "Dinner expense",
  "userMessage":      { "id": 301, "role": "USER", "content": "I spent ₹500 on dinner yesterday", "createdAt": "..." },
  "assistantMessage": { "id": 302, "role": "ASSISTANT", "content": "Done. I recorded ₹500 for Dinner (Food) on 22 Nov.",
                        "createdAt": "...", "toolsUsed": ["addExpense"] },
  "pendingAction": null }
```
When deletion/edit is proposed:
```json
"pendingAction": { "actionId": "9f1c2e7a-...", "type": "DELETE_EXPENSE",
                   "summary": "Delete ₹500 Dinner (Food) on 22 Nov 2026?", "expiresAt": "2026-11-23T08:25:30Z" }
```
`type` is `UPDATE_EXPENSE` or `DELETE_EXPENSE`; for update, the summary lists old → new values.
Errors: 400, 401, 404 (conversation), 503 `AI_UNAVAILABLE`. If the LLM fails, the user message is kept in history and no assistant message is saved.

### POST `/assistant/actions/{actionId}/confirm`
Executes the pending action through `ExpenseService`. Response `200` `ActionResultResponse`:
```json
{ "actionId": "9f1c...", "status": "CONFIRMED", "message": { "id": 305, "role": "ASSISTANT", "content": "Deleted: ₹500 Dinner on 22 Nov.", "createdAt": "..." } }
```
Errors: 401, 404 (unknown/other user's), 410 `ACTION_EXPIRED`.

### POST `/assistant/actions/{actionId}/cancel`
Discards the action. Response `200` `ActionResultResponse` with `status: "CANCELLED"` and an assistant message "Okay, I did not change anything." Errors same as confirm.
Both endpoints append the result message to the conversation history.

## 6. Conversation APIs
`ConversationSummaryResponse`: `{ "id": 12, "title": "Dinner expense", "createdAt": "...", "updatedAt": "...", "lastMessagePreview": "Done. I recorded..." }`

| Method | Endpoint | Purpose | Response |
|---|---|---|---|
| GET | `/conversations?page=&size=` | List own conversations, newest `updatedAt` first | `200` `PageResponse<ConversationSummaryResponse>` |
| GET | `/conversations/{id}` | Conversation details | `200` `ConversationDetailResponse` (`id`, `title`, `createdAt`, `updatedAt`, `messageCount`) |
| GET | `/conversations/{id}/messages?page=&size=` | Message history, oldest first (default size 50, max 200) | `200` `PageResponse<MessageResponse>` |

`MessageResponse`: `id, role (USER|ASSISTANT), content, createdAt, toolsUsed[]` (assistant only; may be empty).
Errors: 401, 404. Conversations are created only through `/assistant/chat` (no separate create endpoint — avoids empty conversations).

## 7. Endpoint summary
| # | Method | Path | FR |
|---|---|---|---|
| 1 | POST | /auth/register | FR-01 |
| 2 | POST | /auth/login | FR-02 |
| 3 | POST | /auth/logout | FR-03 |
| 4 | POST | /expenses | FR-06 |
| 5 | GET | /expenses/{id} | FR-07 |
| 6 | GET | /expenses | FR-07, 10, 11 |
| 7 | PUT | /expenses/{id} | FR-08 |
| 8 | DELETE | /expenses/{id} | FR-09 |
| 9–13 | GET | /analytics/total, by-category, highest-category, monthly, report | FR-12–16 |
| 14 | POST | /assistant/chat | FR-17–24 |
| 15–16 | POST | /assistant/actions/{id}/confirm, /cancel | BR-15 |
| 17–19 | GET | /conversations, /conversations/{id}, /conversations/{id}/messages | FR-25 |
