import { formatInr } from '../utils/money'

export function MoneyText({ amount, className = '' }: { amount: number; className?: string }) {
  return (
    <span className={`num ${className}`.trim()}>{formatInr(amount)}</span>
  )
}