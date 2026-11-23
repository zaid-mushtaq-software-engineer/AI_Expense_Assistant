# UI mockups

Static visual references for the React frontend (no backend, no build step). Open `auth.html` in a browser; everything links together.

| File | Screen | What works |
|---|---|---|
| `auth.html` | Login / Registration | Tab switch, validation, sign-in goes to dashboard |
| `dashboard.html` | Dashboard | Summary cards, donut, 6-month bars, recent expenses with search/category filter, Add expense opens the form |
| `expenses.html` | Expense listing, Add, Edit, Delete | Working add/edit/delete, form validation, search, date/category filters, sort, delete confirm dialog, toasts, and a **state switcher** (Data / Loading / Empty / Error) |
| `expense-details.html` | Expense details | Fields, timestamps, same-day expenses, Edit, Delete confirm |
| `analytics.html` | Analytics | Presets (This month / Last month / Custom), totals, highest category, category chart + percentages, monthly overview, largest expenses |
| `assistant.html` | AI Assistant | Chat, tool chips, thinking state, working Confirm/Cancel on the pending-deletion card |
| `conversations.html` | Conversation history | Searchable list; opens the assistant |

Add and Edit are one modal form. `expenses.html#new` and `expenses.html#edit` open it directly. Data is sample data held in the page, so changes reset on reload. Tokens are CSS variables at the top of each file and match `docs/FRONTEND_DESIGN.md`. Charts are CSS-only here; the React app will use Recharts.
