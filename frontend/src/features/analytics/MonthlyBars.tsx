import type { MonthlySummaryResponse } from '../../types/api'
import { formatInr } from '../../utils/money'
import styles from './Charts.module.css'

interface MonthlyBarsProps {
  months: { label: string; total: number; current?: boolean }[]
}

export function MonthlyBars({ months }: MonthlyBarsProps) {
  const max = Math.max(...months.map((m) => m.total), 1)

  return (
    <div className={styles.bars}>
      {months.map((m) => (
        <div key={m.label} className={styles.barCol} title={formatInr(m.total)}>
          <span
            className={`${styles.barFill} ${m.current ? styles.barFillCurrent : ''}`}
            style={{ height: `${Math.max(4, (m.total / max) * 100)}%` }}
          />
          <span className={styles.barLabel}>{m.label}</span>
        </div>
      ))}
    </div>
  )
}

export function monthlyBarsFromSummaries(
  pairs: { year: number; month: number; label: string; current?: boolean }[],
  summaries: (MonthlySummaryResponse | undefined)[],
): MonthlyBarsProps['months'] {
  return pairs.map((p, i) => ({
    label: p.label,
    total: summaries[i]?.totalAmount ?? 0,
    current: p.current,
  }))
}