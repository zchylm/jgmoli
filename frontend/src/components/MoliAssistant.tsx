import { useEffect, useRef, useState, type FormEvent } from 'react'
import { askMoli, type AssistantReply, type ChatTurn } from '../services/ai'
import './MoliAssistant.css'

type Message = {
  id: string
  role: 'user' | 'assistant'
  content: string
  context: boolean
}

const starterQuestions = [
  'Which complete system fits my space?',
  'Compare Cockpit, Racer and Arena',
  'Upgrade the setup I already own',
]

function Orb({ compact = false }: { compact?: boolean }) {
  return (
    <span className={compact ? 'moli-orb moli-orb-compact' : 'moli-orb'} aria-hidden="true">
      <span className="moli-orb-ring" />
      <span className="moli-orb-core">
        <span className="moli-orb-flow moli-orb-flow-back" />
        <span className="moli-orb-flow moli-orb-flow-front" />
        <span className="moli-orb-glass" />
      </span>
    </span>
  )
}

export function MoliAssistant({ raised = false }: { raised?: boolean }) {
  const [isOpen, setIsOpen] = useState(false)
  const [question, setQuestion] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [status, setStatus] = useState<'ready' | 'live' | 'unavailable'>('ready')
  const [messages, setMessages] = useState<Message[]>([])
  const messagesRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (isOpen) inputRef.current?.focus()
  }, [isOpen])

  useEffect(() => {
    messagesRef.current?.scrollTo({ top: messagesRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, isLoading])

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setIsOpen(false)
    }
    document.addEventListener('keydown', closeOnEscape)
    return () => document.removeEventListener('keydown', closeOnEscape)
  }, [])

  const submitQuestion = async (value: string) => {
    const trimmed = value.trim()
    if (!trimmed || isLoading) return

    const history: ChatTurn[] = messages
      .filter((message) => message.context)
      .map((message) => ({ role: message.role, content: message.content }))
    const userMessageId = crypto.randomUUID()
    setMessages((current) => [...current, {
      id: userMessageId,
      role: 'user',
      content: trimmed,
      context: true,
    }])
    setQuestion('')
    setIsLoading(true)

    try {
      const answer: AssistantReply = await askMoli(trimmed, history)
      const content = [answer.body, ...(answer.bullets ?? []).map((bullet) => `- ${bullet}`)].join('\n')
      setMessages((current) => [...current, {
        id: crypto.randomUUID(),
        role: 'assistant',
        content,
        context: answer.source === 'claude',
      }])
      setStatus('live')
    } catch (error) {
      setMessages((current) => [
        ...current.map((message) => message.id === userMessageId ? { ...message, context: false } : message),
        {
          id: crypto.randomUUID(),
          role: 'assistant',
          content: error instanceof Error ? error.message : 'MOLI AI is unavailable right now.',
          context: false,
        },
      ])
      setStatus('unavailable')
    } finally {
      setIsLoading(false)
    }
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void submitQuestion(question)
  }

  const resetConversation = () => {
    setMessages([])
    setQuestion('')
    setStatus('ready')
    inputRef.current?.focus()
  }

  const statusLabel = status === 'live' ? 'Connected' : status === 'unavailable' ? 'Unavailable' : 'Ready'

  return (
    <div className={`moli-assistant${raised ? ' moli-assistant-raised' : ''}${isOpen ? ' moli-assistant-open' : ''}`}>
      {isOpen && (
        <section className="moli-panel" aria-label="JG MOLI product and setup advisor">
          <header className="moli-panel-header">
            <div className="moli-panel-identity">
              <Orb compact />
              <div>
                <span>MOLI AI / Setup advisor</span>
                <h2>Find your way into the experience.</h2>
              </div>
            </div>
            <div className="moli-panel-actions">
              <button type="button" onClick={resetConversation} aria-label="Start a new conversation" title="Start a new conversation">↺</button>
              <button type="button" onClick={() => setIsOpen(false)} aria-label="Close MOLI AI">×</button>
            </div>
            <p className={`moli-status moli-status-${status}`}><i />{statusLabel}</p>
          </header>

          {messages.length === 0 && !isLoading && (
            <div className="moli-welcome">
              <p>Tell me who will use the space, what you want to experience, or what you already own.</p>
              <div className="moli-starters" aria-label="Suggested questions">
                {starterQuestions.map((starter) => (
                  <button key={starter} type="button" onClick={() => void submitQuestion(starter)}>
                    {starter}<span aria-hidden="true">↗</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {(messages.length > 0 || isLoading) && (
            <div className="moli-messages" aria-live="polite" ref={messagesRef}>
              {messages.map((message) => (
                <article className={`moli-message moli-message-${message.role}`} key={message.id}>
                  <span>{message.role === 'assistant' ? 'MOLI AI' : 'You'}</span>
                  <p>{message.content}</p>
                </article>
              ))}
              {isLoading && (
                <div className="moli-thinking" aria-label="MOLI AI is thinking">
                  <i /><i /><i /><span>Finding the right path</span>
                </div>
              )}
            </div>
          )}

          <form className="moli-input" onSubmit={handleSubmit}>
            <label className="moli-sr-only" htmlFor="moli-question">Ask MOLI AI a question</label>
            <input
              id="moli-question"
              ref={inputRef}
              value={question}
              onChange={(event) => setQuestion(event.target.value)}
              placeholder="Ask about complete systems, gear or your space"
              autoComplete="off"
              maxLength={2000}
            />
            <button type="submit" aria-label="Send question" disabled={isLoading || !question.trim()}>↗</button>
          </form>
          <small className="moli-disclaimer">Product guidance only. Confirm final compatibility before purchase.</small>
        </section>
      )}

      <button
        className="moli-launcher"
        type="button"
        onClick={() => setIsOpen((current) => !current)}
        aria-expanded={isOpen}
        aria-label={isOpen ? 'Close MOLI AI' : 'Open JG MOLI product and setup advisor'}
      >
        <span className="moli-launcher-label">Ask MOLI AI</span>
        <Orb />
      </button>
    </div>
  )
}
