import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Button } from '../../components/Button'
import { Input } from '../../components/Input'
import { Select } from '../../components/Select'
import { CATEGORY_LABELS, EXPENSE_CATEGORIES, type ExpenseResponse } from '../../types/api'
import { todayIso } from '../../utils/dates'
import { ApiError } from '../../api/client'
import { applyFieldErrors } from './applyFieldErrors'
import { expenseFormSchema, type ExpenseFormValues } from './expenseFormSchema'
import styles from './ExpenseForm.module.css'

interface ExpenseFormProps {
  initial?: ExpenseResponse
  onSubmit: (values: ExpenseFormValues) => Promise<void>
  onCancel?: () => void
  submitLabel?: string
  formError?: string | null
}

function toDefaults(initial?: ExpenseResponse): ExpenseFormValues {
  if (initial) {
    return {
      amount: String(initial.amount),
      category: initial.category,
      description: initial.description ?? '',
      expenseDate: initial.expenseDate,
    }
  }
  return {
    amount: '',
    category: 'FOOD',
    description: '',
    expenseDate: todayIso(),
  }
}

export function ExpenseForm({
  initial,
  onSubmit,
  onCancel,
  submitLabel = 'Save',
  formError,
}: ExpenseFormProps) {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ExpenseFormValues>({
    resolver: zodResolver(expenseFormSchema),
    defaultValues: toDefaults(initial),
  })

  return (
    <form
      className={styles.form}
      onSubmit={handleSubmit(async (values) => {
        try {
          await onSubmit(values)
        } catch (e) {
          if (!applyFieldErrors(e, setError) && e instanceof ApiError) {
            throw e
          }
        }
      })}
      noValidate
    >
      {formError && (
        <p className={styles.formError} role="alert">
          {formError}
        </p>
      )}
      <Input
        label="Amount (₹)"
        type="text"
        inputMode="decimal"
        {...register('amount')}
        error={errors.amount?.message}
      />
      <Select label="Category" {...register('category')} error={errors.category?.message}>
        {EXPENSE_CATEGORIES.map((c) => (
          <option key={c} value={c}>
            {CATEGORY_LABELS[c]}
          </option>
        ))}
      </Select>
      <Input
        label="Description (optional)"
        {...register('description')}
        error={errors.description?.message}
      />
      <Input
        label="Date"
        type="date"
        {...register('expenseDate')}
        error={errors.expenseDate?.message}
      />
      <div className={styles.actions}>
        {onCancel && (
          <Button type="button" onClick={onCancel} disabled={isSubmitting}>
            Cancel
          </Button>
        )}
        <Button type="submit" variant="primary" disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : submitLabel}
        </Button>
      </div>
    </form>
  )
}