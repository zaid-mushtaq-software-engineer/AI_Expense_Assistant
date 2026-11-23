import { useCallback, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '../../components/Button'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { EmptyState } from '../../components/EmptyState'
import { ErrorBanner } from '../../components/ErrorBanner'
import { SkeletonRows } from '../../components/Skeleton'
import { useToast } from '../../components/Toast'
import { useIsDesktopNav } from '../../hooks/useMediaQuery'
import type { ExpenseResponse } from '../../types/api'
import { formatDate } from '../../utils/dates'
import { formatInr } from '../../utils/money'
import { AddExpenseModal } from './AddExpenseModal'
import { EditExpenseModal } from './EditExpenseModal'
import { ExpenseFilters } from './ExpenseFilters'
import { ExpenseTable } from './ExpenseTable'
import {
  hasActiveFilters,
  parseExpenseListParams,
  toSearchParams,
  type ExpenseListParams,
} from './expenseListParams'
import { ExpenseForm } from './ExpenseForm'
import type { ExpenseFormValues } from './expenseFormSchema'
import { toCreatePayload } from './expenseFormSchema'
import styles from './ExpensesPage.module.css'
import { useExpenses } from './hooks/useExpenses'
import { ApiError } from '../../api/client'
import { mutationMessage, useExpenseMutations } from './useExpenseMutations'

export function ExpensesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const params = useMemo(() => parseExpenseListParams(searchParams), [searchParams])
  const { data, isLoading, isError, error, refetch, isFetching } = useExpenses(params)
  const { create, update, remove } = useExpenseMutations()
  const { showToast } = useToast()
  const navigate = useNavigate()
  const isDesktop = useIsDesktopNav()

  const [addOpen, setAddOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<ExpenseResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ExpenseResponse | null>(null)
  const [formError, setFormError] = useState<string | null>(null)

  const updateParams = useCallback(
    (patch: Partial<ExpenseListParams>) => {
      const next = { ...params, ...patch }
      setSearchParams(toSearchParams(next), { replace: true })
    },
    [params, setSearchParams],
  )

  const clearFilters = useCallback(() => {
    setSearchParams(new URLSearchParams(), { replace: true })
  }, [setSearchParams])

  const handleAdd = async (values: ExpenseFormValues) => {
    setFormError(null)
    try {
      await create.mutateAsync(toCreatePayload(values))
      showToast('Expense added')
      setAddOpen(false)
    } catch (e) {
      setFormError(mutationMessage(e, 'Could not add expense'))
      throw e
    }
  }

  const handleEdit = async (values: ExpenseFormValues) => {
    if (!editTarget) return
    setFormError(null)
    try {
      await update.mutateAsync({ id: editTarget.id, body: toCreatePayload(values) })
      showToast('Expense updated')
      setEditTarget(null)
    } catch (e) {
      setFormError(mutationMessage(e, 'Could not update expense'))
      throw e
    }
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    try {
      await remove.mutateAsync(deleteTarget.id)
      showToast('Expense deleted')
      setDeleteTarget(null)
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) {
        showToast('Expense was already deleted')
        setDeleteTarget(null)
        await refetch()
      } else {
        showToast(mutationMessage(e, 'Delete failed'))
      }
    }
  }

  const onEditClick = (expense: ExpenseResponse) => {
    if (isDesktop) {
      setFormError(null)
      setEditTarget(expense)
    } else {
      navigate(`/expenses/${expense.id}/edit`)
    }
  }

  const errorMessage =
    error instanceof Error ? error.message : 'Could not load expenses'

  return (
    <div>
      <div className={styles.top}>
        <div>
          <h1>Expenses</h1>
          <p className={styles.sub}>Search, filter, and manage your spending</p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            if (isDesktop) {
              setFormError(null)
              setAddOpen(true)
            } else {
              navigate('/expenses/new')
            }
          }}
        >
          Add expense
        </Button>
      </div>

      <ExpenseFilters
        params={params}
        onChange={updateParams}
        onClear={clearFilters}
        showClear={hasActiveFilters(params)}
      />

      {isError && <ErrorBanner message={errorMessage} onRetry={() => refetch()} />}

      {isLoading && <SkeletonRows count={8} />}

      {!isLoading && !isError && data && data.content.length === 0 && (
        <EmptyState
          title={hasActiveFilters(params) ? 'No matches' : 'No expenses yet'}
          description={
            hasActiveFilters(params)
              ? 'Try clearing filters or broadening your search.'
              : 'Add your first expense to get started.'
          }
          action={
            hasActiveFilters(params) ? (
              <Button onClick={clearFilters}>Clear filters</Button>
            ) : (
              <Button variant="primary" onClick={() => (isDesktop ? setAddOpen(true) : navigate('/expenses/new'))}>
                Add expense
              </Button>
            )
          }
        />
      )}

      {!isLoading && data && data.content.length > 0 && (
        <>
          <ExpenseTable
            expenses={data.content}
            onEdit={onEditClick}
            onDelete={(e) => setDeleteTarget(e)}
          />
          <div className={styles.pagination}>
            <span>
              Page {data.page + 1} of {Math.max(1, data.totalPages)} · {data.totalElements} total
              {isFetching && ' · Updating…'}
            </span>
            <div className={styles.pager}>
              <Button
                disabled={data.page <= 0}
                onClick={() => updateParams({ page: data.page - 1 })}
              >
                Previous
              </Button>
              <Button
                disabled={data.page + 1 >= data.totalPages}
                onClick={() => updateParams({ page: data.page + 1 })}
              >
                Next
              </Button>
            </div>
          </div>
        </>
      )}

      <AddExpenseModal
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onSubmit={handleAdd}
        formError={formError}
      />
      <EditExpenseModal
        open={Boolean(editTarget)}
        expense={editTarget}
        onClose={() => setEditTarget(null)}
        onSubmit={handleEdit}
        formError={formError}
      />
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title="Delete expense?"
        message={
          deleteTarget
            ? `Delete ${formatInr(deleteTarget.amount)} ${deleteTarget.description || 'expense'} on ${formatDate(deleteTarget.expenseDate)}?`
            : ''
        }
        confirmLabel="Delete"
        busy={remove.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
      />
    </div>
  )
}

export function AddExpensePage() {
  const navigate = useNavigate()
  const { create } = useExpenseMutations()
  const { showToast } = useToast()
  const [formError, setFormError] = useState<string | null>(null)

  const onSubmit = async (values: ExpenseFormValues) => {
    setFormError(null)
    try {
      await create.mutateAsync(toCreatePayload(values))
      showToast('Expense added')
      navigate('/expenses')
    } catch (e) {
      setFormError(mutationMessage(e, 'Could not add expense'))
      throw e
    }
  }

  return (
    <div>
      <div className={styles.top}>
        <div>
          <h1>Add expense</h1>
          <p className={styles.sub}>
            <Link to="/expenses">← Back to list</Link>
          </p>
        </div>
      </div>
      <ExpenseForm onSubmit={onSubmit} onCancel={() => navigate('/expenses')} submitLabel="Add expense" formError={formError} />
    </div>
  )
}