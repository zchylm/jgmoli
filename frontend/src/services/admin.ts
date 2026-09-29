import { clearToken } from './auth'
import { apiUrl } from './api'

export type AdminOrderSummary = {
  id: string
  orderReference: string
  customerName: string
  customerEmail: string
  status: string
  fulfillmentStatus: string
  currency: string
  totalCents: number
  itemCount: number
  createdAt: string
  paidAt: string | null
}

export type AdminOrderDetail = {
  summary: AdminOrderSummary
  phone: string
  addressLine1: string
  addressLine2: string | null
  suburb: string
  state: string
  postcode: string
  countryCode: string
  carrier: string | null
  trackingNumber: string | null
  paymentReference: string | null
  paymentStatus: string | null
  invoiceNumber: string | null
  items: Array<{
    sku: string
    brand: string
    productName: string
    variantName: string
    quantity: number
    unitPriceCents: number
    gstCents: number
    lineTotalCents: number
  }>
}

export type AdminOverview = {
  ordersToday: number
  paidRevenueTodayCents: number
  awaitingPayment: number
  needsFulfilment: number
  lowStockProducts: number
  recentOrders: AdminOrderSummary[]
}

export type AdminProduct = {
  productId: string
  variantId: string
  category: string
  brand: string
  name: string
  subtype: string
  sku: string
  status: string
  active: boolean
  priceCents: number
  currency: string
}

export type AdminInventory = {
  itemId: string
  sku: string
  brand: string
  productName: string
  productType: string
  linkedToCatalog: boolean
  locationCode: string
  locationName: string
  onHand: number
  reserved: number
  available: number
  reorderLevel: number
  updatedAt: string
}

export type AdminMovement = {
  id: string
  sku: string
  movementType: string
  onHandDelta: number
  reservedDelta: number
  reason: string
  performedBy: string
  createdAt: string
}

type ApiError = { message?: string }

async function adminRequest<T>(token: string, path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(apiUrl(`/api/admin${path}`), {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...options.headers,
    },
  })
  if (!response.ok) {
    if (response.status === 401) clearToken()
    const error = await response.json().catch(() => ({})) as ApiError
    throw new Error(error.message || 'The admin workspace is temporarily unavailable.')
  }
  return response.json() as Promise<T>
}

export const getAdminOverview = (token: string) => adminRequest<AdminOverview>(token, '/overview')
export const getAdminOrders = (token: string, query = '', status = '') => adminRequest<AdminOrderSummary[]>(token, `/orders?query=${encodeURIComponent(query)}&status=${encodeURIComponent(status)}`)
export const getAdminOrder = (token: string, reference: string) => adminRequest<AdminOrderDetail>(token, `/orders/${encodeURIComponent(reference)}`)
export const getAdminProducts = (token: string) => adminRequest<AdminProduct[]>(token, '/products')
export const getAdminInventory = (token: string) => adminRequest<AdminInventory[]>(token, '/inventory')
export const getAdminMovements = (token: string) => adminRequest<AdminMovement[]>(token, '/inventory/movements')

export const updateFulfilment = (token: string, reference: string, status: string, carrier: string, trackingNumber: string) => adminRequest<AdminOrderDetail>(token, `/orders/${encodeURIComponent(reference)}/fulfillment`, {
  method: 'PATCH',
  body: JSON.stringify({ status, carrier, trackingNumber }),
})

export const updateAdminProduct = (token: string, variantId: string, priceCents: number, status: string, active: boolean) => adminRequest<AdminProduct>(token, `/products/${variantId}`, {
  method: 'PATCH',
  body: JSON.stringify({ priceCents, status, active }),
})

export const adjustAdminInventory = (token: string, itemId: string, movementType: string, quantityDelta: number, reason: string, reorderLevel: number) => adminRequest<AdminInventory>(token, `/inventory/${itemId}`, {
  method: 'PATCH',
  headers: { 'Idempotency-Key': crypto.randomUUID() },
  body: JSON.stringify({ movementType, quantityDelta, reason, reorderLevel }),
})

export const createAdminInventoryItem = (token: string, item: {
  sku: string
  brand: string
  productName: string
  categoryCode: string
  productType: string
  initialQuantity: number
  reorderLevel: number
  reason: string
}) => adminRequest<AdminInventory>(token, '/inventory/items', {
  method: 'POST',
  body: JSON.stringify(item),
})
