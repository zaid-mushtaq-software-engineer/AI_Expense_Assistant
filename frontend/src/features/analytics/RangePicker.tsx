import type { RangePreset } from './rangePresets'
import styles from './RangePicker.module.css'

interface RangePickerProps {
  preset: RangePreset
  from: string
  to: string
  onPresetChange: (preset: RangePreset) => void
  onCustomChange: (from: string, to: string) => void
}

const PRESETS: { id: RangePreset; label: string }[] = [
  { id: 'this-month', label: 'This month' },
  { id: 'last-month', label: 'Last month' },
  { id: 'last-30', label: 'Last 30 days' },
  { id: 'custom', label: 'Custom' },
]

export function RangePicker({ preset, from, to, onPresetChange, onCustomChange }: RangePickerProps) {
  return (
    <div className={styles.wrap}>
      <div className={styles.seg} role="tablist" aria-label="Date range presets">
        {PRESETS.map((p) => (
          <button
            key={p.id}
            type="button"
            role="tab"
            aria-selected={preset === p.id}
            className={preset === p.id ? styles.on : ''}
            onClick={() => onPresetChange(p.id)}
          >
            {p.label}
          </button>
        ))}
      </div>
      {preset === 'custom' && (
        <div className={styles.custom}>
          <input type="date" value={from} onChange={(e) => onCustomChange(e.target.value, to)} aria-label="From" />
          <input type="date" value={to} onChange={(e) => onCustomChange(from, e.target.value)} aria-label="To" />
        </div>
      )}
    </div>
  )
}