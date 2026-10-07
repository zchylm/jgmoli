export type StoreRoute =
  | 'home'
  | 'recommendation'
  | 'cockpit-compare'
  | 'racer-compare'
  | 'arena-compare'
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

export type RecommendationNavigationData = {
  startingPoint: string | null
  experience: string | null
  category: string | null
}

type StaticStoreRoute = Exclude<StoreRoute, 'home' | 'recommendation'>

const routeHashes: Record<StaticStoreRoute, string> = {
  'cockpit-compare': '#/cockpit/compare',
  'racer-compare': '#/racer/compare',
  'arena-compare': '#/arena/compare',
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
  if (hash === '#/recommendation' || hash.startsWith('#/recommendation?')) return 'recommendation'
  const entry = Object.entries(routeHashes).find(([, value]) => value === hash)
  return entry?.[0] as StoreRoute | undefined ?? 'home'
}

export function readStoreNavigation<T = unknown>(): StoreNavigationState<T>['jgmoli'] {
  return (window.history.state as StoreNavigationState<T> | null)?.jgmoli
}

export function pushStoreRoute<T = unknown>(route: StaticStoreRoute, data?: T) {
  window.history.pushState(
    { ...(window.history.state ?? {}), jgmoli: { owned: true, route, data } },
    '',
    routeHashes[route],
  )
}

export function replaceStoreRoute<T = unknown>(route: StaticStoreRoute, data?: T) {
  const owned = readStoreNavigation()?.owned ?? false
  window.history.replaceState(
    { ...(window.history.state ?? {}), jgmoli: { owned, route, data } },
    '',
    routeHashes[route],
  )
}

function recommendationHash(data: RecommendationNavigationData) {
  const params = new URLSearchParams()
  if (data.startingPoint) params.set('device', data.startingPoint)
  if (data.experience) params.set('experience', data.experience)
  if (data.category) params.set('category', data.category)
  const query = params.toString()
  return `#/recommendation${query ? `?${query}` : ''}`
}

export function readRecommendationNavigation(): RecommendationNavigationData {
  const query = window.location.hash.split('?')[1] ?? ''
  const params = new URLSearchParams(query)
  return {
    startingPoint: params.get('device'),
    experience: params.get('experience'),
    category: params.get('category'),
  }
}

export function pushRecommendationRoute(data: RecommendationNavigationData) {
  window.history.pushState(
    { ...(window.history.state ?? {}), jgmoli: { owned: true, route: 'recommendation', data } },
    '',
    recommendationHash(data),
  )
}

export function replaceWithStoreHome() {
  const state = { ...(window.history.state ?? {}) } as StoreNavigationState
  delete state.jgmoli
  window.history.replaceState(state, '', '#top')
}
