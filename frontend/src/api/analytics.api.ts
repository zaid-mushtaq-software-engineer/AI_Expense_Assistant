import type {
  CategorySpendingResponse,
  DateRangeReportResponse,
  HighestCategoryResponse,
  MonthlySummaryResponse,
  TotalSpendingResponse,
} from '../types/api'
import { apiGet } from './client'

export function fetchMonthly(year: number, month: number): Promise<MonthlySummaryResponse> {
  return apiGet('/analytics/monthly', { year, month })
}

export function fetchTotal(from: string, to: string): Promise<TotalSpendingResponse> {
  return apiGet('/analytics/total', { from, to })
}

export function fetchByCategory(
  from: string,
  to: string,
  category?: string,
): Promise<CategorySpendingResponse> {
  return apiGet('/analytics/by-category', { from, to, category })
}

export function fetchHighestCategory(from: string, to: string): Promise<HighestCategoryResponse> {
  return apiGet('/analytics/highest-category', { from, to })
}

export function fetchReport(from: string, to: string): Promise<DateRangeReportResponse> {
  return apiGet('/analytics/report', { from, to })
}