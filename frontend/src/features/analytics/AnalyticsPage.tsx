import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ErrorBanner } from '../../components/ErrorBanner'
import { MoneyText } from '../../components/MoneyText'
import { SkeletonRows } from '../../components/Skeleton'
import { CATEGORY_LABELS } from '../../types/api'
import { ApiError } from '../../api/client'
import { fetchMonthly } from '../../api/analytics.api'
import { queryKeys } from '../../api/queryKeys'
import { monthYearLabel } from '../../utils/dates'
import { formatDate } from '../../utils/dates'
import { CategoryDonut } from './CategoryDonut'
import { RangePicker } from './RangePicker'
import { resolveRange, type RangePreset } from './rangePresets'
import { useAnalyticsReport } from './hooks/useAnalyticsReport'
import { MonthlyBars, monthlyBarsFromSummaries } from './MonthlyBars'
import { getLastSixMonthPairs } from './hooks/useLastSixMonths'
import styles from './AnalyticsPage.module.css'
import dashStyles from '../dashboard/DashboardPage.module.css'

export function AnalyticsPage() {
  const [preset, setPreset] = useState<RangePreset>('this-month')
  const [customFrom, setCustomFrom] = useState('')
  const [customTo, setCustomTo] = useState('')

  const range = useMemo(
    () => resolveRange(preset, customFrom, customTo),
    [preset, customFrom, customTo],
  )

  const report = useAnalyticsReport(range.from, range.to)
  const now = new Date()
  const monthlyOverview = useQuery({
    queryKey: queryKeys.analyticsMonthly(now.getFullYear(), now.getMonth() + 1),
    queryFn: () => fetchMonthly(now.getFullYear(), now.getMonth() + 1),
  })

  const sixPairs = getLastSixMonthPairs(now).map((p) => ({
    ...p,
    label: new Intl.DateTimeFormat('en-IN', { month: 'short' }).format(new Date(p.year, p.month - 1, 1)),
    current: false,
  }))

  const sixMonths = useQuery({
    queryKey: ['analytics', 'overview', 'six'],
    queryFn: async () => {
      const results = await Promise.all(sixPairs.map((p) => fetchMonthly(p.year, p.month)))
      return monthlyBarsFromSummaries(sixPairs, results)
    },
  })

  const validationError =
    report.error instanceof ApiError && report.error.status === 400 ? report.error.message : null

  return (
    <div>
      <div className={styles.top}>
        <div>
          <h1>Analytics</h1>
          <p className={styles.sub}>Totals, categories, and trends</p>
        </div>
      </div>

      <RangePicker
        preset={preset}
        from={range.from}
        to={range.to}
        onPresetChange={setPreset}
        onCustomChange={(from, to) => {
          setCustomFrom(from)
          setCustomTo(to)
          setPreset('custom')
        }}
      />

      {validationError && <ErrorBanner message={validationError} />}

      {report.isLoading && <SkeletonRows count={6} />}

      {report.isError && !validationError && (
        <ErrorBanner message="Could not load analytics." onRetry={() => report.refetch()} />
      )}

      {report.data && (
        <>
          <div className={styles.summaryRow}>
            <div className={styles.stat}>
              <div className={styles.k}>Total</div>
              <div className={`${styles.v} num`}>
                <MoneyText amount={report.data.totalAmount} />
              </div>
              <div className={styles.small}>
                {report.data.expenseCount} expenses · avg{' '}
                <MoneyText amount={report.data.averagePerDay} /> / day
              </div>
            </div>
            <div className={styles.stat}>
              <div className={styles.k}>Highest category</div>
              <div className={styles.v}>
                {report.data.highestCategory ? CATEGORY_LABELS[report.data.highestCategory] : '—'}
              </div>
            </div>
          </div>

          <section className={dashStyles.card} style={{ marginBottom: 16 }}>
            <h2>By category</h2>
            <CategoryDonut categories={report.data.categories} />
            <table className={dashStyles.table}>
              <thead>
                <tr>
                  <th>Category</th>
                  <th className={dashStyles.right}>%</th>
                  <th className={dashStyles.right}>Amount</th>
                </tr>
              </thead>
              <tbody>
                {report.data.categories.map((c) => (
                  <tr
                    key={c.category}
                    className={
                      c.category === report.data?.highestCategory ? styles.highlightRow : undefined
                    }
                  >
                    <td>{CATEGORY_LABELS[c.category]}</td>
                    <td className={`${dashStyles.right} num`}>{c.percentage.toFixed(2)}%</td>
                    <td className={dashStyles.right}>
                      <MoneyText amount={c.amount} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

          <section className={dashStyles.card} style={{ marginBottom: 16 }}>
            <h2>Largest expenses</h2>
            {report.data.topExpenses.length === 0 && (
              <p style={{ color: 'var(--mut)' }}>No expenses in this range.</p>
            )}
            <ul className={styles.topList}>
              {report.data.topExpenses.map((e) => (
                <li key={e.id}>
                  <Link to={`/expenses/${e.id}`}>{e.description || 'Expense'}</Link>
                  <span className="num">
                    {formatDate(e.expenseDate)} · <MoneyText amount={e.amount} />
                  </span>
                </li>
              ))}
            </ul>
          </section>
        </>
      )}

      <section className={dashStyles.card}>
        <h2>Monthly overview</h2>
        {monthlyOverview.data && (
          <p className={styles.sub}>
            {monthYearLabel(monthlyOverview.data.year, monthlyOverview.data.month)}:{' '}
            <MoneyText amount={monthlyOverview.data.totalAmount} /> ({monthlyOverview.data.expenseCount}{' '}
            expenses)
          </p>
        )}
        {sixMonths.data && <MonthlyBars months={sixMonths.data} />}
      </section>
    </div>
  )
}