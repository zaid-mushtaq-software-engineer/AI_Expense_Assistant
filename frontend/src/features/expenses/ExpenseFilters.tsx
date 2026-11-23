import { useEffect, useRef, useState } from 'react'
import { Button } from '../../components/Button'
import { CATEGORY_LABELS, EXPENSE_CATEGORIES } from '../../types/api'
import type { ExpenseListParams } from './expenseListParams'
import { SORT_OPTIONS } from './expenseListParams'
import styles from './ExpenseFilters.module.css'

interface ExpenseFiltersProps {
  params: ExpenseListParams
  onChange: (next: Partial<ExpenseListParams>) => void
  onClear: () => void
  showClear: boolean
}

export function ExpenseFilters({ params, onChange, onClear, showClear }: ExpenseFiltersProps) {
  const [searchDraft, setSearchDraft] = useState(params.q ?? '')
  const paramsRef = useRef(params)
  const onChangeRef = useRef(onChange)
  paramsRef.current = params
  onChangeRef.current = onChange

  useEffect(() => {
    setSearchDraft(params.q ?? '')
  }, [params.q])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      const trimmed = searchDraft.trim()
      if ((paramsRef.current.q ?? '') !== trimmed) {
        onChangeRef.current({ q: trimmed || undefined, page: 0 })
      }
    }, 300)
    return () => window.clearTimeout(timer)
  }, [searchDraft])

  return (
    <div className={styles.bar}>
      <input
        className={styles.search}
        type="search"
        placeholder="Search description…"
        aria-label="Search"
        value={searchDraft}
        onChange={(e) => setSearchDraft(e.target.value)}
      />
      <select
        aria-label="Category"
        value={params.category ?? ''}
        onChange={(e) => onChange({ category: e.target.value || undefined, page: 0 })}
      >
        <option value="">All categories</option>
        {EXPENSE_CATEGORIES.map((c) => (
          <option key={c} value={c}>
            {CATEGORY_LABELS[c]}
          </option>
        ))}
      </select>
      <input
        type="date"
        aria-label="From date"
        value={params.from ?? ''}
        onChange={(e) => onChange({ from: e.target.value || undefined, page: 0 })}
      />
      <input
        type="date"
        aria-label="To date"
        value={params.to ?? ''}
        onChange={(e) => onChange({ to: e.target.value || undefined, page: 0 })}
      />
      <select
        aria-label="Sort"
        value={params.sort}
        onChange={(e) => onChange({ sort: e.target.value, page: 0 })}
      >
        {SORT_OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {showClear && (
        <Button type="button" onClick={onClear}>
          Clear filters
        </Button>
      )}
    </div>
  )
}