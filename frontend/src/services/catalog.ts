import { apiUrl } from './api'

export type CatalogProduct = {
  id: string
  variantId: string
  slug: string
  sku: string
  category: 'displays' | 'controls' | 'audio' | 'sim' | 'furniture'
  categoryName: string
  subtype: string
  brand: string
  name: string
  description: string
  imageKey: string
  priceCents: number
  currency: string
  priceIncludesGst: boolean
}

export async function getCatalogProducts(): Promise<CatalogProduct[]> {
  const response = await fetch(apiUrl('/api/catalog/products'))
  if (!response.ok) {
    throw new Error('The gear catalogue is unavailable right now.')
  }
  return response.json() as Promise<CatalogProduct[]>
}
