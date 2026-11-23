# BACKEND_DESIGN – AI Expense Assistant

## 1. Technology stack
Java 21 · Spring Boot 3.5.x (assumption A1, verify) · Maven · Spring Web, Validation, Data JPA, Security · Spring AI 1.1.x (provider starter + JDBC chat-memory repository) · MySQL 8.4 + Flyway · jjwt 0.12.x · JUnit 5, Mockito, Testcontainers (MySQL) optional.

## 2. Modular monolith
One Maven module, packages per business module. Rule: a module exposes its **service** and DTOs; other modules never use its repositories or entities directly (entities stay inside their module; cross-module data travels as DTOs/records).

## 3. Package structure
```
com.expenseassistant
├── ExpenseAssistantApplication
├── config            AiConfig, SecurityConfig, ClockConfig, JpaConfig(auditing)
├── security          JwtAuthenticationFilter, JwtService, JwtProperties,
│                     CurrentUserProvider (interface), SecurityContextCurrentUserProvider,
│                     DevCurrentUserProvider (@Profile("dev"), removed after Stage 10)
├── user
│   ├── controller    AuthController
│   ├── service       AuthService, UserService
│   ├── repository    UserRepository
│   ├── entity        User
│   └── dto           RegisterRequest, LoginRequest, AuthResponse, UserResponse
├── expense
│   ├── controller    ExpenseController
│   ├── service       ExpenseService
│   ├── repository    ExpenseRepository
│   ├── entity        Expense, ExpenseCategory (enum)
│   ├── mapper        ExpenseMapper
│   └── dto           CreateExpenseRequest, UpdateExpenseRequest, ExpenseResponse, ExpenseFilter
├── analytics
│   ├── controller    AnalyticsController
│   ├── service       AnalyticsService
│   ├── repository    (uses ExpenseRepository aggregate queries via expense module's query interface*)
│   └── dto           TotalSpendingResponse, CategorySpendingResponse, CategoryTotal,
│                     HighestCategoryResponse, MonthlySummaryResponse, DateRangeReportResponse
├── conversation
│   ├── controller    ConversationController
│   ├── service       ConversationService
│   ├── repository    ConversationRepository, ChatMessageRepository
│   ├── entity        Conversation, ChatMessage, MessageRole (enum)
│   └── dto           ConversationSummaryResponse, ConversationDetailResponse, MessageResponse
├── assistant
│   ├── controller    AssistantController
│   ├── service       AssistantService, PendingActionService
│   ├── tools         ExpenseTools, AnalyticsTools, ToolResults (records)
│   ├── prompt        SystemPromptProvider (+ resources/prompts/system.st)
│   └── dto           ChatRequest, ChatResponse, PendingActionResponse, ActionResultResponse, ExpenseDraft
└── common
    ├── exception     GlobalExceptionHandler, ResourceNotFoundException, ValidationFailedException,
    │                 PendingActionExpiredException, AiUnavailableException
    └── response      ErrorResponse, PageResponse<T>
```
\* Simplification: `AnalyticsService` may use a dedicated read-only `ExpenseAnalyticsRepository` interface declared in the `analytics` package that queries the `expenses` table. This keeps aggregation queries out of the CRUD repository without letting analytics reach into expense internals.

## 4. Layered architecture
`Controller → Service → Repository → DB`. DTOs cross the controller boundary; entities never leave the service layer.

## 5. Controllers
Parse and validate (`@Valid`), resolve `userId` through `CurrentUserProvider`, call one service method, return DTO + status. **No business logic, no repository access, no entity returns.**

## 6. Services
| Service | Responsibility |
|---|---|
| `AuthService` | Register (hash, uniqueness), login (verify), logout (bump `token_version`), issue JWT |
| `UserService` | Load user by id/email |
| `ExpenseService` | `create/get/update/delete/search(userId, …)`; enforces BR-01…05, BR-11…14 |
| `AnalyticsService` | `total / byCategory / highestCategory / monthlySummary / report(userId, …)`; validates range (BR-16) |
| `ConversationService` | create, get owned, list, append message, history, title generation |
| `AssistantService` | One chat turn (see §16); owns LLM failure handling |
| `PendingActionService` | Create/consume/cancel pending actions; expiry; owner + conversation binding |

