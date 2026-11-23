# FRONTEND_DESIGN – AI Expense Assistant

Visual references: `ui-mockups/auth.html`, `dashboard.html`, `assistant.html` (open directly in a browser; see `ui-mockups/README.md`).

## 1. Design direction
A calm, trustworthy personal-finance product. Deep ink navy sidebar and text, teal as the single action colour, amber reserved for "needs attention" (pending confirmation), neutral cool-grey surfaces. Numbers are the hero: large, tabular figures.

| Token | Value | Use |
|---|---|---|
| `--ink` | `#0e1b2c` | Text, sidebar |
| `--bg` | `#f3f5f8` | Page background |
| `--card` | `#ffffff` | Surfaces |
| `--accent` | `#0f766e` | Primary buttons, links, active nav |
| `--accent-soft` | `#e3f3f1` | Selected/hover |
| `--warn` | `#b45309` / `#fef3c7` | Pending confirmation |
| `--danger` | `#b42318` | Delete, errors |
| `--line` | `#e2e6ec` | Borders |

Category colours: FOOD `#f59e0b`, TRANSPORTATION `#3b82f6`, SHOPPING `#ec4899`, ENTERTAINMENT `#8b5cf6`, HEALTH `#10b981`, EDUCATION `#06b6d4`, BILLS `#ef4444`, TRAVEL `#f97316`, OTHER `#94a3b8`. Colour is never the only signal (labels always shown).
Type: system UI stack, tabular numerals for money, sentence-case labels. Radius 10 px cards, 8 px controls. Breakpoints: ≥ 960 px sidebar layout; < 960 px top bar + bottom-sheet navigation.
Money formatting: `Intl.NumberFormat('en-IN', {style:'currency', currency:'INR'})`.

## 2. Screens
| # | Screen | Route | Key content |
|---|---|---|---|
| 1 | Login | `/login` | Email, password, link to register |
| 2 | Registration | `/register` | Name, email, password |
| 3 | Dashboard | `/` | Total this month, monthly comparison, category donut, recent 5, spending trend, quick-add, "Ask the assistant" |
| 4 | Expense list | `/expenses` | Table (cards on mobile), search, date range, category filter, sort, pagination, add/edit/delete |
| 5 | Add expense | `/expenses/new` (modal on desktop) | Amount, category, description, date |
| 6 | Edit expense | `/expenses/:id/edit` | Same form, prefilled |
| 7 | Expense details | `/expenses/:id` | All fields, timestamps, edit/delete |
| 8 | Analytics | `/analytics` | Range picker, total, category chart + table, highest category, monthly overview, report |
| 9 | AI Assistant | `/assistant`, `/assistant/:conversationId` | Chat + conversation list |
| 10 | Conversation history | list inside `/assistant` (own page on mobile: `/assistant/history`) | Past conversations |

## 3. Dashboard
Cards: **This month** (total, count, change vs last month), **Highest category**, **Category donut**, **6-month bar chart**, **Recent expenses** (5), buttons **Add expense** and **Ask the assistant**. Data: `GET /analytics/monthly`, `GET /analytics/by-category`, `GET /expenses?size=5`.

## 4. Expense management
- Table columns: date, description, category chip, amount (right-aligned), actions.
- Filters bound to URL query (shareable): `from`, `to`, `category`, `q`.
- Form validation (React Hook Form + Zod): amount > 0 and ≤ 2 decimals; category required; date required, not > tomorrow; description ≤ 255. Server `fieldErrors` mapped onto fields.
- States: **loading** skeleton rows; **empty** "No expenses yet — add your first expense" (or "No matches — clear filters"); **error** inline banner with Retry.
- Delete: confirm dialog ("Delete ₹500 Dinner?"), toast on success.

## 5. Analytics UI
Range presets (This month, Last month, Last 30 days, Custom) → total, average/day, category donut + table with percentages, highlighted highest category, 6-month trend (via `GET /analytics/monthly` ×6 or monthly endpoint calls cached by TanStack Query), report list of top expenses.

## 6. AI Assistant UI
Two panes: conversation list (left, collapsible on mobile) and chat (right).
- Message bubbles: user right (accent), assistant left (white card). Timestamps on hover.
- **Tool feedback chips** under assistant messages: "Added expense", "Searched expenses", "Calculated spending" (from `toolsUsed`).
- **Thinking state**: animated dots with "Checking your expenses…".
- **Confirmation card** (amber) from `pendingAction`: summary, **Confirm** and **Cancel**; disabled with "Expired" after `expiresAt`. Confirm/cancel call the action endpoints and append the returned message.
- Errors: banner "The assistant is unavailable right now. Your message is saved — try again." with Retry; manual features remain reachable.
- Input: multiline, Enter to send, Shift+Enter newline, 2000-char limit, suggested prompts on an empty chat.

## 7. Frontend architecture

### Folder structure
```
frontend/src
├── main.tsx, App.tsx, routes.tsx
├── api/            client.ts (fetch wrapper, auth header, error mapping)
│                   auth.api.ts, expenses.api.ts, analytics.api.ts, assistant.api.ts, conversations.api.ts
├── types/          api.ts (interfaces below)
├── auth/           AuthContext.tsx, useAuth.ts, ProtectedRoute.tsx
├── features/
│   ├── expenses/   ExpenseTable, ExpenseForm, ExpenseFilters, ExpenseDetails, hooks (useExpenses…)
│   ├── analytics/  CategoryDonut, MonthlyBars, SummaryCards, hooks
│   ├── dashboard/  DashboardPage
│   └── assistant/  ChatPage, ConversationList, MessageList, MessageBubble, ChatInput,
│                   PendingActionCard, ToolChips, hooks (useChat)
├── components/     Button, Input, Select, Modal, ConfirmDialog, Toast, Skeleton, EmptyState, ErrorBanner, CategoryChip
├── layouts/        AppLayout (sidebar/topbar), AuthLayout
├── utils/          money.ts, dates.ts
└── styles/         tokens.css, global.css (CSS Modules per component)
```
Styling: CSS variables + CSS Modules (no UI framework needed; keeps the learning focus).

