import { apiUrl } from './api'

export type HealthStatus = {
  service: string
  status: 'UP'
}

export async function getHealth(signal?: AbortSignal): Promise<HealthStatus> {
  const response = await fetch(apiUrl('/api/health'), { signal })

  if (!response.ok) {
    throw new Error(`Health request failed with status ${response.status}`)
  }

  return response.json() as Promise<HealthStatus>
}
