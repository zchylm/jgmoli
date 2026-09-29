import { apiUrl } from './api'

export type ChatTurn = {
  role: 'user' | 'assistant'
  content: string
}

export type AssistantReply = {
  title: string
  body: string
  bullets: string[]
  source: string
}

export async function askMoli(message: string, history: ChatTurn[]): Promise<AssistantReply> {
  const response = await fetch(apiUrl('/api/ai/chat'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, history }),
  })

  if (!response.ok) {
    throw new Error(response.status === 503
      ? 'MOLI AI is not available yet. Please try again shortly.'
      : 'MOLI AI could not answer that request.')
  }
  return response.json() as Promise<AssistantReply>
}
