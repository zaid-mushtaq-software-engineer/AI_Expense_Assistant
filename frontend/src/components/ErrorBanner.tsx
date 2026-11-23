import { Button } from './Button'
import styles from './ErrorBanner.module.css'

interface ErrorBannerProps {
  message: string
  onRetry?: () => void
}

export function ErrorBanner({ message, onRetry }: ErrorBannerProps) {
  return (
    <div className={styles.banner} role="alert">
      <span>{message}</span>
      {onRetry && (
        <Button size="small" onClick={onRetry}>
          Retry
        </Button>
      )}
    </div>
  )
}