Every service method that touches user data has `Long userId` as its first parameter.

## 7. Repositories
Spring Data interfaces. Every query on user data filters by `userId` (e.g. `findByIdAndUserId`, `findByUserIdAndExpenseDateBetween`). Aggregations as JPQL, e.g. `SELECT e.category, SUM(e.amount), COUNT(e) … WHERE e.user.id = :userId AND e.expenseDate BETWEEN :from AND :to GROUP BY e.category`.

## 8. DTO design
Java records with Bean Validation annotations. Request DTOs are separate from response DTOs and from entities. Names are fixed in API_DESIGN.md. `ExpenseDraft` (assistant) = `amount, category, description, expenseDate` all nullable → fields that are null are "missing" (drives clarification).

## 9. Entities
`User`, `Expense`, `Conversation`, `ChatMessage` (columns in DATABASE_DESIGN.md). `Expense` and `Conversation` hold a `@ManyToOne(fetch = LAZY) User`. `createdAt/updatedAt` via JPA auditing. `BigDecimal` for money. Enums stored as `STRING`.

## 10. Validation strategy
Two layers:
1. **Shape validation** in DTOs (`@NotNull`, `@DecimalMin`, `@Size`, `@Digits`).
2. **Business validation** in services (date not too far in future, range rules, category valid).
The same service validation protects the AI path, because tools call services (BR-09). DB constraints are the last line.

## 11. Exception handling
`GlobalExceptionHandler` converts: `MethodArgumentNotValidException`/`ConstraintViolationException` → 400; `ResourceNotFoundException` → 404; `BadCredentialsException` → 401; email conflict → 409; `PendingActionExpiredException` → 410; `AiUnavailableException` → 503; else 500 (log with stack trace, return generic message). Format: `ErrorResponse` (API_DESIGN §1). Security entry point returns the same shape for 401.

## 12. Transaction management
`@Transactional` on service methods (`readOnly = true` for reads/analytics). **No transaction around the LLM call**: the chat turn saves the user message in its own transaction, calls the LLM (tools open their own short service transactions), then saves the assistant message in a new transaction. This avoids holding DB connections during slow network calls.

## 13. Logging and configuration
SLF4J; log request id, userId, tool name, outcome. Never log passwords, tokens, or full message content at INFO. `application.yml` + profiles (`dev`, `prod`); secrets via env vars: `DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, LLM_API_KEY`. `@ConfigurationProperties` for JWT (secret, `expiresIn`), app (`timezone`, `pendingActionTtl`), CORS origins.

## 14. Current-user abstraction (auth is built last)
```java
interface CurrentUserProvider { Long requireUserId(); }
```
| Stage | Implementation | Notes |
|---|---|---|
| Stages 1–9 | `DevCurrentUserProvider` — `@Profile("dev")`, returns the id of a seeded dev user (configurable `app.dev-user-id`, optionally overridden by header `X-Dev-User-Id` to test isolation with 2 users) | Not active without profile `dev`; app refuses to start in `prod` with it. All security headers/CORS still configured; endpoints are *not* publicly deployable. |
| Stage 10 | `SecurityContextCurrentUserProvider` reads `UserPrincipal` from `SecurityContext` | Delete the dev provider and header. Services/tools untouched. |

The multi-user model (FKs, `userId` parameters, scoped queries, ownership tests with two users) exists from Stage 1, so Stage 10 only adds identity.

## 15. Authentication and JWT design
- `POST /auth/register`: validate → lower-case email → BCrypt (strength 10) → save → issue JWT.
- `JwtService`: HS256, secret ≥ 32 bytes from env; claims `sub=userId`, `tv=tokenVersion`, `iat`, `exp` (60 min).
- `JwtAuthenticationFilter` (`OncePerRequestFilter`): read Bearer token → verify → load user → compare `tv` → set `UsernamePasswordAuthenticationToken` with `UserPrincipal(id, email)`.
- `SecurityConfig`: stateless sessions, CSRF disabled (no cookies), CORS for the Vite origin, permit `/api/v1/auth/register|login`, everything else authenticated.
- Logout: increment `users.token_version` → all older tokens rejected.
- Authorization = ownership only (no roles).

