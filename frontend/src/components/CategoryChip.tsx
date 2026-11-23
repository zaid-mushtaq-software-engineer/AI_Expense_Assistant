import { CATEGORY_COLORS } from '../utils/categories'
import { CATEGORY_LABELS, type ExpenseCategory } from '../types/api'
import styles from './CategoryChip.module.css'

export function CategoryChip({ category }: { category: ExpenseCategory }) {
  return (
    <span className={styles.chip}>
      <span className={styles.dot} style={{ background: CATEGORY_COLORS[category] }} aria-hidden />
      {CATEGORY_LABELS[category]}
    </span>
  )
}