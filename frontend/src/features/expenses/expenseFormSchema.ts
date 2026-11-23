import { z } from 'zod'
import type { CreateExpenseRequest } from '../../types/api'
import { EXPENSE_CATEGORIES, type ExpenseCategory } from '../../types/api'
import { maxExpenseDateIso } from '../../utils/dates'

export const expenseFormSchema = z.object({
  amount: z
    .string()
    .min(1, 'Amount is required')
    .refine((v) => /^\d+(\.\d{1,2})?$/.test(v), 'Use at most 2 decimal places')
    .refine((v) => Number(v) > 0, 'Amount must be greater than 0')
    .refine((v) => Number(v) <= 9999999.99, 'Amount is too large'),
  category: z.enum(EXPENSE_CATEGORIES as [string, ...string[]], { error: 'Category is required' }),
  description: z.string().max(255, 'Description is too long').optional(),
  expenseDate: z
    .string()
    .min(1, 'Date is required')
    .refine((d) => d <= maxExpenseDateIso(), 'Date cannot be more than one day in the future'),
})

export type ExpenseFormValues = z.infer<typeof expenseFormSchema>

export function toCreatePayload(values: ExpenseFormValues): CreateExpenseRequest {
  return {
    amount: Number(Number(values.amount).toFixed(2)),
    category: values.category as ExpenseCategory,
    description: values.description?.trim() || undefined,
    expenseDate: values.expenseDate,
  }
}