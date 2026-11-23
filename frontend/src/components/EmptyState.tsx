import styles from './EmptyState.module.css'

interface EmptyStateProps {
  title: string
  description?: string
  action?: React.ReactNode
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className={styles.root}>
      <b className={styles.title}>{title}</b>
      {description && <p>{description}</p>}
      {action && <div style={{ marginTop: 12 }}>{action}</div>}
    </div>
  )
}