import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createExpense, deleteExpense, updateExpense } from '../../api/expenses.api'
import { invalidateExpensesAndAnalytics } from '../../api/invalidateCaches'
import { ApiError } from '../../api/client'
import type { CreateExpenseRequest, UpdateExpenseRequest } from '../../types/api'

export function useExpenseMutations() {
  const queryClient = useQueryClient()

  const create = useMutation({
    mutationFn: (body: CreateExpenseRequest) => createExpense(body),
    onSuccess: () => invalidateExpensesAndAnalytics(queryClient),
  })

  const update = useMutation({
    mutationFn: ({ id, body }: { id: number; body: UpdateExpenseRequest }) => updateExpense(id, body),
    onSuccess: () => invalidateExpensesAndAnalytics(queryClient),
  })

  const remove = useMutation({
    mutationFn: (id: number) => deleteExpense(id),
    onSuccess: () => invalidateExpensesAndAnalytics(queryClient),
  })

  return { create, update, remove }
}

export function mutationMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) return error.body.message
  if (error instanceof Error) return error.message
  return fallback
}