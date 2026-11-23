export type ExpenseCategory =
  | 'FOOD'
  | 'TRANSPORTATION'
  | 'SHOPPING'
  | 'ENTERTAINMENT'
  | 'HEALTH'
  | 'EDUCATION'
  | 'BILLS'
  | 'TRAVEL'
  | 'OTHER'

export interface ExpenseResponse {
  id: number
  amount: number
  category: ExpenseCategory
  description: string | null
  expenseDate: string
  createdAt: string
  updatedAt: string
}

export interface CreateExpenseRequest {
  amount: number
  category: ExpenseCategory
  description?: string
  expenseDate: string
}

export type UpdateExpenseRequest = CreateExpenseRequest

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: { field: string; message: string }[]
}

export interface CategoryTotal {
  category: ExpenseCategory
  amount: number
  percentage: number
  expenseCount: number
}

export interface TotalSpendingResponse {
  from: string
  to: string
  totalAmount: number
  expenseCount: number
}

export interface CategorySpendingResponse {
  from: string
  to: string
  totalAmount: number
  categories: CategoryTotal[]
}

export interface HighestCategoryResponse {
  from: string
  to: string
  category: ExpenseCategory | null
  amount: number | null
}

export interface MonthlySummaryResponse {
  year: number
  month: number
  totalAmount: number
  expenseCount: number
  highestCategory: ExpenseCategory | null
  previousMonthTotal: number
  changeFromPreviousMonth: number
  categories: CategoryTotal[]
}

export interface DateRangeReportResponse {
  from: string
  to: string
  totalAmount: number
  expenseCount: number
  averagePerDay: number
  highestCategory: ExpenseCategory | null
  categories: CategoryTotal[]
  topExpenses: ExpenseResponse[]
}

export const EXPENSE_CATEGORIES: ExpenseCategory[] = [
  'FOOD',
  'TRANSPORTATION',
  'SHOPPING',
  'ENTERTAINMENT',
  'HEALTH',
  'EDUCATION',
  'BILLS',
  'TRAVEL',
  'OTHER',
]

export const CATEGORY_LABELS: Record<ExpenseCategory, string> = {
  FOOD: 'Food',
  TRANSPORTATION: 'Transportation',
  SHOPPING: 'Shopping',
  ENTERTAINMENT: 'Entertainment',
  HEALTH: 'Health',
  EDUCATION: 'Education',
  BILLS: 'Bills',
  TRAVEL: 'Travel',
  OTHER: 'Other',
}