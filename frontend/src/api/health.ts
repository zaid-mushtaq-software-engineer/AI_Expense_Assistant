export interface HealthResponse {
  status: string
  components?: {
    db?: { status: string }
  }
}

function backendOrigin(): string {
  const base = import.meta.env.VITE_API_BASE_URL
  if (!base) throw new Error('VITE_API_BASE_URL is not set')
  return base.replace(/\/api\/v1\/?$/, '')
}

export async function fetchBackendHealth(): Promise<HealthResponse> {
  const response = await fetch(`${backendOrigin()}/actuator/health`)
  if (!response.ok) {
    throw new Error(`Health check failed (${response.status})`)
  }
  return response.json() as Promise<HealthResponse>
}