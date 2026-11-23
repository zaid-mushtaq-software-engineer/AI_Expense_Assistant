import { useQuery } from '@tanstack/react-query'
import { getExpense } from '../../../api/expenses.api'
import { queryKeys } from '../../../api/queryKeys'

export function useExpense(id: number) {
  return useQuery({
    queryKey: queryKeys.expense(id),
    queryFn: () => getExpense(id),
    enabled: Number.isFinite(id) && id > 0,
  })
}