### Component hierarchy (simplified)
```
App → AuthProvider → QueryClientProvider → Router
  ├ AuthLayout → LoginPage | RegisterPage
  └ ProtectedRoute → AppLayout(Sidebar, Topbar)
      ├ DashboardPage → SummaryCards, CategoryDonut, MonthlyBars, RecentExpenses
      ├ ExpensesPage → ExpenseFilters, ExpenseTable, Pagination → ExpenseFormModal
      ├ ExpenseDetailsPage, ExpenseEditPage → ExpenseForm
      ├ AnalyticsPage → RangePicker, SummaryCards, CategoryDonut, MonthlyBars
      └ ChatPage → ConversationList, MessageList(MessageBubble, ToolChips, PendingActionCard), ChatInput
```

### Routing and protection
React Router. `ProtectedRoute` redirects to `/login` when there is no token; `/login` and `/register` redirect home if authenticated. A 401 from any API call clears auth state and redirects to login.

### Auth state
`AuthContext` holds `user` and `accessToken` (memory, mirrored to `sessionStorage` so refresh keeps the session; cleared on logout/401). Token only sent in the `Authorization` header. (Before Stage 10, the client simply omits the header and the dev profile supplies identity.)

### Server state vs UI state
TanStack Query for all API data (cache keys: `['expenses', filters]`, `['analytics', range]`, `['conversations']`, `['messages', id]`); invalidate `expenses` and `analytics` after any expense mutation **including** assistant turns whose `toolsUsed` includes a write tool or after a confirmed action (so UI and chat stay consistent).

### Chat state
`useChat(conversationId)`: messages from `GET …/messages`, optimistic user bubble, `isThinking`, `pendingAction` from the last `ChatResponse`, error state. New chat = no id until the first response returns `conversationId`, then navigate to `/assistant/:id`.

### Forms
React Hook Form + Zod schemas mirroring API validation rules.

### TypeScript interfaces (names match API DTOs)
```ts
type ExpenseCategory = 'FOOD'|'TRANSPORTATION'|'SHOPPING'|'ENTERTAINMENT'|'HEALTH'|'EDUCATION'|'BILLS'|'TRAVEL'|'OTHER';
interface ExpenseResponse { id:number; amount:number; category:ExpenseCategory; description:string|null; expenseDate:string; createdAt:string; updatedAt:string }
interface CreateExpenseRequest { amount:number; category:ExpenseCategory; description?:string; expenseDate:string }
type UpdateExpenseRequest = CreateExpenseRequest;
interface PageResponse<T> { content:T[]; page:number; size:number; totalElements:number; totalPages:number }
interface CategoryTotal { category:ExpenseCategory; amount:number; percentage:number; expenseCount:number }
interface MonthlySummaryResponse { year:number; month:number; totalAmount:number; expenseCount:number; highestCategory:ExpenseCategory|null; previousMonthTotal:number; changeFromPreviousMonth:number; categories:CategoryTotal[] }
interface PendingActionResponse { actionId:string; type:'UPDATE_EXPENSE'|'DELETE_EXPENSE'; summary:string; expiresAt:string }
interface MessageResponse { id:number; role:'USER'|'ASSISTANT'; content:string; createdAt:string; toolsUsed?:string[] }
interface ChatRequest { conversationId?:number; message:string }
interface ChatResponse { conversationId:number; conversationTitle:string; userMessage:MessageResponse; assistantMessage:MessageResponse; pendingAction:PendingActionResponse|null }
interface ErrorResponse { timestamp:string; status:number; error:string; message:string; path:string; fieldErrors?:{field:string;message:string}[] }
```
(Remaining DTOs follow API_DESIGN.md one-to-one.)

### API service layer
`client.ts` wraps `fetch`: base URL from `VITE_API_BASE_URL` (default `http://localhost:8080/api/v1`), adds the bearer token, parses JSON, throws a typed `ApiError(ErrorResponse)`. Feature modules export typed functions (`createExpense`, `listExpenses`, `sendChatMessage`, `confirmAction`…).

### Error handling
`ApiError` → field errors to forms; 401 → logout; 503 `AI_UNAVAILABLE` → chat banner; others → toast / `ErrorBanner`. An error boundary protects the app shell.

### Reusable components
Button, Input, Select, DatePicker (native date input), Modal, ConfirmDialog, Toast, Skeleton, EmptyState, ErrorBanner, CategoryChip, MoneyText.

## 8. Mockups
| File | Screens covered |
|---|---|
| `auth.html` | Login, Registration (switchable) |
| `dashboard.html` | Dashboard |
| `expenses.html` | Expense listing, Add expense, Edit expense (one modal), delete confirmation, loading/empty/error states |
| `expense-details.html` | Expense details |
| `analytics.html` | Analytics dashboard |
| `assistant.html` | AI Assistant, tool chips, confirmation card, thinking state |
| `conversations.html` | Conversation history |
They are static references with sample data (no backend); the React app should reproduce layout, tokens and states. On desktop Add/Edit open as a modal; the routes `/expenses/new` and `/expenses/:id/edit` render the same form full-page (used on mobile and for direct links).
