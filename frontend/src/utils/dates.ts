const dateDisplay = new Intl.DateTimeFormat('en-IN', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
})

const dateTimeDisplay = new Intl.DateTimeFormat('en-IN', {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatDate(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number)
  return dateDisplay.format(new Date(y, m - 1, d))
}

export function formatDateTime(iso: string): string {
  return dateTimeDisplay.format(new Date(iso))
}

export function toIsoDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function monthBounds(year: number, month: number): { from: string; to: string } {
  const from = new Date(year, month - 1, 1)
  const to = new Date(year, month, 0)
  return { from: toIsoDate(from), to: toIsoDate(to) }
}

export function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  date.setDate(date.getDate() + days)
  return toIsoDate(date)
}

export function todayIso(): string {
  return toIsoDate(new Date())
}

export function maxExpenseDateIso(): string {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  return toIsoDate(d)
}

export function monthYearLabel(year: number, month: number): string {
  return new Intl.DateTimeFormat('en-IN', { month: 'long', year: 'numeric' }).format(
    new Date(year, month - 1, 1),
  )
}