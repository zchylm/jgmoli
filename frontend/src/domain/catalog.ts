export type GamingExperience =
  | 'competitive'
  | 'immersive'
  | 'racing'
  | 'flight'
  | 'streaming'

export type GamingPlatform = 'laptop' | 'desktop' | 'console' | 'starting-fresh'

export interface CatalogProduct {
  id: string
  slug: string
  name: string
  category: string
  summary: string
  priceAud: number
  availability: 'in-stock' | 'preorder' | 'unavailable'
  specifications: Record<string, string | number | boolean>
  compatiblePlatforms: GamingPlatform[]
  experiences: GamingExperience[]
  recommendationTags: string[]
}
