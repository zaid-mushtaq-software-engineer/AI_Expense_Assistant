import type { ButtonHTMLAttributes } from 'react'
import styles from './Button.module.css'

type Variant = 'default' | 'primary' | 'danger' | 'ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: 'default' | 'small'
}

export function Button({
  variant = 'default',
  size = 'default',
  className = '',
  children,
  ...props
}: ButtonProps) {
  const classes = [
    styles.button,
    variant === 'primary' && styles.primary,
    variant === 'danger' && styles.danger,
    variant === 'ghost' && styles.ghost,
    size === 'small' && styles.small,
    className,
  ]
    .filter(Boolean)
    .join(' ')
  return (
    <button type="button" className={classes} {...props}>
      {children}
    </button>
  )
}