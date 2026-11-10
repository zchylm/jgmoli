import { useEffect, useRef, useState } from 'react'
import type { Order } from '../services/checkout'
import './PaymentResultDialog.css'

type Props = {
  message: string
  order: Order | null
  canResume: boolean
  busy: boolean
  onClose: () => void
  onOrders: () => void
  onInvoice: () => Promise<void>
  onResume: () => Promise<void>
}

export function PaymentResultDialog({ message, order, canResume, busy, onClose, onOrders, onInvoice, onResume }: Props) {
  const dialog = useRef<HTMLDivElement>(null)
  const close = useRef(onClose)
  const [error, setError] = useState('')
  const [openingInvoice, setOpeningInvoice] = useState(false)
  const paid = order?.status === 'PAID'
  useEffect(() => { close.current = onClose }, [onClose])

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    dialog.current?.focus({ preventScroll: true })
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') close.current()
      if (event.key !== 'Tab') return
      const elements = Array.from(dialog.current?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)') ?? [])
      const first = elements[0], last = elements[elements.length - 1]
      if (!first) { event.preventDefault(); return }
      if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog.current)) {
        event.preventDefault(); last.focus()
      } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialog.current)) {
        event.preventDefault(); first.focus()
      }
    }
    document.addEventListener('keydown', handleKey)
    return () => {
      document.removeEventListener('keydown', handleKey)
      if (previous?.isConnected) previous.focus({ preventScroll: true })
    }
  }, [])

  return <div className="payment-result-backdrop">
    <div ref={dialog} className="payment-result" role="dialog" aria-modal="true" aria-labelledby="payment-result-title" aria-describedby="payment-result-description" tabIndex={-1}>
      <button className="payment-result-close" type="button" aria-label="Close payment confirmation" onClick={onClose}>×</button>
      <span className="payment-result-brand">JG MOLI</span>
      <div className="payment-result-symbol" aria-hidden="true">{paid ? '✓' : '—'}</div>
      <p className="eyebrow">{paid ? 'PAYMENT CONFIRMED' : 'YOUR ORDER'}</p>
      <h2 id="payment-result-title">{paid ? 'Your world awaits.' : 'Let’s check your payment.'}</h2>
      <p id="payment-result-description" role="status">{paid ? 'Thank you for choosing JG MOLI. Your order is confirmed and your tax invoice is ready.' : message}</p>
      {order && <dl>
        <div><dt>Order</dt><dd>{order.orderReference}</dd></div>
        <div><dt>{paid ? 'Amount paid' : 'Order total'}</dt><dd>{new Intl.NumberFormat('en-AU', { style: 'currency', currency: order.currency }).format(order.totalCents / 100)}</dd></div>
      </dl>}
      {error && <p className="checkout-error" role="alert">{error}</p>}
      <div className="payment-result-actions">
        {paid ? <button className="checkout-primary" type="button" disabled={openingInvoice} onClick={async () => {
          setOpeningInvoice(true); setError('')
          try { await onInvoice() } catch (e) { setError(e instanceof Error ? e.message : 'Your invoice is temporarily unavailable.') }
          finally { setOpeningInvoice(false) }
        }}>{openingInvoice ? 'Opening…' : 'View tax invoice'} <span aria-hidden="true">→</span></button>
          : canResume && <button className="checkout-primary" type="button" disabled={busy} onClick={onResume}>{busy ? 'Opening…' : 'Continue payment'} <span aria-hidden="true">→</span></button>}
        <button className="checkout-secondary" type="button" onClick={onOrders}>View my orders</button>
      </div>
      <button className="payment-result-continue" type="button" onClick={onClose}>Continue exploring <span aria-hidden="true">→</span></button>
    </div>
  </div>
}