## 16. Spring AI integration
**`AiConfig`** builds one `ChatClient` with: default system prompt template, `MessageChatMemoryAdvisor` backed by `MessageWindowChatMemory(maxMessages=20)` over `JdbcChatMemoryRepository`, and registered tool beans. Provider/model/temperature in `application.yml` (temperature low, e.g. 0–0.2, for reliable tool use).

**ChatClient responsibilities:** send prompt + tool schemas, run the tool loop, return final text. It does **not**: save history, check ownership, or enforce rules.

**`AssistantService.chat(userId, ChatRequest)`**
1. `ConversationService.getOwnedOrCreate(userId, conversationId)`.
2. `ConversationService.addMessage(USER)`.
3. Build system prompt with `today` (`LocalDate.now(clock)`), weekday, timezone, category list.
4. `chatClient.prompt().system(...).user(message).advisors(a -> a.param(CONVERSATION_ID, id)).toolContext(Map.of("userId", userId, "conversationId", id)).call().content()`
5. Collect tools used and any pending action (set by `PendingActionService` during the turn, read by conversation id).
6. `ConversationService.addMessage(ASSISTANT, text, metadata)`; on first message set the title (first ~50 chars of the user message; assumption — no extra LLM call).
7. Return `ChatResponse`. Spring AI/provider exceptions → `AiUnavailableException`.

## 17. Tool calling implementation design
`@Tool(description=…)` methods on Spring beans; parameters described with `@ToolParam`; every method receives `ToolContext` and reads `userId` from it. Tools return small records (`ToolResults.ExpenseResult`, `ListResult`, `ErrorResult`, `PendingResult`). Rules:
- No user-id parameters. No repository injection. No rules inside tools.
- Dates passed as ISO strings; tool converts, service validates.
- Domain exceptions → `ErrorResult(code, message)` so the model can ask the user.
- Descriptions say when to use the tool and that `proposeExpense*` does **not** change data until the user confirms.

| Tool | Parameters | Returns |
|---|---|---|
| `addExpense` | amount, category, description?, expenseDate | created expense |
| `searchExpenses` | from?, to?, category?, text? | up to 20 matches with ids + total count |
| `proposeExpenseUpdate` | expenseId, amount?, category?, description?, expenseDate? | `PendingResult` (before→after) |
| `proposeExpenseDeletion` | expenseId | `PendingResult` |
| `getTotalSpending` | from, to | total, count |
| `getCategorySpending` | from, to, category? | breakdown |
| `getHighestSpendingCategory` | from, to | category, amount |
| `getMonthlySummary` | year, month | summary |
| `getDateRangeReport` | from, to | report |

**Pending actions:** `PendingActionService` keeps a `ConcurrentHashMap<UUID, PendingAction>` with TTL (A8). `confirm(userId, actionId)` checks owner, expiry, single use, then calls `ExpenseService.update/delete(userId, …)`. The result message is saved to the conversation.

## 18. Structured Output usage
Stage 6 introduces `ExpenseDraft` via `.entity(ExpenseDraft.class)` to teach extraction ("I spent ₹500 on dinner yesterday" → `{amount:500, category:FOOD, description:"Dinner", expenseDate:2026-11-22}`); missing/invalid fields drive a clarification. From Stage 7 the typed `addExpense` tool parameters perform the same extraction, so `ExpenseDraft` stays as the learning/preview endpoint's type and the tools reuse its validation logic via `ExpenseService`. (Structured output and tool calling are not mixed in one call to keep behaviour predictable.)

## 19. Chat Memory integration
Memory key = conversation id (string), set per call via advisor param. Memory holds USER/ASSISTANT text for the last 20 messages. `chat_message` remains the source for history screens. Ownership is checked before the id is used.

## 20. User context propagation
Controller → `CurrentUserProvider` → `userId` parameter → `AssistantService` → `ToolContext` → tool → service. The LLM only ever sees tool schemas that have no user id.

## 21. Testing guidance
Unit-test services (rules), slice-test controllers (`@WebMvcTest`), repository tests on MySQL (Testcontainers) or H2 in MySQL mode for early stages, tool tests with mocked services, and two-user isolation tests at every service.
