import { Link } from 'react-router-dom'
import { Button } from '../../components/Button'
import { CategoryChip } from '../../components/CategoryChip'
import { MoneyText } from '../../components/MoneyText'
import type { ExpenseResponse } from '../../types/api'
import { formatDate } from '../../utils/dates'
import styles from './ExpenseTable.module.css'

interface ExpenseTableProps {
  expenses: ExpenseResponse[]
  onEdit: (expense: ExpenseResponse) => void
  onDelete: (expense: ExpenseResponse) => void
}

export function ExpenseTable({ expenses, onEdit, onDelete }: ExpenseTableProps) {
  return (
    <>
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Date</th>
              <th>Description</th>
              <th>Category</th>
              <th className={styles.right}>Amount</th>
              <th className={styles.right}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {expenses.map((e) => (
              <tr key={e.id}>
                <td className="num">{formatDate(e.expenseDate)}</td>
                <td>
                  <Link className={styles.rowLink} to={`/expenses/${e.id}`}>
                    {e.description || '—'}
                  </Link>
                </td>
                <td>
                  <CategoryChip category={e.category} />
                </td>
                <td className={styles.right}>
                  <MoneyText amount={e.amount} />
                </td>
                <td className={styles.right}>
                  <div className={styles.actions}>
                    <Button size="small" onClick={() => onEdit(e)}>
                      Edit
                    </Button>
                    <Button size="small" variant="danger" onClick={() => onDelete(e)}>
                      Delete
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className={styles.cards}>
        {expenses.map((e) => (
          <article key={e.id} className={styles.card}>
            <div className={styles.cardTop}>
              <div>
                <Link to={`/expenses/${e.id}`}>
                  <strong>{e.description || 'Expense'}</strong>
                </Link>
                <div className="small" style={{ color: 'var(--mut)', fontSize: 13 }}>
                  {formatDate(e.expenseDate)}
                </div>
              </div>
              <MoneyText amount={e.amount} />
            </div>
            <CategoryChip category={e.category} />
            <div className={styles.actions}>
              <Button size="small" onClick={() => onEdit(e)}>
                Edit
              </Button>
              <Button size="small" variant="danger" onClick={() => onDelete(e)}>
                Delete
              </Button>
            </div>
          </article>
        ))}
      </div>
    </>
  )
}