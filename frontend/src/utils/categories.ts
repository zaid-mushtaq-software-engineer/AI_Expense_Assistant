import type { ExpenseCategory } from '../types/api'

export const CATEGORY_HEX: Record<ExpenseCategory, string> = {
  FOOD: '#f59e0b',
  TRANSPORTATION: '#3b82f6',
  SHOPPING: '#ec4899',
  ENTERTAINMENT: '#8b5cf6',
  HEALTH: '#10b981',
  EDUCATION: '#06b6d4',
  BILLS: '#ef4444',
  TRAVEL: '#f97316',
  OTHER: '#94a3b8',
}

export const CATEGORY_COLORS: Record<ExpenseCategory, string> = {
  FOOD: 'var(--cat-food)',
  TRANSPORTATION: 'var(--cat-transportation)',
  SHOPPING: 'var(--cat-shopping)',
  ENTERTAINMENT: 'var(--cat-entertainment)',
  HEALTH: 'var(--cat-health)',
  EDUCATION: 'var(--cat-education)',
  BILLS: 'var(--cat-bills)',
  TRAVEL: 'var(--cat-travel)',
  OTHER: 'var(--cat-other)',
}