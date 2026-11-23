export type ExpenseListFilters = {
  from?: string
  to?: string
  category?: string
  q?: string
  page: number
  size: number
  sort: string
}

export const queryKeys = {
  expenses: (filters: ExpenseListFilters) => ['expenses', filters] as const,
  expense: (id: number) => ['expense', id] as const,
  analyticsMonthly: (year: number, month: number) => ['analytics', 'monthly', year, month] as const,
  analyticsRange: (from: string, to: string, kind: string) =>
    ['analytics', 'range', from, to, kind] as const,
}