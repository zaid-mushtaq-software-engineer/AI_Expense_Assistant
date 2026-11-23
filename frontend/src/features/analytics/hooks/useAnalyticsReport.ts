import { useQuery } from '@tanstack/react-query'
import { fetchReport } from '../../../api/analytics.api'
import { queryKeys } from '../../../api/queryKeys'

export function useAnalyticsReport(from: string, to: string, enabled = true) {
  return useQuery({
    queryKey: queryKeys.analyticsRange(from, to, 'report'),
    queryFn: () => fetchReport(from, to),
    enabled: enabled && Boolean(from && to),
  })
}