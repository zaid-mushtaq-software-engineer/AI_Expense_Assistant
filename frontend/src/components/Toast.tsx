import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import styles from './Toast.module.css'

type ToastMessage = { id: number; text: string }

interface ToastContextValue {
  showToast: (text: string) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [messages, setMessages] = useState<ToastMessage[]>([])

  const showToast = useCallback((text: string) => {
    const id = Date.now()
    setMessages((prev) => [...prev, { id, text }])
    window.setTimeout(() => {
      setMessages((prev) => prev.filter((m) => m.id !== id))
    }, 3200)
  }, [])

  const value = useMemo(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className={styles.host} aria-live="polite">
        {messages.map((m) => (
          <div key={m.id} className={styles.toast}>
            {m.text}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}