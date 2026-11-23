import { addDays, monthBounds, todayIso } from '../../utils/dates'

export type RangePreset = 'this-month' | 'last-month' | 'last-30' | 'custom'

export function resolveRange(
  preset: RangePreset,
  customFrom?: string,
  customTo?: string,
): { from: string; to: string } {
  const today = new Date()
  if (preset === 'this-month') {
    return monthBounds(today.getFullYear(), today.getMonth() + 1)
  }
  if (preset === 'last-month') {
    const last = new Date(today.getFullYear(), today.getMonth() - 1, 1)
    return monthBounds(last.getFullYear(), last.getMonth() + 1)
  }
  if (preset === 'last-30') {
    const to = todayIso()
    const from = addDays(to, -29)
    return { from, to }
  }
  const todayStr = todayIso()
  return { from: customFrom || todayStr, to: customTo || todayStr }
}