import { useQueries } from '@tanstack/react-query'
import { fetchMonthly } from '../../../api/analytics.api'
import { queryKeys } from '../../../api/queryKeys'

export function getLastSixMonthPairs(reference = new Date()): { year: number; month: number }[] {
  const pairs: { year: number; month: number }[] = []
  const d = new Date(reference.getFullYear(), reference.getMonth(), 1)
  for (let i = 5; i >= 0; i -= 1) {
    const cur = new Date(d.getFullYear(), d.getMonth() - i, 1)
    pairs.push({ year: cur.getFullYear(), month: cur.getMonth() + 1 })
  }
  return pairs
}

export function useLastSixMonths(reference = new Date()) {
  const pairs = getLastSixMonthPairs(reference)
  return useQueries({
    queries: pairs.map(({ year, month }) => ({
      queryKey: queryKeys.analyticsMonthly(year, month),
      queryFn: () => fetchMonthly(year, month),
    })),
  })
}