import { CATEGORY_LABELS, type ExpenseCategory } from '../../types/api'
import { formatInr } from '../../utils/money'
import styles from './SummaryCards.module.css'

interface SummaryCardsProps {
  total: number
  expenseCount: number
  changeFromPrevious: number
  highestCategory: ExpenseCategory | null
  highestAmount?: number
  averagePerDay?: number
}

export function SummaryCards({
  total,
  expenseCount,
  changeFromPrevious,
  highestCategory,
  highestAmount,
  averagePerDay,
}: SummaryCardsProps) {
  const changeClass = changeFromPrevious > 0 ? styles.up : styles.down
  const changeLabel =
    changeFromPrevious === 0
      ? 'Same as last month'
      : `${formatInr(Math.abs(changeFromPrevious))} ${changeFromPrevious > 0 ? 'more' : 'less'} than last month`

  return (
    <div className={styles.grid}>
      <div className={styles.card}>
        <div className={styles.label}>Spent this month</div>
        <div className={`${styles.value} num`}>{formatInr(total)}</div>
        <div className={`${styles.hint} ${changeClass} num`}>{changeLabel}</div>
      </div>
      <div className={styles.card}>
        <div className={styles.label}>Expenses logged</div>
        <div className={`${styles.value} num`}>{expenseCount}</div>
        {averagePerDay !== undefined && (
          <div className={`${styles.hint} num`}>Average {formatInr(averagePerDay)} per day</div>
        )}
      </div>
      <div className={styles.card}>
        <div className={styles.label}>Highest category</div>
        <div className={styles.value}>{highestCategory ? CATEGORY_LABELS[highestCategory] : '—'}</div>
        {highestCategory && highestAmount !== undefined && (
          <div className={`${styles.hint} num`}>{formatInr(highestAmount)}</div>
        )}
      </div>
    </div>
  )
}