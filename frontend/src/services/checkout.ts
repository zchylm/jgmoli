import { apiUrl } from './api'

export type CheckoutSource = 'CART' | 'BUY_NOW'

export type CheckoutLine = {
  variantId: string
  quantity: number
}

export type DeliveryDetails = {
  recipientName: string
  phone: string
  addressLine1: string
  addressLine2: string
  suburb: string
  state: string
  postcode: string
  countryCode: 'AU'
}

export type OrderLine = {
  variantId: string
  sku: string
  brand: string
  productName: string
  variantName: string
  quantity: number
  unitPriceCents: number
  gstCents: number
  lineTotalCents: number
}

export type Order = {
  id: string
  orderReference: string
  source: CheckoutSource
  status: string
  currency: string
  subtotalExGstCents: number
  gstCents: number
  deliveryCents: number
  totalCents: number
  customerEmail: string
  delivery: DeliveryDetails
  items: OrderLine[]
  createdAt: string
  paidAt: string | null
}

export type Payment = {
  paymentId: string
  paymentReference: string
  orderReference: string
  orderStatus: string
  paymentStatus: string
  amountCents: number
  currency: string
  paidAt: string
}

export type InvoiceAddress = {
  addressLine1: string
  addressLine2: string | null
  suburb: string
  state: string
  postcode: string
  countryCode: string
}

export type InvoiceLine = {
  lineNumber: number
  sku: string
  description: string
  quantity: number
  unitPriceExGstCents: number
  gstCents: number
  lineTotalIncGstCents: number
  taxable: boolean
}

export type Invoice = {
  id: string
  invoiceNumber: string
  documentType: 'TAX_INVOICE'
  status: string
  orderReference: string
  paymentReference: string
  sellerLegalName: string
  sellerTradingName: string
  sellerAbn: string
  sellerAddress: string
  sellerEmail: string
  sellerPhone: string
  buyerName: string
  buyerEmail: string
  buyerAddress: InvoiceAddress
  currency: string
  subtotalExGstCents: number
  gstCents: number
  deliveryCents: number
  totalCents: number
  amountPaidCents: number
  issuedAt: string
  lines: InvoiceLine[]
}

type ApiError = { message?: string }

async function request<T>(path: string, token: string, idempotencyKey: string, body: unknown): Promise<T> {
  const response = await fetch(apiUrl(`/api${path}`), {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as ApiError
    throw new Error(error.message || 'Checkout is unavailable right now. Try again.')
  }
  return response.json() as Promise<T>
}

export function createOrder(
  token: string,
  idempotencyKey: string,
  source: CheckoutSource,
  items: CheckoutLine[],
  delivery: DeliveryDetails,
) {
  return request<Order>('/checkout/orders', token, idempotencyKey, { source, items, delivery })
}

export function completeDemoPayment(token: string, idempotencyKey: string, orderReference: string) {
  return request<Payment>('/payments/demo', token, idempotencyKey, { orderReference })
}

export async function getInvoice(token: string, orderReference: string) {
  const response = await fetch(apiUrl(`/api/invoices/orders/${encodeURIComponent(orderReference)}`), {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as ApiError
    throw new Error(error.message || 'Your payment is confirmed, but the invoice is temporarily unavailable.')
  }
  return response.json() as Promise<Invoice>
}

export async function getOrders(token: string) {
  const response = await fetch(apiUrl('/api/orders'), {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({})) as ApiError
    throw new Error(error.message || 'Your orders are temporarily unavailable.')
  }
  return response.json() as Promise<Order[]>
}
