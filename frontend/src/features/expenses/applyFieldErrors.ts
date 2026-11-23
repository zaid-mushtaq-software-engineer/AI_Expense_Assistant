import type { UseFormSetError } from 'react-hook-form'
import { ApiError } from '../../api/client'
import type { ExpenseFormValues } from './expenseFormSchema'

export function applyFieldErrors(error: unknown, setError: UseFormSetError<ExpenseFormValues>) {
  if (!(error instanceof ApiError) || !error.body.fieldErrors?.length) {
    return false
  }
  for (const fe of error.body.fieldErrors) {
    const field = fe.field as keyof ExpenseFormValues
    if (field in expenseFormFields) {
      setError(field, { message: fe.message })
    }
  }
  return true
}

const expenseFormFields = {
  amount: true,
  category: true,
  description: true,
  expenseDate: true,
} as const