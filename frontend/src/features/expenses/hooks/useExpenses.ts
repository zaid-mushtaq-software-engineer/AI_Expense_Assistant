import { useQuery } from '@tanstack/react-query'
import { listExpenses } from '../../../api/expenses.api'
import { queryKeys, type ExpenseListFilters } from '../../../api/queryKeys'

export function useExpenses(filters: ExpenseListFilters, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: queryKeys.expenses(filters),
    queryFn: () =>
      listExpenses({
        from: filters.from,
        to: filters.to,
        category: filters.category,
        q: filters.q,
        page: filters.page,
        size: filters.size,
        sort: filters.sort,
      }),
    placeholderData: (prev) => prev,
    enabled: options?.enabled ?? true,
  })
}