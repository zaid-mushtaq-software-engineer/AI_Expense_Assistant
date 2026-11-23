export interface HealthResponse {
  status: string;
  components?: {
    db?: { status: string };
  };
}

export async function fetchBackendHealth(): Promise<HealthResponse> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL;
  if (!baseUrl) {
    throw new Error('VITE_API_BASE_URL is not set');
  }
  const response = await fetch(`${baseUrl}/actuator/health`);
  if (!response.ok) {
    throw new Error(`Health check failed (${response.status})`);
  }
  return response.json() as Promise<HealthResponse>;
}