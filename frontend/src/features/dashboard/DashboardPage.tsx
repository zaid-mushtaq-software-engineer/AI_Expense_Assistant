import { useMemo, useState } from 'react'
import { useToast } from '../../components/Toast'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Button } from '../../components/Button'
import { ErrorBanner } from '../../components/ErrorBanner'
import { MoneyText } from '../../components/MoneyText'
import { SkeletonRows } from '../../components/Skeleton'
import { listExpenses } from '../../api/expenses.api'
import { queryKeys } from '../../api/queryKeys'
import { monthYearLabel } from '../../utils/dates'
import { formatDate } from '../../utils/dates'
import { CategoryDonut } from '../analytics/CategoryDonut'
import { MonthlyBars, monthlyBarsFromSummaries } from '../analytics/MonthlyBars'
import { getLastSixMonthPairs, useLastSixMonths } from '../analytics/hooks/useLastSixMonths'
import { useMonthlySummary } from '../analytics/hooks/useMonthlySummary'
import { SummaryCards } from '../analytics/SummaryCards'
import { AddExpenseModal } from '../expenses/AddExpenseModal'
import type { ExpenseFormValues } from '../expenses/expenseFormSchema'
import { toCreatePayload } from '../expenses/expenseFormSchema'
import { mutationMessage, useExpenseMutations } from '../expenses/useExpenseMutations'
import { useIsDesktopNav } from '../../hooks/useMediaQuery'
import { useNavigate } from 'react-router-dom'
import styles from './DashboardPage.module.css'

export function DashboardPage() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const monthly = useMonthlySummary(year, month)
  const sixQueries = useLastSixMonths(now)
  const recent = useQuery({
    queryKey: queryKeys.expenses({ page: 0, size: 5, sort: 'expenseDate,desc' }),
    queryFn: () => listExpenses({ page: 0, size: 5, sort: 'expenseDate,desc' }),
  })

  const [addOpen, setAddOpen] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const { create } = useExpenseMutations()
  const { showToast } = useToast()
  const isDesktop = useIsDesktopNav()
  const navigate = useNavigate()

  const monthPairs = useMemo(() => {
    return getLastSixMonthPairs(now).map((p) => ({
      ...p,
      label: new Intl.DateTimeFormat('en-IN', { month: 'short' }).format(new Date(p.year, p.month - 1, 1)),
      current: p.year === year && p.month === month,
    }))
  }, [now, year, month])

  const barMonths = monthlyBarsFromSummaries(
    monthPairs,
    sixQueries.map((q) => q.data),
  )

  const highestAmount = monthly.data?.categories.find((c) => c.category === monthly.data?.highestCategory)?.amount

  const daysInMonth = new Date(year, month, 0).getDate()
  const averagePerDay =
    monthly.data && daysInMonth > 0 ? monthly.data.totalAmount / daysInMonth : 0

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

  const loading = monthly.isLoading || recent.isLoading

  return (
    <div>
      <div className={styles.top}>
        <div>
          <h1>{monthYearLabel(year, month)}</h1>
          <p className={styles.sub}>Your spending so far this month</p>
        </div>
        <div className={styles.actions}>
          <Button disabled title="Coming in Stage 9">
            Ask the assistant
          </Button>
          <Button
            variant="primary"
            onClick={() => (isDesktop ? setAddOpen(true) : navigate('/expenses/new'))}
          >
            Add expense
          </Button>
        </div>
      </div>

      {(monthly.isError || recent.isError) && (
        <ErrorBanner
          message="Some dashboard data could not be loaded."
          onRetry={() => {
            monthly.refetch()
            recent.refetch()
          }}
        />
      )}

      {loading && <SkeletonRows count={6} />}

      {monthly.data && (
        <>
          <SummaryCards
            total={monthly.data.totalAmount}
            expenseCount={monthly.data.expenseCount}
            changeFromPrevious={monthly.data.changeFromPreviousMonth}
            highestCategory={monthly.data.highestCategory}
            highestAmount={highestAmount}
            averagePerDay={averagePerDay}
          />

          <div className={styles.grid2}>
            <section className={styles.card}>
              <h2>Where the money went</h2>
              <CategoryDonut categories={monthly.data.categories} />
            </section>
            <section className={styles.card}>
              <h2>Last 6 months</h2>
              <MonthlyBars months={barMonths} />
            </section>
          </div>

          <section className={`${styles.card}`} style={{ marginTop: 16 }}>
            <h2>Recent expenses</h2>
            {recent.data && recent.data.content.length === 0 && (
              <p style={{ color: 'var(--mut)' }}>No expenses yet.</p>
            )}
            {recent.data && recent.data.content.length > 0 && (
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Description</th>
                    <th className={styles.right}>Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {recent.data.content.map((e) => (
                    <tr key={e.id}>
                      <td className="num">{formatDate(e.expenseDate)}</td>
                      <td>
                        <Link to={`/expenses/${e.id}`}>{e.description || 'Expense'}</Link>
                      </td>
                      <td className={styles.right}>
                        <MoneyText amount={e.amount} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>
        </>
      )}

      <AddExpenseModal open={addOpen} onClose={() => setAddOpen(false)} onSubmit={handleAdd} formError={formError} />
    </div>
  )
}