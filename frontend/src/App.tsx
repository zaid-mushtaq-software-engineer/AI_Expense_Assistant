import { useEffect, useState } from 'react'
import { fetchBackendHealth, type HealthResponse } from './api/health'
import './App.css'

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const apiBase = import.meta.env.VITE_API_BASE_URL ?? '(not set)'

  useEffect(() => {
    let cancelled = false
    fetchBackendHealth()
      .then((data) => {
        if (!cancelled) {
          setHealth(data)
          setError(null)
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setHealth(null)
          setError(err instanceof Error ? err.message : 'Request failed')
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <main className="health-page">
      <h1>AI Expense Assistant</h1>
      <p className="subtitle">Stage 0C — backend connectivity</p>
      <p className="meta">
        API base: <code>{apiBase}</code>
      </p>

      {loading && <p className="status loading">Checking backend…</p>}

      {!loading && error && (
        <p className="status error" role="alert">
          {error}
        </p>
      )}

      {!loading && health && (
        <div className="status success">
          <p>
            Backend status: <strong>{health.status}</strong>
          </p>
          {health.components?.db && (
            <p>
              Database: <strong>{health.components.db.status}</strong>
            </p>
          )}
        </div>
      )}
    </main>
  )
}

export default App