import type {
  CreateExpenseRequest,
  ExpenseResponse,
  PageResponse,
  UpdateExpenseRequest,
} from '../types/api'
import { apiDelete, apiGet, apiPost, apiPut } from './client'

export interface ListExpensesParams {
  from?: string
  to?: string
  category?: string
  q?: string
  page?: number
  size?: number
  sort?: string
}

export function listExpenses(params: ListExpensesParams): Promise<PageResponse<ExpenseResponse>> {
  return apiGet('/expenses', params as Record<string, string | number | undefined>)
}

export function getExpense(id: number): Promise<ExpenseResponse> {
  return apiGet(`/expenses/${id}`)
}

export function createExpense(body: CreateExpenseRequest): Promise<ExpenseResponse> {
  return apiPost('/expenses', body)
}

export function updateExpense(id: number, body: UpdateExpenseRequest): Promise<ExpenseResponse> {
  return apiPut(`/expenses/${id}`, body)
}

export function deleteExpense(id: number): Promise<void> {
  return apiDelete(`/expenses/${id}`)
}