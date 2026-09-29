export type CartLine = {
  variantId: string
  quantity: number
}

const CART_KEY = 'jgmoli.cart'

export function loadCart(): CartLine[] {
  try {
    const stored = JSON.parse(window.localStorage.getItem(CART_KEY) ?? '[]') as unknown
    if (!Array.isArray(stored)) return []

    return stored.filter((line): line is CartLine => (
      typeof line === 'object'
      && line !== null
      && typeof (line as CartLine).variantId === 'string'
      && Number.isInteger((line as CartLine).quantity)
      && (line as CartLine).quantity > 0
    ))
  } catch {
    return []
  }
}

export function saveCart(lines: CartLine[]) {
  window.localStorage.setItem(CART_KEY, JSON.stringify(lines))
}
