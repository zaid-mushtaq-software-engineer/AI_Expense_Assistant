import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ErrorBanner } from '../../components/ErrorBanner'
import { SkeletonRows } from '../../components/Skeleton'
import { useToast } from '../../components/Toast'
import { ApiError } from '../../api/client'
import { ExpenseForm } from './ExpenseForm'
import type { ExpenseFormValues } from './expenseFormSchema'
import { toCreatePayload } from './expenseFormSchema'
import styles from './ExpensesPage.module.css'
import { useExpense } from './hooks/useExpense'
import { mutationMessage, useExpenseMutations } from './useExpenseMutations'

export function EditExpensePage() {
  const { id } = useParams()
  const expenseId = Number(id)
  const navigate = useNavigate()
  const { data, isLoading, isError, error, refetch } = useExpense(expenseId)
  const { update } = useExpenseMutations()
  const { showToast } = useToast()
  const [formError, setFormError] = useState<string | null>(null)

  if (isLoading) return <SkeletonRows count={4} />

  if (isError) {
    const notFound = error instanceof ApiError && error.status === 404
    return (
      <div>
        <ErrorBanner
          message={notFound ? 'This expense no longer exists.' : mutationMessage(error, 'Could not load expense')}
          onRetry={notFound ? undefined : () => refetch()}
        />
        <Link to="/expenses">← Back to list</Link>
      </div>
    )
  }

  if (!data) return null

  const onSubmit = async (values: ExpenseFormValues) => {
    setFormError(null)
    try {
      await update.mutateAsync({ id: expenseId, body: toCreatePayload(values) })
      showToast('Expense updated')
      navigate(`/expenses/${expenseId}`)
    } catch (e) {
      setFormError(mutationMessage(e, 'Could not update expense'))
      throw e
    }
  }

  return (
    <div>
      <div className={styles.top}>
        <div>
          <h1>Edit expense</h1>
          <p className={styles.sub}>
            <Link to={`/expenses/${expenseId}`}>← Back to details</Link>
          </p>
        </div>
      </div>
      <ExpenseForm
        key={data.updatedAt}
        initial={data}
        onSubmit={onSubmit}
        onCancel={() => navigate(`/expenses/${expenseId}`)}
        submitLabel="Save changes"
        formError={formError}
      />
    </div>
  )
}