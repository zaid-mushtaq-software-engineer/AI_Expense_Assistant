import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { CATEGORY_HEX } from '../../utils/categories'
import { CATEGORY_LABELS, type CategoryTotal } from '../../types/api'
import { formatInr } from '../../utils/money'
import styles from './Charts.module.css'

export function CategoryDonut({ categories }: { categories: CategoryTotal[] }) {
  if (categories.length === 0) {
    return <p className={styles.emptyChart}>No category data for this period.</p>
  }

  const data = categories.map((c) => ({
    name: CATEGORY_LABELS[c.category],
    value: c.amount,
    category: c.category,
  }))

  return (
    <div className={styles.donutWrap}>
      <div className={styles.chartBox}>
        <ResponsiveContainer width="100%" height={200}>
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" innerRadius={52} outerRadius={80}>
              {data.map((entry) => (
                <Cell key={entry.category} fill={CATEGORY_HEX[entry.category]} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => formatInr(Number(value ?? 0))} />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <div className={styles.legend}>
        {categories.map((c) => (
          <div key={c.category} className={`${styles.legendRow} num`}>
            <span>
              <span className={styles.dot} style={{ background: CATEGORY_HEX[c.category] }} />
              {CATEGORY_LABELS[c.category]}
            </span>
            <b>{formatInr(c.amount)}</b>
          </div>
        ))}
      </div>
    </div>
  )
}