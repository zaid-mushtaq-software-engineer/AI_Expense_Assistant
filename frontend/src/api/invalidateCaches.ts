import type { QueryClient } from '@tanstack/react-query'

export function invalidateExpensesAndAnalytics(queryClient: QueryClient) {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['expenses'] }),
    queryClient.invalidateQueries({ queryKey: ['expense'] }),
    queryClient.invalidateQueries({ queryKey: ['analytics'] }),
  ])
}