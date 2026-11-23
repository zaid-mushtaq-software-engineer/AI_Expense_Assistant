const DEFAULT_SORT = 'expenseDate,desc'
const DEFAULT_SIZE = 20

export type ExpenseListParams = {
  from?: string
  to?: string
  category?: string
  q?: string
  page: number
  size: number
  sort: string
}

export function parseExpenseListParams(search: URLSearchParams): ExpenseListParams {
  return {
    from: search.get('from') ?? undefined,
    to: search.get('to') ?? undefined,
    category: search.get('category') ?? undefined,
    q: search.get('q') ?? undefined,
    page: Math.max(0, Number(search.get('page') ?? 0) || 0),
    size: Number(search.get('size') ?? DEFAULT_SIZE) || DEFAULT_SIZE,
    sort: search.get('sort') ?? DEFAULT_SORT,
  }
}

export function toSearchParams(params: ExpenseListParams): URLSearchParams {
  const sp = new URLSearchParams()
  if (params.from) sp.set('from', params.from)
  if (params.to) sp.set('to', params.to)
  if (params.category) sp.set('category', params.category)
  if (params.q) sp.set('q', params.q)
  if (params.page > 0) sp.set('page', String(params.page))
  if (params.size !== DEFAULT_SIZE) sp.set('size', String(params.size))
  if (params.sort !== DEFAULT_SORT) sp.set('sort', params.sort)
  return sp
}

export function hasActiveFilters(params: ExpenseListParams): boolean {
  return Boolean(params.from || params.to || params.category || params.q)
}

export const SORT_OPTIONS = [
  { value: 'expenseDate,desc', label: 'Date (newest)' },
  { value: 'expenseDate,asc', label: 'Date (oldest)' },
  { value: 'amount,desc', label: 'Amount (high)' },
  { value: 'amount,asc', label: 'Amount (low)' },
  { value: 'createdAt,desc', label: 'Created (newest)' },
] as const