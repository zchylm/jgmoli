export type StoreRoute =
  | 'home'
  | 'shop'
  | 'cart'
  | 'account'
  | 'orders'
  | 'checkout-delivery'
  | 'checkout-review'
  | 'checkout-complete'
  | 'invoice'

export type StoreNavigationState<T = unknown> = {
  jgmoli?: {
    owned: boolean
    route: StoreRoute
    data?: T
  }
}

const routeHashes: Record<Exclude<StoreRoute, 'home'>, string> = {
  shop: '#/shop',
  cart: '#/cart',
  account: '#/account',
  orders: '#/orders',
  'checkout-delivery': '#/checkout/delivery',
  'checkout-review': '#/checkout/review',
  'checkout-complete': '#/checkout/complete',
  invoice: '#/invoice',
}

export function readStoreRoute(hash = window.location.hash): StoreRoute {
  const entry = Object.entries(routeHashes).find(([, value]) => value === hash)
  return entry?.[0] as StoreRoute | undefined ?? 'home'
}

export function readStoreNavigation<T = unknown>(): StoreNavigationState<T>['jgmoli'] {
  return (window.history.state as StoreNavigationState<T> | null)?.jgmoli
}

export function pushStoreRoute<T = unknown>(route: Exclude<StoreRoute, 'home'>, data?: T) {
  window.history.pushState(
    { ...(window.history.state ?? {}), jgmoli: { owned: true, route, data } },
    '',
    routeHashes[route],
  )
}

export function replaceStoreRoute<T = unknown>(route: Exclude<StoreRoute, 'home'>, data?: T) {
  const owned = readStoreNavigation()?.owned ?? false
  window.history.replaceState(
    { ...(window.history.state ?? {}), jgmoli: { owned, route, data } },
    '',
    routeHashes[route],
  )
}

export function replaceWithStoreHome() {
  const state = { ...(window.history.state ?? {}) } as StoreNavigationState
  delete state.jgmoli
  window.history.replaceState(state, '', '#top')
}
