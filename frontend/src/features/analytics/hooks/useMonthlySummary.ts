import { useQuery } from '@tanstack/react-query'
import { fetchMonthly } from '../../../api/analytics.api'
import { queryKeys } from '../../../api/queryKeys'

export function useMonthlySummary(year: number, month: number) {
  return useQuery({
    queryKey: queryKeys.analyticsMonthly(year, month),
    queryFn: () => fetchMonthly(year, month),
  })
}