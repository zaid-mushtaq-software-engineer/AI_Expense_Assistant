import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button } from '../../components/Button'
import { CategoryChip } from '../../components/CategoryChip'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { EmptyState } from '../../components/EmptyState'
import { ErrorBanner } from '../../components/ErrorBanner'
import { MoneyText } from '../../components/MoneyText'
import { SkeletonRows } from '../../components/Skeleton'
import { useToast } from '../../components/Toast'
import { ApiError } from '../../api/client'
import { CATEGORY_LABELS } from '../../types/api'
import { formatDate, formatDateTime } from '../../utils/dates'
import { formatInr } from '../../utils/money'
import { useIsDesktopNav } from '../../hooks/useMediaQuery'
import { EditExpenseModal } from './EditExpenseModal'
import styles from './ExpenseDetailsPage.module.css'
import pageStyles from './ExpensesPage.module.css'
import type { ExpenseFormValues } from './expenseFormSchema'
import { toCreatePayload } from './expenseFormSchema'
import { useExpense } from './hooks/useExpense'
import { useExpenses } from './hooks/useExpenses'
import { mutationMessage, useExpenseMutations } from './useExpenseMutations'

export function ExpenseDetailsPage() {
  const { id } = useParams()
  const expenseId = Number(id)
  const navigate = useNavigate()
  const isDesktop = useIsDesktopNav()
  const { data, isLoading, isError, error, refetch } = useExpense(expenseId)
  const sameDayQuery = useExpenses(
    {
      from: data?.expenseDate,
      to: data?.expenseDate,
      page: 0,
      size: 20,
      sort: 'expenseDate,desc',
    },
    { enabled: Boolean(data?.expenseDate) },
  )
  const { update, remove } = useExpenseMutations()
  const { showToast } = useToast()
  const [editOpen, setEditOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
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

  const sameDay = (sameDayQuery.data?.content ?? []).filter((e) => e.id !== data.id)

  const onEdit = () => {
    if (isDesktop) {
      setFormError(null)
      setEditOpen(true)
    } else {
      navigate(`/expenses/${data.id}/edit`)
    }
  }

  const handleUpdate = async (values: ExpenseFormValues) => {
    setFormError(null)
    try {
      await update.mutateAsync({ id: data.id, body: toCreatePayload(values) })
      showToast('Expense updated')
      setEditOpen(false)
    } catch (e) {
      setFormError(mutationMessage(e, 'Could not update expense'))
      throw e
    }
  }

  const handleDelete = async () => {
    try {
      await remove.mutateAsync(data.id)
      showToast('Expense deleted')
      navigate('/expenses')
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        showToast('Expense was already deleted')
        navigate('/expenses')
      } else {
        showToast(mutationMessage(e, 'Delete failed'))
      }
    }
  }

  return (
    <div>
      <div className={pageStyles.top}>
        <div>
          <h1>Expense details</h1>
          <p className={pageStyles.sub}>
            <Link to="/expenses">← Back to list</Link>
          </p>
        </div>
        <div className={styles.actions}>
          <Button onClick={onEdit}>Edit</Button>
          <Button variant="danger" onClick={() => setDeleteOpen(true)}>
            Delete
          </Button>
        </div>
      </div>

      <div className={styles.card}>
        <div className={styles.amount}>
          <MoneyText amount={data.amount} />
        </div>
        <CategoryChip category={data.category} />
        <dl className={styles.meta}>
          <div>
            <dt>Description</dt>
            <dd>{data.description || '—'}</dd>
          </div>
          <div>
            <dt>Date</dt>
            <dd className="num">{formatDate(data.expenseDate)}</dd>
          </div>
          <div>
            <dt>Category</dt>
            <dd>{CATEGORY_LABELS[data.category]}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd className="num">{formatDateTime(data.createdAt)}</dd>
          </div>
          <div>
            <dt>Updated</dt>
            <dd className="num">{formatDateTime(data.updatedAt)}</dd>
          </div>
        </dl>
      </div>

      <section className={styles.sameDay}>
        <h2>Same day</h2>
        {sameDayQuery.isLoading && <SkeletonRows count={2} />}
        {!sameDayQuery.isLoading && sameDay.length === 0 && (
          <EmptyState title="No other expenses" description="Nothing else logged on this date." />
        )}
        <ul className={styles.sameList}>
          {sameDay.map((e) => (
            <li key={e.id}>
              <Link to={`/expenses/${e.id}`}>{e.description || 'Expense'}</Link>
              <span className="num">
                <MoneyText amount={e.amount} />
              </span>
            </li>
          ))}
        </ul>
      </section>

      <EditExpenseModal
        open={editOpen}
        expense={data}
        onClose={() => setEditOpen(false)}
        onSubmit={handleUpdate}
        formError={formError}
      />
      <ConfirmDialog
        open={deleteOpen}
        title="Delete expense?"
        message={`Delete ${formatInr(data.amount)} ${data.description || 'expense'} on ${formatDate(data.expenseDate)}?`}
        confirmLabel="Delete"
        busy={remove.isPending}
        onCancel={() => setDeleteOpen(false)}
        onConfirm={handleDelete}
      />
    </div>
  )
}