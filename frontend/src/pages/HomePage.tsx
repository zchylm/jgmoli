import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import competitiveHero from '../assets/site/hero-competitive-v3.jpg'
import competitiveHeroAlternate from '../assets/site/hero-competitive-frame-b-v2.jpg'
import immersiveHero from '../assets/site/hero-immersive-v3.jpg'
import immersiveHeroAlternate from '../assets/site/hero-immersive-frame-b-v1.jpg'
import racingHero from '../assets/site/hero-racing-v3.jpg'
import racingHeroAlternate from '../assets/site/hero-racing-frame-b-v6.png'
import arenaHero from '../assets/site/hero-arena-v4.jpg'
import arenaHeroAlternate from '../assets/site/hero-arena-frame-b-v1.jpg'
import gearHero from '../assets/site/hero-gear-v1.jpg'
import gearHeroAlternate from '../assets/site/hero-gear-frame-b-v1.jpg'
import cockpitImage from '../assets/site/moli-cockpit-v2.jpg'
import cockpitPlusImage from '../assets/site/moli-cockpit-plus-v2.jpg'
import cockpitProImage from '../assets/site/moli-cockpit-pro-v2.jpg'
import cockpitUltraImage from '../assets/site/moli-cockpit-ultra-v2.jpg'
import racerCoreImage from '../assets/site/moli-racer-core-v3.jpg'
import racerSignatureImage from '../assets/site/moli-racer-signature-v3.jpg'
import racerEliteImage from '../assets/site/moli-racer-elite-v3.jpg'
import arenaCoreImage from '../assets/site/moli-arena-core-v1.jpg'
import arenaSignatureImage from '../assets/site/moli-arena-signature-v1.jpg'
import arenaVenueImage from '../assets/site/moli-arena-venue-v1.jpg'
import competitiveExperience from '../assets/site/experience-competitive-v2.jpg'
import immersiveExperience from '../assets/site/experience-immersive-v2.jpg'
import simulationExperience from '../assets/site/experience-simulation-v3.jpg'
import streamingExperience from '../assets/site/experience-streaming-v2.jpg'
import displayGear from '../assets/site/gear-displays-v1.jpg'
import controlGear from '../assets/site/gear-controls-v1.jpg'
import audioGear from '../assets/site/gear-audio-v1.jpg'
import furnitureGear from '../assets/site/gear-furniture-v1.jpg'
import laptopStart from '../assets/site/start-laptop-v1.jpg'
import desktopStart from '../assets/site/start-desktop-v1.jpg'
import consoleStart from '../assets/site/start-console-v1.jpg'
import freshStart from '../assets/site/start-fresh-v1.jpg'
import displayHighRefresh from '../assets/catalog/display-high-refresh-v1.jpg'
import displayUltrawide from '../assets/catalog/display-ultrawide-v1.jpg'
import displayArm from '../assets/catalog/display-arm-v1.jpg'
import displayConnections from '../assets/catalog/display-connections-v1.jpg'
import controlKeyboard from '../assets/catalog/control-keyboard-v1.jpg'
import controlMouse from '../assets/catalog/control-mouse-v1.jpg'
import controlController from '../assets/catalog/control-controller-v1.jpg'
import audioHeadset from '../assets/catalog/audio-headset-v1.jpg'
import audioSpeakers from '../assets/catalog/audio-speakers-v1.jpg'
import audioMicrophone from '../assets/catalog/audio-microphone-v1.jpg'
import furnitureDesk from '../assets/catalog/furniture-desk-v1.jpg'
import furnitureChair from '../assets/catalog/furniture-chair-v1.jpg'
import furnitureLighting from '../assets/catalog/furniture-lighting-v1.jpg'
import {
  AUTH_TOKEN_STORAGE_KEY,
  clearToken,
  confirmPasswordReset,
  getCurrentUser,
  loadToken,
  login,
  register,
  requestPasswordReset,
  resendVerification,
  saveToken,
  verifyEmail,
  type AuthUser,
} from '../services/auth'
import { getCatalogProducts, type CatalogProduct } from '../services/catalog'
import { loadCart, saveCart, type CartLine } from '../services/cart'
import { MoliAssistant } from '../components/MoliAssistant'
import {
  completeDemoPayment,
  createOrder,
  getInvoice,
  getOrders,
  type CheckoutSource,
  type Invoice,
  type Order,
  type Payment,
} from '../services/checkout'
import {
  pushRecommendationRoute,
  pushStoreRoute,
  readRecommendationNavigation,
  readStoreNavigation,
  readStoreRoute,
  replaceStoreRoute,
  replaceWithStoreHome,
  type StoreRoute,
} from '../app/storeNavigation'
import './HomePage.css'

type HeroMode = 'brand' | 'cockpit' | 'racer' | 'arena' | 'setups'
type CockpitModelId = 'cockpit' | 'plus' | 'pro' | 'ultra'
type RacerModelId = 'core' | 'signature' | 'elite'
type ArenaModelId = 'core' | 'signature' | 'venue'
type StartingPoint = 'laptop' | 'desktop' | 'console' | 'fresh'
type Experience = 'competitive' | 'immersive' | 'racing' | 'streaming'
type GearTarget = 'displays' | 'controls' | 'audio' | 'sim' | 'furniture'
type CatalogView = { title: string; allowedCategories: GearTarget[]; source: 'direct' | 'recommendation'; lockedSubtype?: string }
type CatalogItem = CatalogProduct & { image: string }
type AuthMode = 'login' | 'register' | 'forgot' | 'reset'
type CheckoutDraft = { source: CheckoutSource; lines: CartLine[] }
type CheckoutStep = 'delivery' | 'review' | 'complete'
type CatalogNavigationData = { catalogView: CatalogView; category: GearTarget | null; subtype?: string | null }
type CheckoutNavigationData = { depth: number }

const deviceRecommendationProfiles: Record<StartingPoint, {
  defaultGears: GearTarget[]
  alwaysInclude: GearTarget[]
}> = {
  laptop: { defaultGears: ['displays', 'controls', 'audio'], alwaysInclude: [] },
  desktop: { defaultGears: ['displays', 'controls', 'audio', 'furniture'], alwaysInclude: [] },
  console: { defaultGears: ['controls', 'displays', 'audio'], alwaysInclude: ['controls'] },
  fresh: { defaultGears: ['displays', 'controls', 'audio', 'furniture'], alwaysInclude: [] },
}

const experienceRecommendationProfiles: Record<Experience, {
  gears: GearTarget[]
  subtypes: Partial<Record<GearTarget, string[]>>
}> = {
  competitive: {
    gears: ['displays', 'controls', 'audio'],
    subtypes: {
      displays: ['Gaming monitors'],
      controls: ['Keyboards', 'Mice', 'Controllers'],
      audio: ['Headsets', 'Microphones'],
    },
  },
  immersive: {
    gears: ['displays', 'audio', 'furniture'],
    subtypes: {
      displays: ['Gaming monitors'],
      audio: ['Headsets', 'Speakers'],
      furniture: ['Seating', 'Lighting'],
    },
  },
  racing: {
    gears: ['displays', 'audio', 'furniture'],
    subtypes: {
      displays: ['Gaming monitors', 'Monitor arms'],
      audio: ['Headsets', 'Speakers'],
      furniture: ['Seating'],
    },
  },
  streaming: {
    gears: ['audio', 'furniture'],
    subtypes: {
      audio: ['Headsets', 'Microphones'],
      furniture: ['Desks', 'Lighting'],
    },
  },
}

function uniqueGearTargets(...groups: GearTarget[][]) {
  return [...new Set(groups.flat())]
}

function productMatchesRecommendation(
  item: CatalogProduct,
  startingPoint: StartingPoint | null,
  experience: Experience | null,
  recommendedGears: GearTarget[],
) {
  if (!startingPoint && !experience) return true
  if (!recommendedGears.includes(item.category)) return false

  if (startingPoint === 'console') {
    if (item.category === 'controls' && item.subtype !== 'Controllers') return false
    if (item.category === 'displays' && item.subtype !== 'Gaming monitors') return false
  }

  if (!experience) return true

  const isDeviceEssential = startingPoint
    ? deviceRecommendationProfiles[startingPoint].alwaysInclude.includes(item.category)
    : false
  if (isDeviceEssential) return true

  const allowedSubtypes = experienceRecommendationProfiles[experience].subtypes[item.category]
  return !allowedSubtypes || allowedSubtypes.includes(item.subtype)
}

const heroModes: Record<HeroMode, {
  label: string
  descriptor: string
  summary: string
  action: string
  href: string
  note: string
  images: [string, string]
  imagePosition: string
}> = {
  brand: {
    label: 'JG MOLI',
    descriptor: 'Complete experiences',
    summary: 'Complete systems and gaming gear, shaped around how you play.',
    action: 'Explore JG MOLI',
    href: '#moli-cockpit',
    note: 'Complete. Real. Only here.',
    images: [competitiveHero, competitiveHeroAlternate],
    imagePosition: 'center 50%',
  },
  cockpit: {
    label: 'MOLI Cockpit',
    descriptor: 'Personal immersion',
    summary: 'One seat. One screen. Your own cockpit.',
    action: 'Explore MOLI Cockpit',
    href: '#moli-cockpit',
    note: 'Comfort. Focus. Immersion.',
    images: [immersiveHero, immersiveHeroAlternate],
    imagePosition: 'center 50%',
  },
  racer: {
    label: 'MOLI Racer',
    descriptor: 'Motion racing',
    summary: 'Feel every turn with motion and race-ready control.',
    action: 'Explore MOLI Racer',
    href: '#moli-racer',
    note: 'Control. Feedback. Instinct.',
    images: [racingHero, racingHeroAlternate],
    imagePosition: 'center 50%',
  },
  arena: {
    label: 'JG MOLI Arena',
    descriptor: 'Shared play',
    summary: 'Turn one room into play for everyone.',
    action: 'Explore JG MOLI Arena',
    href: '#moli-arena',
    note: 'Together. Active. Alive.',
    images: [arenaHero, arenaHeroAlternate],
    imagePosition: 'center 50%',
  },
  setups: {
    label: 'Gaming Gear',
    descriptor: 'Upgrade your setup',
    summary: 'Complete what you already own.',
    action: 'Shop gaming gear',
    href: '#shop-by-gear',
    note: 'See more. Hear more. Play better.',
    images: [gearHero, gearHeroAlternate],
    imagePosition: 'center 50%',
  },
}

const cockpitModels: Array<{
  id: CockpitModelId
  index: string
  name: string
  tier: string
  headline: string
  difference: string
  idealFor: string
  structure: string
  visualSystem: string
  priceCents: number
  image: string
  imageAlt: string
}> = [
  {
    id: 'cockpit',
    index: '01',
    name: 'MOLI Cockpit',
    tier: 'Essential',
    headline: 'A complete cockpit for your first home racing setup.',
    difference: 'Compact frame · Single curved display',
    idealFor: 'Everyday personal setups',
    structure: 'Compact open frame',
    visualSystem: 'Single curved display',
    priceCents: 1199900,
    image: cockpitImage,
    imageAlt: 'MOLI Cockpit with a compact frame and single curved display in a home gaming room.',
  },
  {
    id: 'plus',
    index: '02',
    name: 'MOLI Cockpit Plus',
    tier: 'Enhanced',
    headline: 'More room and a wider view for longer sessions.',
    difference: 'Enclosed shell · Wider curved display',
    idealFor: 'Longer immersive sessions',
    structure: 'Enveloping shell',
    visualSystem: 'Wider curved display',
    priceCents: 2499900,
    image: cockpitPlusImage,
    imageAlt: 'MOLI Cockpit Plus with a partial carbon shell and wide curved display in a home gaming room.',
  },
  {
    id: 'pro',
    index: '03',
    name: 'MOLI Cockpit Pro',
    tier: 'Core model',
    headline: 'See more of the race with a full wraparound view.',
    difference: 'Performance cockpit · Triple display',
    idealFor: 'Committed enthusiasts',
    structure: 'Performance cockpit',
    visualSystem: 'Wraparound triple display',
    priceCents: 2999900,
    image: cockpitProImage,
    imageAlt: 'MOLI Cockpit Pro with formula controls and three displays in a home gaming room.',
  },
  {
    id: 'ultra',
    index: '04',
    name: 'MOLI Cockpit Ultra',
    tier: 'Flagship',
    headline: 'Add motion for the most immersive home setup.',
    difference: 'Full motion · Triple display',
    idealFor: 'The complete home experience',
    structure: 'Adaptive flagship platform',
    visualSystem: 'Large triple-display environment',
    priceCents: 3999900,
    image: cockpitUltraImage,
    imageAlt: 'MOLI Cockpit Ultra with a full-motion platform and three large displays in a home gaming room.',
  },
]

const racerModels: Array<{
  id: RacerModelId
  index: string
  name: string
  tier: string
  headline: string
  difference: string
  idealFor: string
  motionSystem: string
  visualSystem: string
  controls: string
  priceCents: number
  image: string
  imageAlt: string
}> = [
  {
    id: 'core',
    index: '01',
    name: 'MOLI Racer Core',
    tier: 'Entry',
    headline: 'Start with a complete motion racing setup.',
    difference: '32-inch display · Direct drive · 4DOF motion',
    idealFor: 'A first complete motion setup',
    motionSystem: 'Compact calibrated 4DOF platform',
    visualSystem: '32-inch single display',
    controls: 'Entry direct-drive wheel and pedals',
    priceCents: 2690000,
    image: racerCoreImage,
    imageAlt: 'MOLI Racer Core motion simulator in a dedicated residential sim-racing room.',
  },
  {
    id: 'signature',
    index: '02',
    name: 'MOLI Racer Signature',
    tier: 'Core model',
    headline: 'A wider view and stronger controls for serious racing.',
    difference: '49-inch QLED · R5 direct drive · 4DOF motion',
    idealFor: 'Committed sim racers',
    motionSystem: 'Full calibrated 4DOF platform',
    visualSystem: '49-inch QLED 5120×1440 165Hz',
    controls: 'R5 base, ES wheel and SRP pedals',
    priceCents: 3490000,
    image: racerSignatureImage,
    imageAlt: 'MOLI Racer Signature with a 49-inch ultrawide display in a dedicated residential sim-racing room.',
  },
  {
    id: 'elite',
    index: '03',
    name: 'MOLI Racer Elite',
    tier: 'Flagship',
    headline: 'Our largest display and most advanced controls.',
    difference: '57-inch ultrawide · Hydraulic pedals · 4DOF motion',
    idealFor: 'The highest-performance home racing experience',
    motionSystem: 'Reinforced flagship 4DOF platform',
    visualSystem: '57-inch ultrawide curved display',
    controls: 'High-end DD, hydraulic pedals, shifter and handbrake',
    priceCents: 4490000,
    image: racerEliteImage,
    imageAlt: 'MOLI Racer Elite flagship simulator with a 57-inch ultrawide display in a dedicated residential sim-racing room.',
  },
]

const arenaModels: Array<{
  id: ArenaModelId
  index: string
  name: string
  tier: string
  headline: string
  difference: string
  idealFor: string
  players: string
  content: string
  installation: string
  support: string
  priceCents: number
  image: string
  imageAlt: string
}> = [
  {
    id: 'core',
    index: '01',
    name: 'JG MOLI Arena Core',
    tier: 'Home',
    headline: 'Two players. Sixty games. Ready for home.',
    difference: '2 players · 60 games',
    idealFor: 'Families creating their first shared play space',
    players: 'Two infrared light controllers',
    content: '60 perpetually licensed offline games',
    installation: 'Standard home installation and calibration',
    support: 'Two-year on-site cover',
    priceCents: 999900,
    image: arenaCoreImage,
    imageAlt: 'A family enjoying JG MOLI Arena Core together in a warm, realistic Australian living room.',
  },
  {
    id: 'signature',
    index: '02',
    name: 'JG MOLI Arena Signature',
    tier: 'Core model',
    headline: 'Bring four players into the same game.',
    difference: '4 players · 60 games · Blackout upgrade',
    idealFor: 'Families who want everyone playing together',
    players: 'Four controllers, including two custom infrared units',
    content: '60 games, first-year content updates and AI Studio',
    installation: 'Home installation with basic blackout works',
    support: 'Three-year on-site cover',
    priceCents: 1399900,
    image: arenaSignatureImage,
    imageAlt: 'A family of four playing JG MOLI Arena Signature together in a darkened home games room.',
  },
  {
    id: 'venue',
    index: '03',
    name: 'JG MOLI Arena Venue',
    tier: 'Commercial',
    headline: 'Run shared play for groups and guests.',
    difference: '6 players · QR and timer controls',
    idealFor: 'Entertainment, education and community venues',
    players: 'Six infrared light controllers',
    content: '60 offline commercial titles',
    installation: 'Commercial installation with QR or timer module',
    support: 'Operations dashboard and three-year on-site cover',
    priceCents: 1799900,
    image: arenaVenueImage,
    imageAlt: 'Six people using JG MOLI Arena Venue in a compact, professionally installed activity room.',
  },
]

const startingPoints: Array<{
  id: StartingPoint
  index: string
  label: string
  products: string[]
  image: string
  message: string
}> = [
  {
    id: 'laptop',
    index: '01',
    label: 'Gaming Laptop',
    products: ['Displays', 'Docks & cables', 'Controls', 'Audio'],
    image: laptopStart,
    message: 'Expand the device you own with the display, connection and control that fit it.',
  },
  {
    id: 'desktop',
    index: '02',
    label: 'Gaming Desktop',
    products: ['Displays', 'Controls', 'Audio', 'Desk & seating'],
    image: desktopStart,
    message: 'Build outward from your PC with clearer vision, faster control and focused sound.',
  },
  {
    id: 'console',
    index: '03',
    label: 'PlayStation / Xbox',
    products: ['4K displays', 'Controllers', 'Headsets', 'Room audio'],
    image: consoleStart,
    message: 'Bring display, control and sound together around the console you already play.',
  },
  {
    id: 'fresh',
    index: '04',
    label: 'Starting Fresh',
    products: ['Displays', 'Controls', 'Audio', 'Desk & seating'],
    image: freshStart,
    message: 'Begin with the essentials, then shape every part around the way you want to play.',
  },
]

const gearShortcuts: Array<{
  id: GearTarget
  label: string
  products: string[]
  image: string
  headline: string
  message: string
}> = [
  { id: 'displays', label: 'Displays', products: ['Gaming monitors', 'Monitor arms', 'Cables & adapters'], image: displayGear, headline: 'See the action clearly.', message: 'High-refresh motion and a wider view make every decision easier to read.' },
  { id: 'controls', label: 'Controls', products: ['Keyboards', 'Mice', 'Controllers'], image: controlGear, headline: 'Make every input intentional.', message: 'Precise, low-latency control keeps the action connected to your next move.' },
  { id: 'audio', label: 'Audio', products: ['Headsets', 'Speakers', 'Microphones'], image: audioGear, headline: 'Hear where the world is.', message: 'Directional detail turns sound into awareness, atmosphere and connection.' },
  { id: 'furniture', label: 'Furniture', products: ['Desks', 'Seating', 'Lighting'], image: furnitureGear, headline: 'Stay in the world longer.', message: 'Support, positioning and considered light keep comfort out of the way.' },
]

const catalogCategoryLabels: Record<GearTarget, string> = {
  displays: 'Displays',
  controls: 'Controls',
  audio: 'Audio',
  sim: 'Complete systems',
  furniture: 'Furniture',
}

const catalogImages: Record<string, string> = {
  'display-high-refresh': displayHighRefresh,
  'display-ultrawide': displayUltrawide,
  'display-arm': displayArm,
  'display-connect': displayConnections,
  'control-keyboard': controlKeyboard,
  'control-mouse': controlMouse,
  'control-controller': controlController,
  'audio-headset': audioHeadset,
  'audio-speakers': audioSpeakers,
  'audio-microphone': audioMicrophone,
  'moli-cockpit': cockpitImage,
  'moli-cockpit-plus': cockpitPlusImage,
  'moli-cockpit-pro': cockpitProImage,
  'moli-cockpit-ultra': cockpitUltraImage,
  'moli-racer-core': racerCoreImage,
  'moli-racer-signature': racerSignatureImage,
  'moli-racer-elite': racerEliteImage,
  'moli-arena-core': arenaCoreImage,
  'moli-arena-signature': arenaSignatureImage,
  'moli-arena-venue': arenaVenueImage,
  'furniture-desk': furnitureDesk,
  'furniture-seat': furnitureChair,
  'furniture-light': furnitureLighting,
}

const experiences = [
  {
    id: 'competitive' as const,
    index: '01',
    title: 'Competitive Gaming',
    caption: 'See sooner. React faster. Hear the direction.',
    products: [
      { label: 'High-refresh display', benefit: 'Smoother motion. Faster reads.', gear: 'displays' as const, focus: ['50%', '36%'] },
      { label: 'Precision controls', benefit: 'Quicker inputs. More consistent control.', gear: 'controls' as const, focus: ['52%', '49%'] },
      { label: 'Team audio', benefit: 'Clearer direction. Cleaner team communication.', gear: 'audio' as const, focus: ['30%', '44%'] },
    ],
    image: competitiveExperience,
  },
  {
    id: 'immersive' as const,
    index: '02',
    title: 'Immersive Gaming',
    caption: 'More world in view. More atmosphere around you.',
    products: [
      { label: 'Ultrawide or 4K', benefit: 'More detail. More world in view.', gear: 'displays' as const, focus: ['50%', '37%'] },
      { label: 'Immersive audio', benefit: 'Wider sound. Stronger atmosphere.', gear: 'audio' as const, focus: ['65%', '43%'] },
      { label: 'Comfort & lighting', benefit: 'Longer comfort. Less visual strain.', gear: 'furniture' as const, focus: ['44%', '61%'] },
    ],
    image: immersiveExperience,
  },
  {
    id: 'racing' as const,
    index: '03',
    title: 'Sim Racing',
    caption: 'Feel the road through every input and response.',
    products: [
      { label: 'Racing display', benefit: 'A clearer view of every braking point.', gear: 'displays' as const, focus: ['50%', '40%'] },
      { label: 'Focused audio', benefit: 'Hear the engine, tyres and competitors around you.', gear: 'audio' as const, focus: ['64%', '42%'] },
      { label: 'Seat & lighting', benefit: 'Stay comfortable and focused through longer races.', gear: 'furniture' as const, focus: ['38%', '70%'] },
    ],
    image: simulationExperience,
  },
  {
    id: 'streaming' as const,
    index: '04',
    title: 'Streaming & Creation',
    caption: 'Play, capture and create from one considered space.',
    products: [
      { label: 'Microphone', benefit: 'Cleaner voice. Less room noise.', gear: 'audio' as const, focus: ['38%', '42%'] },
      { label: 'Camera & capture', benefit: 'Sharper capture. A more polished stream.', gear: 'controls' as const, focus: ['51%', '29%'] },
      { label: 'Lighting & control', benefit: 'Balanced light. Faster scene control.', gear: 'furniture' as const, focus: ['69%', '30%'] },
    ],
    image: streamingExperience,
  },
]

const demoPaymentEnabled = import.meta.env.DEV || import.meta.env.VITE_DEMO_PAYMENT_ENABLED === 'true'
const initialAccountParams = new URL(window.location.href).searchParams
const initialVerificationToken = initialAccountParams.get('verifyEmail')
const initialPasswordResetToken = initialAccountParams.get('resetPassword')

function readStartingPoint(value: string | null): StartingPoint | null {
  return startingPoints.some((point) => point.id === value) ? value as StartingPoint : null
}

function readExperience(value: string | null): Experience | null {
  return experiences.some((item) => item.id === value) ? value as Experience : null
}

function readGearTarget(value: string | null): GearTarget | null {
  return gearShortcuts.some((gear) => gear.id === value) ? value as GearTarget : null
}

function restoreWindowScroll(top: number) {
  const root = document.documentElement
  const previousScrollBehavior = root.style.scrollBehavior
  root.style.scrollBehavior = 'auto'
  window.scrollTo({ top, behavior: 'auto' })
  root.style.scrollBehavior = previousScrollBehavior
}

const homeSectionIds = [
  'top',
  'moli-cockpit',
  'moli-racer',
  'moli-arena',
  'shop-by-gear',
  'how-it-works',
  'starting-point',
  'experiences',
  'support',
]

function syncHomeHashToScroll(top: number) {
  const nearestSection = homeSectionIds.reduce<{ id: string; distance: number } | null>((nearest, id) => {
    const section = document.getElementById(id)
    if (!section) return nearest
    const distance = Math.abs(section.getBoundingClientRect().top + window.scrollY - top)
    return !nearest || distance < nearest.distance ? { id, distance } : nearest
  }, null)
  if (!nearestSection || window.location.hash === `#${nearestSection.id}`) return
  window.history.replaceState(window.history.state, '', `#${nearestSection.id}`)
}

export function HomePage() {
  const [mode, setMode] = useState<HeroMode>('brand')
  const [activeCockpitModel, setActiveCockpitModel] = useState<CockpitModelId>('cockpit')
  const [isCockpitCompareOpen, setIsCockpitCompareOpen] = useState(false)
  const [activeRacerModel, setActiveRacerModel] = useState<RacerModelId>('core')
  const [isRacerCompareOpen, setIsRacerCompareOpen] = useState(false)
  const [activeArenaModel, setActiveArenaModel] = useState<ArenaModelId>('core')
  const [isArenaCompareOpen, setIsArenaCompareOpen] = useState(false)
  const [startingPoint, setStartingPoint] = useState<StartingPoint | null>(null)
  const [experience, setExperience] = useState<Experience | null>(null)
  const [experienceProduct, setExperienceProduct] = useState<string | null>(null)
  const [gearTarget, setGearTarget] = useState<GearTarget>('displays')
  const [productFocus, setProductFocus] = useState<GearTarget | null>(null)
  const [catalogSubtype, setCatalogSubtype] = useState<string | null>(null)
  const [catalogView, setCatalogView] = useState<CatalogView | null>(null)
  const [isCategoryMenuOpen, setIsCategoryMenuOpen] = useState(false)
  const [isGuidedPathVisible, setIsGuidedPathVisible] = useState(false)
  const [hasGuidedInteraction, setHasGuidedInteraction] = useState(false)
  const [isHeroMotionAllowed, setIsHeroMotionAllowed] = useState(false)
  const [isHeroVisible, setIsHeroVisible] = useState(true)
  const [isDocumentVisible, setIsDocumentVisible] = useState(true)
  const [visibleShowcase, setVisibleShowcase] = useState<'cockpit' | 'racer' | 'arena' | null>(null)
  const [pausedShowcase, setPausedShowcase] = useState<'cockpit' | 'racer' | 'arena' | null>(null)
  const [heroFrame, setHeroFrame] = useState<0 | 1>(0)
  const [previousHero, setPreviousHero] = useState<{ mode: HeroMode; frame: 0 | 1 } | null>(null)
  const [isAccountOpen, setIsAccountOpen] = useState(Boolean(initialVerificationToken || initialPasswordResetToken))
  const [authMode, setAuthMode] = useState<AuthMode>(initialPasswordResetToken ? 'reset' : 'login')
  const [authToken, setAuthToken] = useState<string | null>(loadToken)
  const [authUser, setAuthUser] = useState<AuthUser | null>(null)
  const [authError, setAuthError] = useState('')
  const [authNotice, setAuthNotice] = useState('')
  const [passwordResetToken, setPasswordResetToken] = useState(initialPasswordResetToken ?? '')
  const [isAuthSubmitting, setIsAuthSubmitting] = useState(Boolean(initialVerificationToken))
  const [catalogItems, setCatalogItems] = useState<CatalogItem[]>([])
  const [catalogError, setCatalogError] = useState('')
  const [isCatalogLoading, setIsCatalogLoading] = useState(true)
  const [cartLines, setCartLines] = useState<CartLine[]>(() => authToken ? loadCart() : [])
  const [selectedCartVariantIds, setSelectedCartVariantIds] = useState<string[]>(() => authToken ? loadCart().map((line) => line.variantId) : [])
  const [isCartOpen, setIsCartOpen] = useState(false)
  const [pendingCartItem, setPendingCartItem] = useState<CatalogItem | null>(null)
  const [checkoutDraft, setCheckoutDraft] = useState<CheckoutDraft | null>(null)
  const [pendingCheckout, setPendingCheckout] = useState<CheckoutDraft | null>(null)
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false)
  const [checkoutStep, setCheckoutStep] = useState<CheckoutStep>('delivery')
  const [checkoutOrder, setCheckoutOrder] = useState<Order | null>(null)
  const [checkoutPayment, setCheckoutPayment] = useState<Payment | null>(null)
  const [checkoutInvoice, setCheckoutInvoice] = useState<Invoice | null>(null)
  const [isInvoiceOpen, setIsInvoiceOpen] = useState(false)
  const [isOrdersOpen, setIsOrdersOpen] = useState(false)
  const [orders, setOrders] = useState<Order[]>([])
  const [ordersError, setOrdersError] = useState('')
  const [isOrdersLoading, setIsOrdersLoading] = useState(false)
  const [loadingInvoiceReference, setLoadingInvoiceReference] = useState<string | null>(null)
  const [checkoutError, setCheckoutError] = useState('')
  const [isCheckoutSubmitting, setIsCheckoutSubmitting] = useState(false)
  const [checkoutIdempotencyKey, setCheckoutIdempotencyKey] = useState(() => crypto.randomUUID())
  const [paymentIdempotencyKey, setPaymentIdempotencyKey] = useState(() => crypto.randomUUID())
  const heroRef = useRef<HTMLElement>(null)
  const heroTransitionTimerRef = useRef<number | null>(null)
  const cockpitModelListRef = useRef<HTMLDivElement>(null)
  const racerModelListRef = useRef<HTMLDivElement>(null)
  const arenaModelListRef = useRef<HTMLDivElement>(null)
  const startingPointRef = useRef<HTMLElement>(null)
  const experiencesRef = useRef<HTMLElement>(null)
  const checkoutRef = useRef<HTMLElement>(null)
  const checkoutSubmittingRouteRef = useRef<StoreRoute | null>(null)
  const pendingCheckoutCompletionRef = useRef(false)
  const managedViewReturnScrollRef = useRef<number | null>(null)
  const showcaseResumeTimerRef = useRef<number | null>(null)
  const selectedStart = startingPoints.find((point) => point.id === startingPoint)
  const displayedStart = selectedStart ?? startingPoints[0]
  const activeExperience = experience ?? 'competitive'
  const selectedExperience = experiences.find((item) => item.id === activeExperience)!
  const selectedExperienceProduct = selectedExperience.products.find((product) => product.label === experienceProduct)
    ?? selectedExperience.products[0]
  const selectedGear = gearShortcuts.find((gear) => gear.id === gearTarget)!
  const selectedCockpitModel = cockpitModels.find((model) => model.id === activeCockpitModel)!
  const selectedRacerModel = racerModels.find((model) => model.id === activeRacerModel)!
  const selectedArenaModel = arenaModels.find((model) => model.id === activeArenaModel)!
  const showSetupTray = hasGuidedInteraction && isGuidedPathVisible
  const isBlockingViewOpen = Boolean(catalogView)
    || isCockpitCompareOpen
    || isRacerCompareOpen
    || isArenaCompareOpen
    || isCartOpen
    || (isAccountOpen && !authUser)
    || isOrdersOpen
    || isCheckoutOpen
    || isInvoiceOpen
  const allGearIds = gearShortcuts.map((gear) => gear.id)
  const allProductIds: GearTarget[] = ['sim', ...allGearIds]
  const deviceProfile = startingPoint ? deviceRecommendationProfiles[startingPoint] : null
  const experienceProfile = experience ? experienceRecommendationProfiles[experience] : null
  const recommendationGearIds = experienceProfile
    ? uniqueGearTargets(deviceProfile?.alwaysInclude ?? [], experienceProfile.gears)
    : deviceProfile?.defaultGears ?? allGearIds
  const recommendationTitle = selectedStart && experience
    ? `${selectedStart.label} + ${selectedExperience.title}`
    : selectedStart?.label ?? (experience ? selectedExperience.title : 'All gaming gear')
  const visibleCatalogItems = catalogView
    ? catalogItems.filter((item) => (
      catalogView.allowedCategories.includes(item.category)
      && (!productFocus || item.category === productFocus)
      && (!(catalogView.lockedSubtype ?? catalogSubtype) || item.subtype === (catalogView.lockedSubtype ?? catalogSubtype))
      && (catalogView.source !== 'recommendation'
        || productMatchesRecommendation(item, startingPoint, experience, recommendationGearIds))
    )).sort((left, right) => catalogView.source === 'recommendation'
      ? recommendationGearIds.indexOf(left.category) - recommendationGearIds.indexOf(right.category)
      : 0)
    : []
  const catalogSubtypes = productFocus
    ? catalogView?.lockedSubtype
      ? []
      : catalogView?.source === 'recommendation'
      ? [...new Set(catalogItems
        .filter((item) => item.category === productFocus
          && productMatchesRecommendation(item, startingPoint, experience, recommendationGearIds))
        .map((item) => item.subtype))]
      : [...new Set(catalogItems
        .filter((item) => item.category === productFocus)
        .map((item) => item.subtype))]
    : []
  const cartItems = cartLines.flatMap((line) => {
    const product = catalogItems.find((item) => item.variantId === line.variantId)
    return product ? [{ ...line, product }] : []
  })
  const cartCount = cartLines.reduce((total, line) => total + line.quantity, 0)
  const selectedCartItems = cartItems.filter((line) => selectedCartVariantIds.includes(line.variantId))
  const selectedCartSubtotalCents = selectedCartItems.reduce((total, line) => total + line.product.priceCents * line.quantity, 0)

  useEffect(() => {
    const applyBrowserRoute = (navigationSource: 'initial' | 'history') => {
      const route = readStoreRoute()
      const restoreManagedViewReturnScroll = () => {
        const returnScrollTop = managedViewReturnScrollRef.current
        if (returnScrollTop === null) return false
        managedViewReturnScrollRef.current = null
        window.requestAnimationFrame(() => {
          window.requestAnimationFrame(() => {
            restoreWindowScroll(returnScrollTop)
            if (route === 'home') syncHomeHashToScroll(returnScrollTop)
          })
        })
        return true
      }
      setIsCockpitCompareOpen(route === 'cockpit-compare')
      setIsRacerCompareOpen(route === 'racer-compare')
      setIsArenaCompareOpen(route === 'arena-compare')

      if (route !== 'account') {
        setPendingCartItem(null)
        setPendingCheckout(null)
      }

      if (checkoutSubmittingRouteRef.current && route !== checkoutSubmittingRouteRef.current) {
        window.history.forward()
        return
      }

      if (pendingCheckoutCompletionRef.current) {
        pendingCheckoutCompletionRef.current = false
        pushStoreRoute<CheckoutNavigationData>('checkout-complete', { depth: 1 })
        setCheckoutStep('complete')
        setIsCheckoutOpen(true)
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        return
      }

      if (route === 'recommendation') {
        const navigation = readRecommendationNavigation()
        const nextStartingPoint = readStartingPoint(navigation.startingPoint)
        const nextExperience = readExperience(navigation.experience)
        const nextCategory = readGearTarget(navigation.category)
        setStartingPoint(nextStartingPoint)
        setExperience(nextExperience)
        setExperienceProduct(nextExperience
          ? experiences.find((item) => item.id === nextExperience)?.products[0].label ?? null
          : null)
        setProductFocus(nextCategory)
        setCatalogView(null)
        setIsCategoryMenuOpen(false)
        setHasGuidedInteraction(true)
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsCheckoutOpen(false)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        if (!restoreManagedViewReturnScroll() && navigationSource === 'history') {
          window.requestAnimationFrame(() => {
            const sectionId = nextExperience ? 'experiences' : nextStartingPoint ? 'starting-point' : 'how-it-works'
            document.getElementById(sectionId)?.scrollIntoView({ behavior: 'auto', block: 'start' })
          })
        } else if (navigationSource === 'initial') {
          window.requestAnimationFrame(() => {
            window.requestAnimationFrame(() => restoreWindowScroll(0))
          })
        }
        return
      }

      if (route === 'shop') {
        const navigation = readStoreNavigation<CatalogNavigationData>()
        const fallbackView: CatalogView = {
          title: 'All gaming gear',
          allowedCategories: gearShortcuts.map((gear) => gear.id),
          source: 'direct',
        }
        setCatalogView(navigation?.data?.catalogView ?? fallbackView)
        setProductFocus(navigation?.data?.category ?? null)
        setCatalogSubtype(navigation?.data?.subtype ?? null)
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsCheckoutOpen(false)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        return
      }

      if (route === 'cockpit-compare') {
        setCatalogView(null)
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsCheckoutOpen(false)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        return
      }

      if (route === 'racer-compare') {
        setCatalogView(null)
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsCheckoutOpen(false)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        return
      }

      if (route === 'arena-compare') {
        setCatalogView(null)
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsCheckoutOpen(false)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        return
      }

      if (route === 'cart') {
        setIsCartOpen(true)
        setIsAccountOpen(false)
        setIsCheckoutOpen(false)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        return
      }

      if (route === 'account') {
        setIsCartOpen(false)
        setIsAccountOpen(true)
        setIsCheckoutOpen(false)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        return
      }

      if (route === 'orders') {
        if (!loadToken()) {
          replaceStoreRoute('account')
          setIsAccountOpen(true)
          setIsOrdersOpen(false)
          return
        }
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsCheckoutOpen(false)
        setIsOrdersOpen(true)
        setIsInvoiceOpen(false)
        return
      }

      if (route.startsWith('checkout-')) {
        setCheckoutStep(route.replace('checkout-', '') as CheckoutStep)
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsCheckoutOpen(true)
        setIsOrdersOpen(false)
        setIsInvoiceOpen(false)
        return
      }

      if (route === 'invoice') {
        setIsCartOpen(false)
        setIsAccountOpen(false)
        setIsInvoiceOpen(true)
        return
      }

      setCatalogView(null)
      setStartingPoint(null)
      setExperience(null)
      setExperienceProduct(null)
      setProductFocus(null)
      setIsCategoryMenuOpen(false)
      setHasGuidedInteraction(false)
      setIsCartOpen(false)
      setIsAccountOpen(Boolean(initialVerificationToken || initialPasswordResetToken))
      setIsCheckoutOpen(false)
      setIsOrdersOpen(false)
      setIsInvoiceOpen(false)
      const navigationEntry = window.performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined
      if (navigationSource === 'initial' && navigationEntry?.type === 'reload') {
        replaceWithStoreHome()
        window.requestAnimationFrame(() => {
          window.requestAnimationFrame(() => restoreWindowScroll(0))
        })
        return
      }
      if (!restoreManagedViewReturnScroll()) {
        window.requestAnimationFrame(() => {
          const hash = window.location.hash
          const sectionId = hash && !hash.startsWith('#/') ? hash.slice(1) : 'top'
          document.getElementById(sectionId || 'top')?.scrollIntoView({ behavior: 'auto', block: 'start' })
        })
      }
    }

    const handleHistoryNavigation = () => applyBrowserRoute('history')
    applyBrowserRoute('initial')
    window.addEventListener('popstate', handleHistoryNavigation)
    return () => window.removeEventListener('popstate', handleHistoryNavigation)
  }, [])

  useEffect(() => {
    if (!isBlockingViewOpen) return
    const lockedScrollTop = window.scrollY
    const previousBodyStyles = {
      overflow: document.body.style.overflow,
      position: document.body.style.position,
      top: document.body.style.top,
      width: document.body.style.width,
    }
    document.body.style.overflow = 'hidden'
    document.body.style.position = 'fixed'
    document.body.style.top = `-${lockedScrollTop}px`
    document.body.style.width = '100%'

    return () => {
      document.body.style.overflow = previousBodyStyles.overflow
      document.body.style.position = previousBodyStyles.position
      document.body.style.top = previousBodyStyles.top
      document.body.style.width = previousBodyStyles.width
      window.requestAnimationFrame(() => {
        window.requestAnimationFrame(() => restoreWindowScroll(lockedScrollTop))
      })
    }
  }, [isBlockingViewOpen])

  useEffect(() => {
    if (!isCheckoutOpen) return
    const route = readStoreRoute()
    const routeNeedsOrder = route === 'checkout-review'
    const routeNeedsPayment = route === 'checkout-complete'
    if (checkoutDraft && (!routeNeedsOrder || checkoutOrder) && (!routeNeedsPayment || checkoutPayment)) return

    replaceWithStoreHome()
    window.dispatchEvent(new PopStateEvent('popstate', { state: window.history.state }))
  }, [checkoutDraft, checkoutOrder, checkoutPayment, isCheckoutOpen])

  useEffect(() => {
    if (!isInvoiceOpen || checkoutInvoice) return
    replaceWithStoreHome()
    window.dispatchEvent(new PopStateEvent('popstate', { state: window.history.state }))
  }, [checkoutInvoice, isInvoiceOpen])

  useEffect(() => {
    getCatalogProducts()
      .then((products) => setCatalogItems(products.map((product) => ({
        ...product,
        image: catalogImages[product.imageKey] ?? displayGear,
      }))))
      .catch((error) => setCatalogError(error instanceof Error ? error.message : 'The gear catalogue is unavailable right now.'))
      .finally(() => setIsCatalogLoading(false))
  }, [])

  useEffect(() => {
    saveCart(cartLines)
  }, [cartLines])

  useEffect(() => {
    checkoutRef.current?.scrollTo({ top: 0 })
  }, [checkoutStep])

  useEffect(() => {
    if (!authToken) return

    getCurrentUser(authToken)
      .then(setAuthUser)
      .catch(() => {
        clearToken()
        setAuthToken(null)
        setAuthUser(null)
        setCartLines([])
        setSelectedCartVariantIds([])
      })
  }, [authToken])

  useEffect(() => {
    if (!isOrdersOpen || !authToken) return
    let active = true
    window.queueMicrotask(() => {
      if (!active) return
      setOrdersError('')
      setIsOrdersLoading(true)
      getOrders(authToken)
        .then((nextOrders) => { if (active) setOrders(nextOrders) })
        .catch((error) => { if (active) setOrdersError(error instanceof Error ? error.message : 'Your orders are temporarily unavailable.') })
        .finally(() => { if (active) setIsOrdersLoading(false) })
    })
    return () => { active = false }
  }, [authToken, isOrdersOpen])

  useEffect(() => {
    const url = new URL(window.location.href)
    const verificationToken = url.searchParams.get('verifyEmail')
    const resetToken = url.searchParams.get('resetPassword')
    if (!verificationToken && !resetToken) return

    if (resetToken) return

    verifyEmail(verificationToken!)
      .then(({ message }) => {
        setAuthNotice(message)
        if (authToken) getCurrentUser(authToken).then(setAuthUser).catch(() => undefined)
        url.searchParams.delete('verifyEmail')
        window.history.replaceState({}, '', url)
      })
      .catch((error) => setAuthError(error instanceof Error ? error.message : 'This verification link could not be used.'))
      .finally(() => setIsAuthSubmitting(false))
  }, [authToken])

  useEffect(() => {
    const syncAuthToken = (event: StorageEvent) => {
      if (event.key !== AUTH_TOKEN_STORAGE_KEY) return
      setAuthToken(event.newValue)
      if (!event.newValue) {
        setAuthUser(null)
        setCartLines([])
        setSelectedCartVariantIds([])
      }
    }
    window.addEventListener('storage', syncAuthToken)
    return () => window.removeEventListener('storage', syncAuthToken)
  }, [])

  useEffect(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
    const updateMotionPreference = () => setIsHeroMotionAllowed(!reducedMotion.matches)

    updateMotionPreference()
    reducedMotion.addEventListener('change', updateMotionPreference)
    return () => reducedMotion.removeEventListener('change', updateMotionPreference)
  }, [])

  useEffect(() => () => {
    if (showcaseResumeTimerRef.current !== null) window.clearTimeout(showcaseResumeTimerRef.current)
  }, [])

  useEffect(() => {
    if (!heroRef.current) return
    const observer = new IntersectionObserver(([entry]) => setIsHeroVisible(entry.isIntersecting), { threshold: 0.08 })
    observer.observe(heroRef.current)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    const showcases = [
      ['cockpit', cockpitModelListRef.current],
      ['racer', racerModelListRef.current],
      ['arena', arenaModelListRef.current],
    ] as const
    const visibleRatios = new Map<Element, number>()
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => visibleRatios.set(entry.target, entry.intersectionRatio))
      const mostVisible = showcases
        .filter((entry): entry is readonly ['cockpit' | 'racer' | 'arena', HTMLDivElement] => Boolean(entry[1]))
        .map(([id, element]) => ({ id, ratio: visibleRatios.get(element) ?? 0 }))
        .sort((left, right) => right.ratio - left.ratio)[0]
      setVisibleShowcase(mostVisible && mostVisible.ratio >= 0.55 ? mostVisible.id : null)
    }, { threshold: [0, 0.55, 0.75] })

    showcases.forEach(([, element]) => {
      if (element) observer.observe(element)
    })
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    if (!visibleShowcase || pausedShowcase === visibleShowcase || !isHeroMotionAllowed || !isDocumentVisible) return
    if (window.matchMedia('(max-width: 1000px)').matches) return

    const advanceModel = window.setTimeout(() => {
      if (visibleShowcase === 'cockpit') {
        const currentIndex = cockpitModels.findIndex((model) => model.id === activeCockpitModel)
        setActiveCockpitModel(cockpitModels[(currentIndex + 1) % cockpitModels.length].id)
      } else if (visibleShowcase === 'racer') {
        const currentIndex = racerModels.findIndex((model) => model.id === activeRacerModel)
        setActiveRacerModel(racerModels[(currentIndex + 1) % racerModels.length].id)
      } else {
        const currentIndex = arenaModels.findIndex((model) => model.id === activeArenaModel)
        setActiveArenaModel(arenaModels[(currentIndex + 1) % arenaModels.length].id)
      }
    }, 4000)

    return () => window.clearTimeout(advanceModel)
  }, [activeArenaModel, activeCockpitModel, activeRacerModel, isDocumentVisible, isHeroMotionAllowed, pausedShowcase, visibleShowcase])

  useEffect(() => {
    const updateDocumentVisibility = () => setIsDocumentVisible(document.visibilityState === 'visible')
    updateDocumentVisibility()
    document.addEventListener('visibilitychange', updateDocumentVisibility)
    return () => document.removeEventListener('visibilitychange', updateDocumentVisibility)
  }, [])

  useEffect(() => () => {
    if (heroTransitionTimerRef.current !== null) window.clearTimeout(heroTransitionTimerRef.current)
  }, [])

  const transitionHero = useCallback((nextMode: HeroMode, nextFrame: 0 | 1) => {
    if (nextMode === mode && nextFrame === heroFrame) return
    if (heroTransitionTimerRef.current !== null) window.clearTimeout(heroTransitionTimerRef.current)

    setPreviousHero({ mode, frame: heroFrame })
    setMode(nextMode)
    setHeroFrame(nextFrame)
    heroTransitionTimerRef.current = window.setTimeout(() => {
      setPreviousHero(null)
      heroTransitionTimerRef.current = null
    }, 520)
  }, [heroFrame, mode])

  useEffect(() => {
    const shouldSequence = isHeroMotionAllowed
      && isHeroVisible
      && isDocumentVisible
    if (!shouldSequence) return

    const advanceHero = window.setTimeout(() => {
      if (heroFrame === 0) {
        transitionHero(mode, 1)
        return
      }
      const modes = Object.keys(heroModes) as HeroMode[]
      transitionHero(modes[(modes.indexOf(mode) + 1) % modes.length], 0)
    }, 4000)
    return () => window.clearTimeout(advanceHero)
  }, [mode, heroFrame, isHeroMotionAllowed, isHeroVisible, isDocumentVisible, transitionHero])

  useEffect(() => {
    const sections = [startingPointRef.current, experiencesRef.current].filter((section): section is HTMLElement => Boolean(section))
    if (!sections.length) return

    const visibleSections = new Map<Element, boolean>()
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => visibleSections.set(entry.target, entry.isIntersecting))
      const isVisible = [...visibleSections.values()].some(Boolean)
      setIsGuidedPathVisible(isVisible)
      if (!isVisible) setHasGuidedInteraction(false)
    }, { threshold: 0 })

    sections.forEach((section) => {
      visibleSections.set(section, false)
      observer.observe(section)
    })
    return () => observer.disconnect()
  }, [])

  const scrollToSection = (id: string) => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  const openHomeSection = (id: string) => {
    setHasGuidedInteraction(false)
    if (readStoreRoute() !== 'home') replaceWithStoreHome()
    window.requestAnimationFrame(() => scrollToSection(id))
  }

  const pauseShowcaseAfterSelection = (showcase: 'cockpit' | 'racer' | 'arena') => {
    if (showcaseResumeTimerRef.current !== null) window.clearTimeout(showcaseResumeTimerRef.current)
    setPausedShowcase(showcase)
    showcaseResumeTimerRef.current = window.setTimeout(() => {
      setPausedShowcase((current) => current === showcase ? null : current)
      showcaseResumeTimerRef.current = null
    }, 10_000)
  }

  const selectCockpitModel = (modelId: CockpitModelId) => {
    setActiveCockpitModel(modelId)
    pauseShowcaseAfterSelection('cockpit')
  }

  const selectRacerModel = (modelId: RacerModelId) => {
    setActiveRacerModel(modelId)
    pauseShowcaseAfterSelection('racer')
  }

  const selectArenaModel = (modelId: ArenaModelId) => {
    setActiveArenaModel(modelId)
    pauseShowcaseAfterSelection('arena')
  }

  const selectHeroMode = (nextMode: HeroMode) => {
    if (nextMode === mode) {
      transitionHero(nextMode, 0)
      return
    }

    transitionHero(nextMode, 0)
  }

  const showNextHeroFrame = () => transitionHero(mode, heroFrame === 0 ? 1 : 0)

  const selectStartingPoint = (point: (typeof startingPoints)[number]) => {
    setStartingPoint(point.id)
    setProductFocus(null)
    setIsCategoryMenuOpen(false)
    if (readStoreRoute() !== 'recommendation' || startingPoint !== point.id || productFocus !== null) {
      pushRecommendationRoute({ startingPoint: point.id, experience, category: null })
    }
  }

  const selectExperience = (item: (typeof experiences)[number]) => {
    setExperience(item.id)
    setExperienceProduct(item.products[0].label)
    setProductFocus(null)
    setIsCategoryMenuOpen(false)
    if (readStoreRoute() !== 'recommendation' || experience !== item.id || productFocus !== null) {
      pushRecommendationRoute({ startingPoint, experience: item.id, category: null })
    }
  }

  const selectExperienceProduct = (product: (typeof experiences)[number]['products'][number]) => {
    setExperienceProduct(product.label)
  }

  const openRecommendation = () => {
    setCatalogView(null)
    setIsCartOpen(false)
    setIsAccountOpen(false)
    setIsCheckoutOpen(false)
    setIsOrdersOpen(false)
    setIsInvoiceOpen(false)
    setHasGuidedInteraction(true)
    if (readStoreRoute() !== 'recommendation') {
      pushRecommendationRoute({ startingPoint, experience, category: productFocus })
    }
    scrollToSection(startingPoint ? experience ? 'experiences' : 'starting-point' : 'how-it-works')
  }

  const selectRecommendationCategory = (category: GearTarget | null) => {
    setProductFocus(category)
    setIsCategoryMenuOpen(false)
    if (readStoreRoute() !== 'recommendation' || productFocus !== category) {
      pushRecommendationRoute({ startingPoint, experience, category })
    }
  }

  const leaveManagedView = (fallback: () => void) => {
    const navigation = readStoreNavigation()
    if (readStoreRoute() !== 'home' && navigation?.owned) {
      window.history.back()
      return
    }
    if (readStoreRoute() !== 'home') replaceWithStoreHome()
    fallback()
  }

  const rememberManagedViewReturnScroll = () => {
    if (managedViewReturnScrollRef.current === null) {
      managedViewReturnScrollRef.current = window.scrollY
      if (readStoreRoute() === 'home') syncHomeHashToScroll(window.scrollY)
    }
  }

  const openCatalog = (view: CatalogView, category: GearTarget | null = null, subtype: string | null = null) => {
    rememberManagedViewReturnScroll()
    setIsCockpitCompareOpen(false)
    setIsRacerCompareOpen(false)
    setIsArenaCompareOpen(false)
    setCatalogView(view)
    setProductFocus(category)
    setCatalogSubtype(subtype)
    setIsCategoryMenuOpen(false)
    setIsCartOpen(false)
    setIsAccountOpen(false)
    setIsCheckoutOpen(false)
    setIsOrdersOpen(false)
    setIsInvoiceOpen(false)
    pushStoreRoute<CatalogNavigationData>('shop', { catalogView: view, category, subtype })
  }

  const openCockpitCollection = () => openCatalog({
    title: 'MOLI Cockpit collection',
    allowedCategories: ['sim'],
    source: 'direct',
    lockedSubtype: 'Complete cockpits',
  }, 'sim')

  const openCockpitComparison = () => {
    rememberManagedViewReturnScroll()
    setIsCockpitCompareOpen(true)
    setIsRacerCompareOpen(false)
    setIsArenaCompareOpen(false)
    setCatalogView(null)
    setIsCartOpen(false)
    setIsAccountOpen(false)
    setIsCheckoutOpen(false)
    setIsOrdersOpen(false)
    setIsInvoiceOpen(false)
    pushStoreRoute('cockpit-compare')
  }

  const closeCockpitComparison = () => leaveManagedView(() => setIsCockpitCompareOpen(false))

  const openRacerCollection = () => openCatalog({
    title: 'MOLI Racer collection',
    allowedCategories: ['sim'],
    source: 'direct',
    lockedSubtype: 'Complete racers',
  }, 'sim')

  const openRacerComparison = () => {
    rememberManagedViewReturnScroll()
    setIsRacerCompareOpen(true)
    setIsCockpitCompareOpen(false)
    setIsArenaCompareOpen(false)
    setCatalogView(null)
    setIsCartOpen(false)
    setIsAccountOpen(false)
    setIsCheckoutOpen(false)
    setIsOrdersOpen(false)
    setIsInvoiceOpen(false)
    pushStoreRoute('racer-compare')
  }

  const closeRacerComparison = () => leaveManagedView(() => setIsRacerCompareOpen(false))

  const openArenaCollection = () => openCatalog({
    title: 'JG MOLI Arena collection',
    allowedCategories: ['sim'],
    source: 'direct',
    lockedSubtype: 'Interactive arenas',
  }, 'sim')

  const openArenaComparison = () => {
    rememberManagedViewReturnScroll()
    setIsArenaCompareOpen(true)
    setIsCockpitCompareOpen(false)
    setIsRacerCompareOpen(false)
    setCatalogView(null)
    setIsCartOpen(false)
    setIsAccountOpen(false)
    setIsCheckoutOpen(false)
    setIsOrdersOpen(false)
    setIsInvoiceOpen(false)
    pushStoreRoute('arena-compare')
  }

  const closeArenaComparison = () => leaveManagedView(() => setIsArenaCompareOpen(false))

  const openCart = (navigation: 'push' | 'replace' = 'push') => {
    rememberManagedViewReturnScroll()
    setIsCartOpen(true)
    setIsAccountOpen(false)
    setIsCheckoutOpen(false)
    setIsOrdersOpen(false)
    setIsInvoiceOpen(false)
    if (navigation === 'replace') replaceStoreRoute('cart')
    else pushStoreRoute('cart')
  }

  const showAccount = () => {
    if (!authUser) {
      setPendingCartItem(null)
      setPendingCheckout(null)
    }
    if (authUser) {
      setIsAccountOpen((open) => !open)
      return
    }
    rememberManagedViewReturnScroll()
    setIsAccountOpen(true)
    setIsCartOpen(false)
    setIsCheckoutOpen(false)
    setIsOrdersOpen(false)
    setIsInvoiceOpen(false)
    if (readStoreRoute() !== 'account') pushStoreRoute('account')
  }

  const openAccount = () => {
    setAuthError('')
    setAuthNotice('')
    showAccount()
  }

  const requireAccount = (message: string) => {
    rememberManagedViewReturnScroll()
    setAuthError(message)
    setAuthNotice('')
    setIsAccountOpen(true)
    setIsCartOpen(false)
    setIsCheckoutOpen(false)
    setIsOrdersOpen(false)
    setIsInvoiceOpen(false)
    if (readStoreRoute() !== 'account') pushStoreRoute('account')
  }

  const closeAccount = () => {
    setAuthError('')
    setAuthNotice('')
    setPendingCartItem(null)
    setPendingCheckout(null)
    leaveManagedView(() => setIsAccountOpen(false))
  }

  const submitAuth = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setAuthError('')
    setAuthNotice('')
    setIsAuthSubmitting(true)
    const data = new FormData(event.currentTarget)
    const email = String(data.get('email') ?? '')
    const password = String(data.get('password') ?? '')

    try {
      if (authMode === 'forgot') {
        const response = await requestPasswordReset(email)
        setAuthNotice(response.message)
        return
      }
      if (authMode === 'reset') {
        const response = await confirmPasswordReset(passwordResetToken, password)
        setAuthNotice(response.message)
        setPasswordResetToken('')
        setAuthMode('login')
        const url = new URL(window.location.href)
        url.searchParams.delete('resetPassword')
        window.history.replaceState({}, '', url)
        return
      }
      const session = authMode === 'register'
        ? await register(email, password, String(data.get('displayName') ?? ''))
        : await login(email, password)
      saveToken(session.accessToken)
      setAuthToken(session.accessToken)
      setAuthUser(session.user)
      if (pendingCartItem) {
        commitAddToCart(pendingCartItem, 'replace')
        setPendingCartItem(null)
      } else if (pendingCheckout) {
        openCheckout(pendingCheckout, 'replace')
        setPendingCheckout(null)
      } else {
        leaveManagedView(() => setIsAccountOpen(false))
      }
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : 'Something went wrong. Try again.')
    } finally {
      setIsAuthSubmitting(false)
    }
  }

  const logOut = () => {
    clearToken()
    setAuthToken(null)
    setAuthUser(null)
    setAuthMode('login')
    setIsAccountOpen(false)
    setCartLines([])
    setSelectedCartVariantIds([])
    setIsOrdersOpen(false)
    setOrders([])
    if (readStoreRoute() === 'orders') replaceWithStoreHome()
  }

  const resendAccountVerification = async () => {
    if (!authToken) return
    setAuthError('')
    setAuthNotice('')
    try {
      const response = await resendVerification(authToken)
      setAuthNotice(response.message)
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : 'A verification email could not be sent.')
    }
  }

  const openOrders = () => {
    if (!authToken) return
    rememberManagedViewReturnScroll()
    setIsAccountOpen(false)
    setIsOrdersOpen(true)
    setOrdersError('')
    setIsCartOpen(false)
    setIsCheckoutOpen(false)
    setIsInvoiceOpen(false)
    pushStoreRoute('orders')
  }

  const openOrderInvoice = async (orderReference: string) => {
    if (!authToken) return
    setOrdersError('')
    setLoadingInvoiceReference(orderReference)
    try {
      setCheckoutInvoice(await getInvoice(authToken, orderReference))
      setIsInvoiceOpen(true)
      pushStoreRoute('invoice')
    } catch (error) {
      setOrdersError(error instanceof Error ? error.message : 'That invoice is temporarily unavailable.')
    } finally {
      setLoadingInvoiceReference(null)
    }
  }

  const commitAddToCart = (item: CatalogItem, navigation: 'push' | 'replace' = 'push') => {
    setCartLines((current) => {
      const existing = current.find((line) => line.variantId === item.variantId)
      return existing
        ? current.map((line) => line.variantId === item.variantId
          ? { ...line, quantity: Math.min(line.quantity + 1, 99) }
          : line)
        : [...current, { variantId: item.variantId, quantity: 1 }]
    })
    setSelectedCartVariantIds((current) => current.includes(item.variantId) ? current : [...current, item.variantId])
    openCart(navigation)
  }

  const addToCart = (item: CatalogItem) => {
    if (!authUser || !authToken) {
      setPendingCheckout(null)
      setPendingCartItem(item)
      setAuthMode('login')
      requireAccount('Log in or create an account to save gear to your cart.')
      return
    }
    commitAddToCart(item)
  }

  const changeCartQuantity = (variantId: string, change: number) => {
    const currentLine = cartLines.find((line) => line.variantId === variantId)
    if (currentLine && currentLine.quantity + change <= 0) {
      setSelectedCartVariantIds((selected) => selected.filter((id) => id !== variantId))
    }
    setCartLines((current) => current.flatMap((line) => {
      if (line.variantId !== variantId) return [line]
      const quantity = Math.min(line.quantity + change, 99)
      return quantity > 0 ? [{ ...line, quantity }] : []
    }))
  }

  const removeFromCart = (variantId: string) => {
    setCartLines((current) => current.filter((line) => line.variantId !== variantId))
    setSelectedCartVariantIds((current) => current.filter((id) => id !== variantId))
  }

  const toggleCartSelection = (variantId: string) => {
    setSelectedCartVariantIds((current) => current.includes(variantId)
      ? current.filter((id) => id !== variantId)
      : [...current, variantId])
  }

  const toggleAllCartItems = () => {
    setSelectedCartVariantIds(selectedCartVariantIds.length === cartLines.length ? [] : cartLines.map((line) => line.variantId))
  }

  const openCheckout = (draft: CheckoutDraft, navigation: 'push' | 'replace' = 'push') => {
    rememberManagedViewReturnScroll()
    setCheckoutDraft(draft)
    setIsCheckoutOpen(true)
    setCheckoutStep('delivery')
    setCheckoutOrder(null)
    setCheckoutPayment(null)
    setCheckoutInvoice(null)
    setIsInvoiceOpen(false)
    setCheckoutError('')
    setCheckoutIdempotencyKey(crypto.randomUUID())
    setPaymentIdempotencyKey(crypto.randomUUID())
    setIsCartOpen(false)
    setIsAccountOpen(false)
    setIsOrdersOpen(false)
    if (navigation === 'replace') replaceStoreRoute<CheckoutNavigationData>('checkout-delivery', { depth: 1 })
    else pushStoreRoute<CheckoutNavigationData>('checkout-delivery', { depth: 1 })
  }

  const startCheckout = (draft: CheckoutDraft) => {
    if (!draft.lines.length) return
    if (!authUser || !authToken) {
      setPendingCartItem(null)
      setPendingCheckout(draft)
      setAuthMode('login')
      setIsCartOpen(false)
      requireAccount('Log in or create an account to continue to checkout.')
      return
    }
    openCheckout(draft)
  }

  const submitDelivery = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!checkoutDraft) return
    if (!authToken) {
      setCheckoutError('Log in again to continue.')
      return
    }
    const data = new FormData(event.currentTarget)
    setCheckoutError('')
    setIsCheckoutSubmitting(true)
    checkoutSubmittingRouteRef.current = 'checkout-delivery'
    try {
      const order = await createOrder(authToken, checkoutIdempotencyKey, checkoutDraft.source, checkoutDraft.lines, {
        recipientName: String(data.get('recipientName') ?? ''),
        phone: String(data.get('phone') ?? ''),
        addressLine1: String(data.get('addressLine1') ?? ''),
        addressLine2: String(data.get('addressLine2') ?? ''),
        suburb: String(data.get('suburb') ?? ''),
        state: String(data.get('state') ?? ''),
        postcode: String(data.get('postcode') ?? ''),
        countryCode: 'AU',
      })
      setCheckoutOrder(order)
      setCheckoutStep('review')
      checkoutSubmittingRouteRef.current = null
      pushStoreRoute<CheckoutNavigationData>('checkout-review', { depth: 2 })
    } catch (error) {
      setCheckoutError(error instanceof Error ? error.message : 'Checkout is unavailable right now. Try again.')
    } finally {
      checkoutSubmittingRouteRef.current = null
      setIsCheckoutSubmitting(false)
    }
  }

  const payForOrder = async () => {
    if (!checkoutOrder) return
    if (!authToken) {
      setCheckoutError('Log in again to continue.')
      return
    }
    setCheckoutError('')
    setIsCheckoutSubmitting(true)
    checkoutSubmittingRouteRef.current = 'checkout-review'
    try {
      const payment = await completeDemoPayment(authToken, paymentIdempotencyKey, checkoutOrder.orderReference)
      setCheckoutPayment(payment)
      setCheckoutStep('complete')
      checkoutSubmittingRouteRef.current = null
      const checkoutNavigation = readStoreNavigation<CheckoutNavigationData>()
      const checkoutDepth = checkoutNavigation?.data?.depth ?? 1
      if (checkoutNavigation?.owned && checkoutDepth > 0) {
        pendingCheckoutCompletionRef.current = true
        window.history.go(-checkoutDepth)
      } else {
        replaceStoreRoute<CheckoutNavigationData>('checkout-complete', { depth: 1 })
      }
      try {
        setCheckoutInvoice(await getInvoice(authToken, checkoutOrder.orderReference))
      } catch (error) {
        setCheckoutError(error instanceof Error ? error.message : 'Your payment is confirmed, but the invoice is temporarily unavailable.')
      }
      if (checkoutDraft?.source === 'CART') {
        const paidVariantIds = new Set(checkoutDraft.lines.map((line) => line.variantId))
        setCartLines((current) => current.filter((line) => !paidVariantIds.has(line.variantId)))
        setSelectedCartVariantIds((current) => current.filter((id) => !paidVariantIds.has(id)))
      }
    } catch (error) {
      setCheckoutError(error instanceof Error ? error.message : 'Payment could not be completed. Try again.')
    } finally {
      checkoutSubmittingRouteRef.current = null
      setIsCheckoutSubmitting(false)
    }
  }

  const closeCheckout = () => {
    if (isCheckoutSubmitting) return
    setCheckoutError('')
    const checkoutNavigation = readStoreNavigation<CheckoutNavigationData>()
    const depth = checkoutNavigation?.data?.depth ?? 1
    if (readStoreRoute().startsWith('checkout-') && checkoutNavigation?.owned) {
      window.history.go(-depth)
      return
    }
    if (readStoreRoute() !== 'home') replaceWithStoreHome()
    setIsCheckoutOpen(false)
    setIsInvoiceOpen(false)
  }

  const returnToDelivery = () => {
    if (isCheckoutSubmitting) return
    const navigation = readStoreNavigation<CheckoutNavigationData>()
    if (readStoreRoute() === 'checkout-review' && navigation?.owned && (navigation.data?.depth ?? 1) > 1) {
      window.history.back()
      return
    }
    setCheckoutStep('delivery')
    replaceStoreRoute<CheckoutNavigationData>('checkout-delivery', { depth: 1 })
  }

  const openInvoice = () => {
    if (!checkoutInvoice) return
    rememberManagedViewReturnScroll()
    setIsInvoiceOpen(true)
    pushStoreRoute('invoice')
  }

  const closeInvoice = () => leaveManagedView(() => setIsInvoiceOpen(false))

  const checkoutItems = checkoutDraft?.lines.flatMap((line) => {
    const product = catalogItems.find((item) => item.variantId === line.variantId)
    return product ? [{ ...line, product }] : []
  }) ?? []

  const formatMoney = (cents: number, currency = 'AUD') => new Intl.NumberFormat('en-AU', {
    style: 'currency', currency,
  }).format(cents / 100)

  const formatWholeMoney = (cents: number) => new Intl.NumberFormat('en-AU', {
    style: 'currency', currency: 'AUD', maximumFractionDigits: 0,
  }).format(cents / 100)

  const formatPrice = (item: CatalogItem) => new Intl.NumberFormat('en-AU', {
    style: 'currency',
    currency: item.currency,
  }).format(item.priceCents / 100)

  return (
    <main className="home-page">
      <header className="site-header">
        <a className="brand" href="#top" aria-label="JG MOLI home">
          <span className="brand-monogram">JG</span>
          <span>MOLI</span>
        </a>

        <nav className="main-nav" aria-label="Primary navigation">
          <div className="nav-sections">
            <a href="#moli-cockpit">Cockpit</a>
            <a href="#moli-racer">Racer</a>
            <a href="#moli-arena">Arena</a>
            <a className="nav-gear-link" href="#shop-by-gear">Gear</a>
          </div>
          <button className="nav-shop" type="button" onClick={() => openCatalog({ title: 'All products', allowedCategories: allProductIds, source: 'direct' })} aria-label="Shop all products">
            Shop all <span aria-hidden="true">→</span>
          </button>
          <button className="nav-cart" type="button" onClick={() => openCart()} aria-label={`Open cart with ${cartCount} items`}>
            <span className="nav-cart-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" focusable="false">
                <path d="M3 4h2.2l1.55 8.05a2 2 0 0 0 1.96 1.62h7.72a2 2 0 0 0 1.94-1.5L19.7 7H6" />
                <circle cx="9" cy="18.5" r="1.25" />
                <circle cx="17" cy="18.5" r="1.25" />
              </svg>
            </span>
            {cartCount > 0 && <strong>{cartCount}</strong>}
          </button>
          <button className="nav-account" type="button" onClick={openAccount} aria-label={authUser ? `Account for ${authUser.displayName}` : 'Log in or create an account'}>
            <span className="nav-account-icon" aria-hidden="true">{authUser ? authUser.displayName.charAt(0).toUpperCase() : '○'}</span>
          </button>
        </nav>

        {isAccountOpen && authUser && (
          <aside className="account-popover" aria-label="Your account">
            <p>{authUser.displayName}</p>
            <span>{authUser.email}</span>
            {!authUser.emailVerified && <button type="button" onClick={() => void resendAccountVerification()}>Verify email →</button>}
            {authNotice && <small className="account-popover-notice">{authNotice}</small>}
            {authError && <small className="account-popover-error">{authError}</small>}
            {authUser.role === 'ADMIN' && <a className="account-admin-link" href="#/admin">Admin workspace →</a>}
            <button type="button" onClick={openOrders}>My orders →</button>
            <button type="button" onClick={logOut}>Log out</button>
          </aside>
        )}
      </header>

      <MoliAssistant raised={showSetupTray} />

      <section
        ref={heroRef}
        className="hero page-shell"
        id="top"
      >
        <div className="hero-heading">
          <p className="hero-kicker">JG MOLI</p>
          <h1>
            <span>Leave the noise.</span>
            <em>Enter your world.</em>
          </h1>
          <div className="hero-summary">
            <p aria-live="polite">{heroModes[mode].summary}</p>
            <div className="hero-actions">
              <a className="hero-primary-action" href={heroModes[mode].href}>{heroModes[mode].action} <span aria-hidden="true">↓</span></a>
              {mode === 'setups' && (
                <a className="hero-setup-action" href="#/recommendation" onClick={(event) => { event.preventDefault(); openRecommendation() }}>
                  Not sure where to start? Find your setup <span aria-hidden="true">→</span>
                </a>
              )}
            </div>
          </div>
        </div>

        <div className="hero-media">
          <div
            className="hero-visual"
            role="button"
            tabIndex={0}
            aria-label={`Switch ${heroModes[mode].label} scene`}
            onClick={showNextHeroFrame}
            onKeyDown={(event) => {
              if (event.key !== 'Enter' && event.key !== ' ') return
              event.preventDefault()
              showNextHeroFrame()
            }}
          >
            {Object.entries(heroModes).flatMap(([key, hero]) => hero.images.map((image, frame) => (
              <img
                key={`${key}-${frame}`}
                className={mode === key && heroFrame === frame
                  ? 'hero-image active'
                  : previousHero?.mode === key && previousHero.frame === frame
                    ? 'hero-image previous'
                    : 'hero-image'}
                src={image}
                alt=""
                style={{ objectPosition: hero.imagePosition }}
              />
            )))}
            <div className="image-wash" />
            <p className="scene-note" aria-live="polite">{heroModes[mode].note}</p>
          </div>

          <div className="scene-tabs" role="tablist" aria-label="JG MOLI product worlds">
            {(Object.keys(heroModes) as HeroMode[]).map((key, index) => (
              <button
                key={key}
                type="button"
                role="tab"
                aria-selected={mode === key}
                className={mode === key ? 'active' : ''}
                onClick={() => selectHeroMode(key)}
              >
                <span className="scene-tab-index">0{index + 1}</span>
                <span className="scene-tab-copy">
                  <strong>{heroModes[key].label}</strong>
                  <small>{heroModes[key].descriptor}</small>
                </span>
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="cockpit-showcase" id="moli-cockpit" aria-labelledby="cockpit-showcase-title">
        <header className="cockpit-intro page-shell">
          <p className="eyebrow">MOLI Cockpit / Four levels of immersion</p>
          <h2 id="cockpit-showcase-title">Not a chair.<br />Your cockpit.</h2>
          <p>Four levels of personal space, shaped around comfort, focus and the way you play.</p>
        </header>

        <div
          className="cockpit-story page-shell"
          onFocusCapture={() => setPausedShowcase('cockpit')}
          onBlurCapture={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) setPausedShowcase(null)
          }}
        >
          <div ref={cockpitModelListRef} className="cockpit-model-list" aria-live="polite">
            {cockpitModels.map((model) => (
                <article
                  key={model.id}
                  className={activeCockpitModel === model.id ? 'cockpit-model active' : 'cockpit-model'}
                >
                  <img className="cockpit-model-mobile-image" src={model.image} alt={model.imageAlt} />
                  <div className="cockpit-model-meta">
                    <span>{model.index}</span>
                    <small>{model.tier}</small>
                  </div>
                  <h3>{model.name}</h3>
                  <h4>{model.headline}</h4>
                  <div className="cockpit-commerce">
                    <p>
                      <strong>{formatWholeMoney(model.priceCents)}</strong>
                      <small>Incl. GST</small>
                    </p>
                    <button type="button" onClick={openCockpitCollection}>
                      Explore &amp; buy <span aria-hidden="true">→</span>
                    </button>
                  </div>
                </article>
            ))}
          </div>

          <div className="cockpit-media-column">
            <div className="cockpit-media-sticky">
              <div className="cockpit-image-stage" aria-live="polite">
                {cockpitModels.map((model) => (
                  <img
                    key={model.id}
                    className={activeCockpitModel === model.id ? 'active' : ''}
                    src={model.image}
                    alt={activeCockpitModel === model.id ? model.imageAlt : ''}
                  />
                ))}
                <div className="cockpit-image-caption">
                  <span>{selectedCockpitModel.index} / 04</span>
                  <strong>{selectedCockpitModel.name}</strong>
                </div>
              </div>

              <nav className="cockpit-ladder" aria-label="MOLI Cockpit models">
                {cockpitModels.map((model) => (
                  <button
                    key={model.id}
                    type="button"
                    className={activeCockpitModel === model.id ? 'active' : ''}
                    aria-current={activeCockpitModel === model.id ? 'true' : undefined}
                    onClick={() => selectCockpitModel(model.id)}
                  >
                    <span>{model.index}</span>
                    <strong>{model.id === 'cockpit' ? 'Cockpit' : model.name.replace('MOLI Cockpit ', '')}</strong>
                  </button>
                ))}
              </nav>
              <button className="cockpit-compare-trigger" type="button" onClick={openCockpitComparison}>
                Compare all models <span aria-hidden="true">→</span>
              </button>
            </div>
          </div>
        </div>

        <button className="cockpit-compare-trigger cockpit-compare-mobile page-shell" type="button" onClick={openCockpitComparison}>
          Compare all models <span aria-hidden="true">→</span>
        </button>

        <p className="cockpit-trust page-shell">
          <span aria-hidden="true">●</span> Configured for you · Installed at home · Supported in Australia
        </p>
      </section>

      <section className="cockpit-showcase racer-showcase" id="moli-racer" aria-labelledby="racer-showcase-title">
        <header className="cockpit-intro page-shell">
          <p className="eyebrow">MOLI Racer / Three levels of motion</p>
          <h2 id="racer-showcase-title">Beyond the cockpit.<br />Own the drive.</h2>
          <p>Three complete motion racing machines, installed and calibrated so you can sit down and race.</p>
        </header>

        <div
          className="cockpit-story page-shell"
          onFocusCapture={() => setPausedShowcase('racer')}
          onBlurCapture={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) setPausedShowcase(null)
          }}
        >
          <div ref={racerModelListRef} className="cockpit-model-list" aria-live="polite">
            {racerModels.map((model) => (
              <article
                key={model.id}
                className={activeRacerModel === model.id ? 'cockpit-model active' : 'cockpit-model'}
              >
                <img className="cockpit-model-mobile-image" src={model.image} alt={model.imageAlt} />
                <div className="cockpit-model-meta">
                  <span>{model.index}</span>
                  <small>{model.tier}</small>
                </div>
                <h3>{model.name}</h3>
                <h4>{model.headline}</h4>
                <div className="cockpit-commerce">
                  <p>
                    <strong>{formatWholeMoney(model.priceCents)}</strong>
                    <small>Incl. GST</small>
                  </p>
                  <button type="button" onClick={openRacerCollection}>
                    Explore &amp; buy <span aria-hidden="true">→</span>
                  </button>
                </div>
              </article>
            ))}
          </div>

          <div className="cockpit-media-column">
            <div className="cockpit-media-sticky">
              <div className="cockpit-image-stage" aria-live="polite">
                {racerModels.map((model) => (
                  <img
                    key={model.id}
                    className={activeRacerModel === model.id ? 'active' : ''}
                    src={model.image}
                    alt={activeRacerModel === model.id ? model.imageAlt : ''}
                  />
                ))}
                <div className="cockpit-image-caption">
                  <span>{selectedRacerModel.index} / 03</span>
                  <strong>{selectedRacerModel.name}</strong>
                </div>
              </div>

              <nav className="cockpit-ladder" aria-label="MOLI Racer models">
                {racerModels.map((model) => (
                  <button
                    key={model.id}
                    type="button"
                    className={activeRacerModel === model.id ? 'active' : ''}
                    aria-current={activeRacerModel === model.id ? 'true' : undefined}
                    onClick={() => selectRacerModel(model.id)}
                  >
                    <span>{model.index}</span>
                    <strong>{model.name.replace('MOLI Racer ', '')}</strong>
                  </button>
                ))}
              </nav>
              <button className="cockpit-compare-trigger" type="button" onClick={openRacerComparison}>
                Compare all models <span aria-hidden="true">→</span>
              </button>
            </div>
          </div>
        </div>

        <button className="cockpit-compare-trigger cockpit-compare-mobile page-shell" type="button" onClick={openRacerComparison}>
          Compare all models <span aria-hidden="true">→</span>
        </button>

        <p className="cockpit-trust page-shell">
          <span aria-hidden="true">●</span> Complete machine · Installed and calibrated · Supported in Australia
        </p>
      </section>

      <section className="cockpit-showcase arena-showcase" id="moli-arena" aria-labelledby="arena-showcase-title">
        <header className="cockpit-intro page-shell">
          <p className="eyebrow">JG MOLI Arena / Three ways to play together</p>
          <h2 id="arena-showcase-title">One room.<br />Everyone plays.</h2>
          <p>Three complete light-and-projection systems that turn familiar spaces into shared play.</p>
        </header>

        <div
          className="cockpit-story page-shell"
          onFocusCapture={() => setPausedShowcase('arena')}
          onBlurCapture={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) setPausedShowcase(null)
          }}
        >
          <div ref={arenaModelListRef} className="cockpit-model-list" aria-live="polite">
            {arenaModels.map((model) => (
              <article
                key={model.id}
                className={activeArenaModel === model.id ? 'cockpit-model active' : 'cockpit-model'}
              >
                <img className="cockpit-model-mobile-image" src={model.image} alt={model.imageAlt} />
                <div className="cockpit-model-meta">
                  <span>{model.index}</span>
                  <small>{model.tier}</small>
                </div>
                <h3>{model.name}</h3>
                <h4>{model.headline}</h4>
                <div className="cockpit-commerce">
                  <p>
                    <strong>{formatWholeMoney(model.priceCents)}</strong>
                    <small>Incl. GST</small>
                  </p>
                  <button type="button" onClick={openArenaCollection}>
                    Explore &amp; buy <span aria-hidden="true">→</span>
                  </button>
                </div>
              </article>
            ))}
          </div>

          <div className="cockpit-media-column">
            <div className="cockpit-media-sticky">
              <div className="cockpit-image-stage" aria-live="polite">
                {arenaModels.map((model) => (
                  <img
                    key={model.id}
                    className={activeArenaModel === model.id ? 'active' : ''}
                    src={model.image}
                    alt={activeArenaModel === model.id ? model.imageAlt : ''}
                  />
                ))}
                <div className="cockpit-image-caption">
                  <span>{selectedArenaModel.index} / 03</span>
                  <strong>{selectedArenaModel.name}</strong>
                </div>
              </div>

              <nav className="cockpit-ladder" aria-label="JG MOLI Arena models">
                {arenaModels.map((model) => (
                  <button
                    key={model.id}
                    type="button"
                    className={activeArenaModel === model.id ? 'active' : ''}
                    aria-current={activeArenaModel === model.id ? 'true' : undefined}
                    onClick={() => selectArenaModel(model.id)}
                  >
                    <span>{model.index}</span>
                    <strong>{model.name.replace('JG MOLI Arena ', '')}</strong>
                  </button>
                ))}
              </nav>
              <button className="cockpit-compare-trigger" type="button" onClick={openArenaComparison}>
                Compare all models <span aria-hidden="true">→</span>
              </button>
            </div>
          </div>
        </div>

        <button className="cockpit-compare-trigger cockpit-compare-mobile page-shell" type="button" onClick={openArenaComparison}>
          Compare all models <span aria-hidden="true">→</span>
        </button>

        <p className="cockpit-trust page-shell">
          <span aria-hidden="true">●</span> 60 games included · Installed and calibrated · Supported in Australia
        </p>
      </section>

      <section className="gear-bridge" aria-labelledby="gear-bridge-title">
        <div className="page-shell">
          <p className="eyebrow">Beyond complete systems</p>
          <h2 id="gear-bridge-title">Shape the rest of your setup.</h2>
          <p>Displays · Controls · Audio · Furniture</p>
        </div>
      </section>

      <section className="quick-entry page-shell" id="shop-by-gear" aria-labelledby="quick-entry-title">
        <div>
          <p className="eyebrow">Product-first / Shop directly</p>
          <h2 id="quick-entry-title">Go straight to the gear.</h2>
          <button className="quick-entry-all" type="button" onClick={() => openCatalog({ title: 'All gaming gear', allowedCategories: allGearIds, source: 'direct' })}>
            Browse all gear <span aria-hidden="true">→</span>
          </button>
        </div>
        <div className="gear-shortcuts" aria-label="Shop by product">
          {gearShortcuts.map((gear) => (
            <button
              key={gear.id}
              type="button"
              aria-pressed={gearTarget === gear.id}
              className={gearTarget === gear.id ? 'active' : ''}
              onClick={() => setGearTarget(gear.id)}
            >
              {gear.label} <span aria-hidden="true">↗</span>
            </button>
          ))}
        </div>
        <div key={`${selectedGear.id}-copy`} className="gear-selection-summary" aria-live="polite">
          <span className="result-label">Selected gear / {selectedGear.label}</span>
          <h3>{selectedGear.headline}</h3>
          <p>{selectedGear.message}</p>
          <button className="product-entry-action" type="button" onClick={() => openCatalog({ title: `${selectedGear.label} products`, allowedCategories: [selectedGear.id], source: 'direct' }, selectedGear.id)}>
            View all {selectedGear.label} <span aria-hidden="true">→</span>
          </button>
        </div>
        <article key={selectedGear.id} className="gear-preview" aria-live="polite">
          <img src={selectedGear.image} alt={`${selectedGear.label} shaping the gaming experience`} />
          <span>Product in play</span>
        </article>
      </section>

      <div className="guided-path has-selection">
      <section className="recommendation-intro page-shell" id="how-it-works" aria-labelledby="recommendation-title">
        <div>
          <p className="eyebrow">How recommendations work</p>
          <h2 id="recommendation-title">Two choices.<br />One upgrade path.</h2>
        </div>
        <ol className="recommendation-steps">
          <li><span>01</span><strong>What you play on</strong></li>
          <li><span>02</span><strong>How you want to play</strong></li>
          <li><span>03</span><strong>Upgrades that connect them</strong></li>
        </ol>
        <p className="recommendation-note">Complete systems live above. This guide narrows the individual upgrades around the way you play.</p>
      </section>
      <section ref={startingPointRef} className="starting-point" id="starting-point" onClick={() => setHasGuidedInteraction(true)} onFocusCapture={() => setHasGuidedInteraction(true)}>
        <div className="page-shell">
          <div className="starting-sidebar">
            <div className="section-heading compact-heading">
              <p className="eyebrow">Step 01 / Current device</p>
              <h2>What do you<br />play on today?</h2>
              <p className="section-intro">Choose what you own. We’ll show the gear that works with it.</p>
            </div>

            <div className="starting-options" aria-label="Choose your current gaming platform">
              {startingPoints.map((point) => (
                <button
                  key={point.id}
                  type="button"
                  aria-pressed={startingPoint === point.id}
                  className={startingPoint === point.id ? 'starting-option active' : 'starting-option'}
                  onClick={() => selectStartingPoint(point)}
                >
                  <span>{point.index}</span>
                  {point.label}
                </button>
              ))}
            </div>

          </div>

          <div className="starting-result" aria-live="polite">
            <article key={displayedStart.id} className="starting-preview">
              <img src={displayedStart.image} alt={`${displayedStart.label} expanded into a gaming setup`} />
              <span>{selectedStart ? `${selectedStart.label} / Your starting point` : 'Preview / Gaming Laptop'}</span>
            </article>
          </div>
        </div>
      </section>

      <section ref={experiencesRef} className="experiences" id="experiences" onClick={() => setHasGuidedInteraction(true)} onFocusCapture={() => setHasGuidedInteraction(true)}>
        <div className="page-shell">
          <div className="section-heading experience-heading">
            <div>
              <p className="eyebrow">Step 02 / Desired experience</p>
              <h2>Choose a world.<br />See what builds it.</h2>
            </div>
            <p className="experience-intro">The feeling comes first. The equipment explains how to create it.</p>
          </div>

          <div className="experience-options" role="tablist" aria-label="Choose a gaming experience">
            {experiences.map((item) => (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={activeExperience === item.id}
                className={activeExperience === item.id ? 'experience-option active' : 'experience-option'}
                onClick={() => selectExperience(item)}
              >
                <span>{item.index}</span>
                <strong>{item.title}</strong>
              </button>
            ))}
          </div>

          <article className="experience-stage" role="tabpanel">
            <div className="experience-frame">
              {experiences.map((item) => (
                <img
                  key={item.id}
                  className={activeExperience === item.id ? 'active' : ''}
                  src={item.image}
                  alt=""
                />
              ))}
              <div className="experience-stage-shade" />
              <div className="experience-ambient" />
              <span
                className="experience-focus"
                style={{ left: selectedExperienceProduct.focus[0], top: selectedExperienceProduct.focus[1] }}
                aria-hidden="true"
              />
              <p className="experience-scene-note">{selectedExperience.caption}</p>
            </div>

            <div className="experience-console">
              <div className="experience-stage-copy">
                <span>Selected world</span>
                <h3>{selectedExperience.title}</h3>
              </div>

              <div className="experience-products" aria-label={`Equipment for ${selectedExperience.title}`}>
                {selectedExperience.products.map((product) => (
                  <button
                    key={product.label}
                    type="button"
                    aria-pressed={selectedExperienceProduct.label === product.label}
                    className={selectedExperienceProduct.label === product.label ? 'active' : ''}
                    onClick={() => selectExperienceProduct(product)}
                  >
                    {product.label}
                  </button>
                ))}
              </div>

              <div className="experience-unlock" aria-live="polite">
                <span>What this upgrades</span>
                <p>{selectedExperienceProduct.benefit}</p>
                {experience === 'racing' && (
                  <button type="button" onClick={() => openHomeSection('moli-racer')}>
                    Explore complete MOLI Racer <span aria-hidden="true">→</span>
                  </button>
                )}
              </div>
            </div>
          </article>
        </div>
      </section>

      {showSetupTray && (
        <aside className="setup-tray" aria-label="Current setup selections" aria-live="polite">
          <span className="setup-tray-label">JG MOLI recommendation</span>
          <button className="setup-tray-choice" type="button" onClick={() => scrollToSection('starting-point')}>
            <small>Device</small>
            <strong>{selectedStart?.label ?? 'Add device'}</strong>
          </button>
          <button className="setup-tray-choice" type="button" onClick={() => scrollToSection('experiences')}>
            <small>Experience</small>
            <strong>{experience ? selectedExperience.title : 'Add experience'}</strong>
          </button>
          <div className="setup-category">
            <button className="setup-tray-choice" type="button" aria-expanded={isCategoryMenuOpen} onClick={() => setIsCategoryMenuOpen((open) => !open)}>
              <small>Category</small>
              <strong>{productFocus ? gearShortcuts.find((gear) => gear.id === productFocus)?.label : 'All gear'} <span aria-hidden="true">⌃</span></strong>
            </button>
            {isCategoryMenuOpen && (
              <div className="setup-category-menu" aria-label="Filter recommendation by category">
                <button type="button" className={!productFocus ? 'active' : ''} onClick={() => selectRecommendationCategory(null)}>All recommended gear</button>
                {recommendationGearIds.map((id) => (
                  <button key={id} type="button" className={productFocus === id ? 'active' : ''} onClick={() => selectRecommendationCategory(id)}>
                    {gearShortcuts.find((gear) => gear.id === id)?.label}
                  </button>
                ))}
              </div>
            )}
          </div>
          <button className="setup-tray-mobile-summary" type="button" onClick={() => scrollToSection(experience ? 'experiences' : 'starting-point')}>
            <small>Your setup</small>
            <strong>{experience ? selectedExperience.title : selectedStart?.label ?? 'All gaming gear'} · {productFocus ? gearShortcuts.find((gear) => gear.id === productFocus)?.label : 'All gear'}</strong>
          </button>
          <button
            className="setup-tray-action"
            type="button"
            onClick={() => openCatalog({ title: recommendationTitle, allowedCategories: recommendationGearIds, source: 'recommendation' }, productFocus)}
          >
            {startingPoint || experience ? 'View my recommendations' : 'Browse all products'} <span aria-hidden="true">→</span>
          </button>
        </aside>
      )}
      </div>

      <section className="site-support" id="support" aria-labelledby="support-title">
        <div className="page-shell">
          <div className="support-heading">
            <div>
              <p className="eyebrow">JG MOLI / Help &amp; contact</p>
              <h2 id="support-title">Help that keeps you<br />in your world.</h2>
            </div>
            <p>From choosing your gear to questions after delivery, our Melbourne team is here when you need us.</p>
          </div>

          <div className="support-routes">
            <button type="button" onClick={() => authUser ? void openOrders() : openAccount()}>
              <span>01 / Orders</span>
              <strong>Orders &amp; invoices</strong>
              <small>Review a purchase or reopen your tax invoice.</small>
              <em aria-hidden="true">→</em>
            </button>
            <button type="button" onClick={openRecommendation}>
              <span>02 / Recommendations</span>
              <strong>Find the right upgrade</strong>
              <small>Start with what you own and how you want to play.</small>
              <em aria-hidden="true">→</em>
            </button>
            <a href="mailto:support@jgmoli.com.au">
              <span>03 / Melbourne</span>
              <strong>Talk to our team</strong>
              <small>Product, delivery and after-sales questions.</small>
              <em aria-hidden="true">↗</em>
            </a>
          </div>
        </div>
      </section>

      <footer className="site-footer">
        <div className="site-footer-main page-shell">
          <div className="site-footer-brand">
            <a className="footer-brand" href="#top" aria-label="JG MOLI home">
              <span>JG</span>
              <strong>MOLI</strong>
            </a>
            <p>Gaming gear, shaped around how you play.</p>
          </div>
          <div className="site-footer-group">
            <span>Company</span>
            <strong>AI CYBER AUSTRALIA PTY LTD</strong>
            <p>ABN 22 689 546 450</p>
          </div>
          <address className="site-footer-group">
            <span>Visit</span>
            <strong>205 Kensington Rd</strong>
            <p>West Melbourne VIC 3003<br />Australia</p>
          </address>
          <address className="site-footer-group">
            <span>Contact</span>
            <a href="mailto:support@jgmoli.com.au">support@jgmoli.com.au</a>
            <a href="tel:+61436365016">+61 436 365 016</a>
          </address>
        </div>
        <div className="site-footer-base page-shell">
          <span>© {new Date().getFullYear()} AI CYBER AUSTRALIA PTY LTD</span>
          <a href="#top">Back to top <span aria-hidden="true">↑</span></a>
        </div>
      </footer>

      {isCockpitCompareOpen && (
        <section className="cockpit-compare-view" role="dialog" aria-modal="true" aria-labelledby="cockpit-compare-title">
          <div className="cockpit-compare-shell">
            <header className="cockpit-compare-header">
              <div>
                <p className="eyebrow">MOLI Cockpit / Model comparison</p>
                <h2 id="cockpit-compare-title">See what changes<br />at every level.</h2>
                <p>Compare the differences that shape the experience, then choose the cockpit that fits your space and the way you play.</p>
              </div>
              <button type="button" onClick={closeCockpitComparison} aria-label="Close model comparison">×</button>
            </header>

            <p className="cockpit-compare-swipe">Swipe to compare <span aria-hidden="true">→</span></p>
            <div className="cockpit-compare-table-wrap">
              <table className="cockpit-compare-table">
                <thead>
                  <tr>
                    <th scope="col">Model</th>
                    {cockpitModels.map((model) => (
                      <th scope="col" key={model.id}>
                        <span>{model.index} / {model.tier}</span>
                        <strong>{model.name}</strong>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <th scope="row">Best for</th>
                    {cockpitModels.map((model) => <td key={model.id}>{model.idealFor}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Experience</th>
                    {cockpitModels.map((model) => <td key={model.id}>{model.headline}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Structure</th>
                    {cockpitModels.map((model) => <td key={model.id}>{model.structure}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Visual system</th>
                    {cockpitModels.map((model) => <td key={model.id}>{model.visualSystem}</td>)}
                  </tr>
                  <tr className="cockpit-compare-price-row">
                    <th scope="row">Price</th>
                    {cockpitModels.map((model) => (
                      <td key={model.id}>
                        <strong>{formatWholeMoney(model.priceCents)}</strong>
                        <small>Incl. GST</small>
                      </td>
                    ))}
                  </tr>
                </tbody>
              </table>
            </div>

            <footer className="cockpit-compare-footer">
              <p><span aria-hidden="true">●</span> Configured for you · Installed at home · Supported in Australia</p>
              <button type="button" onClick={openCockpitCollection}>Explore &amp; buy <span aria-hidden="true">→</span></button>
            </footer>
          </div>
        </section>
      )}

      {isRacerCompareOpen && (
        <section className="cockpit-compare-view racer-compare-view" role="dialog" aria-modal="true" aria-labelledby="racer-compare-title">
          <div className="cockpit-compare-shell">
            <header className="cockpit-compare-header">
              <div>
                <p className="eyebrow">MOLI Racer / Model comparison</p>
                <h2 id="racer-compare-title">See what moves<br />at every level.</h2>
                <p>Compare the display, controls and motion platform, then choose the complete machine that matches the way you race.</p>
              </div>
              <button type="button" onClick={closeRacerComparison} aria-label="Close Racer model comparison">×</button>
            </header>

            <p className="cockpit-compare-swipe">Swipe to compare <span aria-hidden="true">→</span></p>
            <div className="cockpit-compare-table-wrap">
              <table className="cockpit-compare-table">
                <thead>
                  <tr>
                    <th scope="col">Model</th>
                    {racerModels.map((model) => (
                      <th scope="col" key={model.id}>
                        <span>{model.index} / {model.tier}</span>
                        <strong>{model.name}</strong>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <th scope="row">Best for</th>
                    {racerModels.map((model) => <td key={model.id}>{model.idealFor}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Motion</th>
                    {racerModels.map((model) => <td key={model.id}>{model.motionSystem}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Display</th>
                    {racerModels.map((model) => <td key={model.id}>{model.visualSystem}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Controls</th>
                    {racerModels.map((model) => <td key={model.id}>{model.controls}</td>)}
                  </tr>
                  <tr className="cockpit-compare-price-row">
                    <th scope="row">Price</th>
                    {racerModels.map((model) => (
                      <td key={model.id}>
                        <strong>{formatWholeMoney(model.priceCents)}</strong>
                        <small>Incl. GST</small>
                      </td>
                    ))}
                  </tr>
                </tbody>
              </table>
            </div>

            <footer className="cockpit-compare-footer">
              <p><span aria-hidden="true">●</span> Complete machine · Installed and calibrated · Supported in Australia</p>
              <button type="button" onClick={openRacerCollection}>Explore &amp; buy <span aria-hidden="true">→</span></button>
            </footer>
          </div>
        </section>
      )}

      {isArenaCompareOpen && (
        <section className="cockpit-compare-view arena-compare-view" role="dialog" aria-modal="true" aria-labelledby="arena-compare-title">
          <div className="cockpit-compare-shell">
            <header className="cockpit-compare-header">
              <div>
                <p className="eyebrow">JG MOLI Arena / Model comparison</p>
                <h2 id="arena-compare-title">See how the room<br />opens up.</h2>
                <p>Compare players, content, installation and support, then choose the shared experience that fits your space.</p>
              </div>
              <button type="button" onClick={closeArenaComparison} aria-label="Close Arena model comparison">×</button>
            </header>

            <p className="cockpit-compare-swipe">Swipe to compare <span aria-hidden="true">→</span></p>
            <div className="cockpit-compare-table-wrap">
              <table className="cockpit-compare-table">
                <thead>
                  <tr>
                    <th scope="col">Model</th>
                    {arenaModels.map((model) => (
                      <th scope="col" key={model.id}>
                        <span>{model.index} / {model.tier}</span>
                        <strong>{model.name}</strong>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <th scope="row">Best for</th>
                    {arenaModels.map((model) => <td key={model.id}>{model.idealFor}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Players</th>
                    {arenaModels.map((model) => <td key={model.id}>{model.players}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Content</th>
                    {arenaModels.map((model) => <td key={model.id}>{model.content}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Installation</th>
                    {arenaModels.map((model) => <td key={model.id}>{model.installation}</td>)}
                  </tr>
                  <tr>
                    <th scope="row">Support</th>
                    {arenaModels.map((model) => <td key={model.id}>{model.support}</td>)}
                  </tr>
                  <tr className="cockpit-compare-price-row">
                    <th scope="row">Price</th>
                    {arenaModels.map((model) => (
                      <td key={model.id}>
                        <strong>{formatWholeMoney(model.priceCents)}</strong>
                        <small>Incl. GST</small>
                      </td>
                    ))}
                  </tr>
                </tbody>
              </table>
            </div>

            <footer className="cockpit-compare-footer">
              <p><span aria-hidden="true">●</span> 60 games included · Installed and calibrated · Supported in Australia</p>
              <button type="button" onClick={openArenaCollection}>Explore &amp; buy <span aria-hidden="true">→</span></button>
            </footer>
          </div>
        </section>
      )}

      {catalogView && (
        <section className="catalog-view" role="dialog" aria-modal="true" aria-labelledby="catalog-title">
          <div className="catalog-shell">
            <header className="catalog-header">
              <div>
                <p className="eyebrow">{catalogView.source === 'recommendation' ? 'JG MOLI recommendation' : catalogView.lockedSubtype ? 'Complete system collection' : 'Shop directly'}</p>
                <h2 id="catalog-title">{catalogView.title}</h2>
                <p>{catalogView.source === 'recommendation'
                  ? 'Matched to your device and the way you want to play.'
                  : catalogView.lockedSubtype
                    ? 'Compare the complete range, then choose the level that fits your space.'
                    : 'Start with the full collection, then narrow it only if you want to.'}</p>
              </div>
              <button className="catalog-close" type="button" onClick={() => leaveManagedView(() => setCatalogView(null))} aria-label="Close product catalogue">×</button>
            </header>

            {catalogView.allowedCategories.length > 1 && (
              <div className="catalog-category-tabs" aria-label="Filter products by category">
                <button type="button" className={!productFocus ? 'active' : ''} onClick={() => { setProductFocus(null); setCatalogSubtype(null) }}>
                  {catalogView.source === 'recommendation'
                    ? 'All matched'
                    : catalogView.allowedCategories.includes('sim')
                      ? 'All products'
                      : 'All gear'}
                </button>
                {catalogView.allowedCategories.map((id) => (
                  <button key={id} type="button" className={productFocus === id ? 'active' : ''} onClick={() => { setProductFocus(id); setCatalogSubtype(null) }}>
                    {catalogCategoryLabels[id]}
                  </button>
                ))}
              </div>
            )}

            {productFocus && !catalogView.lockedSubtype && (
              <div className="catalog-subfilters" aria-label={`Filter ${productFocus} products`}>
                <button type="button" className={!catalogSubtype ? 'active' : ''} onClick={() => setCatalogSubtype(null)}>All</button>
                {catalogSubtypes.map((subtype) => (
                  <button key={subtype} type="button" className={catalogSubtype === subtype ? 'active' : ''} onClick={() => setCatalogSubtype(subtype)}>{subtype}</button>
                ))}
              </div>
            )}

            <div className="catalog-results-heading">
              <span>{visibleCatalogItems.length.toString().padStart(2, '0')} products</span>
              <strong>{productFocus
                ? catalogCategoryLabels[productFocus]
                : catalogView.allowedCategories.includes('sim')
                  ? 'All available products'
                  : 'All available gear'}</strong>
            </div>

            <div className="catalog-grid">
              {isCatalogLoading && <p className="catalog-loading">Loading the collection…</p>}
              {catalogError && <p className="catalog-error" role="alert">{catalogError}</p>}
              {visibleCatalogItems.map((item) => {
                return (
                  <article key={item.id} className="catalog-card">
                    <div className="catalog-card-image"><img src={item.image} alt={`${item.name} product preview`} /></div>
                    <span>{catalogCategoryLabels[item.category]} / {item.subtype}</span>
                    <small>{item.brand}</small>
                    <h3>{item.name}</h3>
                    <p>{item.description}</p>
                    <div className="catalog-card-price">
                      <strong>{formatPrice(item)}</strong>
                      <small>{item.priceIncludesGst ? 'GST included' : 'GST added at checkout'}</small>
                    </div>
                    <div className="catalog-actions">
                      <button className="catalog-buy" type="button" onClick={() => startCheckout({ source: 'BUY_NOW', lines: [{ variantId: item.variantId, quantity: 1 }] })}>
                        Buy now <span aria-hidden="true">→</span>
                      </button>
                      <button className="catalog-add" type="button" onClick={() => addToCart(item)}>
                        Add to cart
                      </button>
                    </div>
                  </article>
                )
              })}
            </div>
          </div>
        </section>
      )}

      {isCartOpen && (
        <section className="cart-overlay" role="dialog" aria-modal="true" aria-labelledby="cart-title">
          <button className="cart-backdrop" type="button" onClick={() => leaveManagedView(() => setIsCartOpen(false))} aria-label="Close cart" />
          <aside className="cart-panel">
            <header className="cart-header">
              <div>
                <p className="eyebrow">Your selection</p>
                <h2 id="cart-title">Your cart.</h2>
                <span>{cartCount} {cartCount === 1 ? 'item' : 'items'}</span>
              </div>
              <button className="cart-close" type="button" onClick={() => leaveManagedView(() => setIsCartOpen(false))} aria-label="Close cart">×</button>
            </header>

            {cartItems.length === 0 ? (
              <div className="cart-empty">
                <p>Your next setup starts with one piece of gear.</p>
                <button type="button" onClick={() => leaveManagedView(() => setIsCartOpen(false))}>Continue exploring <span aria-hidden="true">→</span></button>
              </div>
            ) : (
              <>
                <div className="cart-selection-bar">
                  <button type="button" onClick={toggleAllCartItems}>
                    <span className={selectedCartVariantIds.length === cartLines.length ? 'cart-checkbox selected' : 'cart-checkbox'} aria-hidden="true">✓</span>
                    {selectedCartVariantIds.length === cartLines.length ? 'Clear all' : 'Select all'}
                  </button>
                  <span>{selectedCartItems.length} of {cartItems.length} selected</span>
                </div>
                <div className="cart-lines">
                  {cartItems.map(({ product, quantity }) => (
                    <article className="cart-line" key={product.variantId}>
                      <button
                        className={selectedCartVariantIds.includes(product.variantId) ? 'cart-line-select selected' : 'cart-line-select'}
                        type="button"
                        aria-pressed={selectedCartVariantIds.includes(product.variantId)}
                        aria-label={`${selectedCartVariantIds.includes(product.variantId) ? 'Remove' : 'Add'} ${product.name} ${selectedCartVariantIds.includes(product.variantId) ? 'from' : 'to'} checkout`}
                        onClick={() => toggleCartSelection(product.variantId)}
                      >✓</button>
                      <img src={product.image} alt="" />
                      <div className="cart-line-copy">
                        <small>{product.brand}</small>
                        <h3>{product.name}</h3>
                        <strong>{formatPrice(product)}</strong>
                        <div className="cart-line-actions">
                          <div className="cart-quantity" aria-label={`Quantity for ${product.name}`}>
                            <button type="button" onClick={() => changeCartQuantity(product.variantId, -1)} aria-label={`Decrease ${product.name} quantity`}>−</button>
                            <span>{quantity}</span>
                            <button type="button" onClick={() => changeCartQuantity(product.variantId, 1)} aria-label={`Increase ${product.name} quantity`}>+</button>
                          </div>
                          <button className="cart-remove" type="button" onClick={() => removeFromCart(product.variantId)}>Remove</button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
                <footer className="cart-summary">
                  <div><span>Selected subtotal</span><strong>{new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' }).format(selectedCartSubtotalCents / 100)}</strong></div>
                  <p>{selectedCartItems.length ? 'GST included. Unselected gear stays in your cart.' : 'Choose the gear you want to check out.'}</p>
                  <button
                    className="cart-checkout"
                    type="button"
                    disabled={!selectedCartItems.length}
                    onClick={() => startCheckout({ source: 'CART', lines: selectedCartItems.map(({ variantId, quantity }) => ({ variantId, quantity })) })}
                  >{selectedCartItems.length ? 'Checkout selected' : 'Select gear to checkout'} <span aria-hidden="true">→</span></button>
                  <button className="cart-continue" type="button" onClick={() => leaveManagedView(() => setIsCartOpen(false))}>Continue shopping</button>
                </footer>
              </>
            )}
          </aside>
        </section>
      )}

      {isCheckoutOpen && checkoutDraft && (
        <section ref={checkoutRef} className="checkout-overlay" role="dialog" aria-modal="true" aria-labelledby="checkout-title">
          <div className="checkout-shell">
            <header className="checkout-header">
              <a className="brand" href="#top" onClick={(event) => { event.preventDefault(); closeCheckout() }} aria-label="Close checkout and return to JG MOLI">
                <span className="brand-monogram">JG</span><span>MOLI</span>
              </a>
              <div className="checkout-progress" aria-label="Checkout progress">
                <span className={checkoutStep === 'delivery' ? 'active' : ''}>01 Delivery</span>
                <span className={checkoutStep === 'review' ? 'active' : ''}>02 Review & pay</span>
                <span className={checkoutStep === 'complete' ? 'active' : ''}>03 Confirmed</span>
              </div>
              <button className="checkout-close" type="button" onClick={closeCheckout} aria-label="Close checkout">×</button>
            </header>

            <div className="checkout-layout">
              <div className="checkout-main">
                {checkoutStep === 'delivery' && (
                  <>
                    <div className="checkout-heading">
                      <p className="eyebrow">Delivery / Australia</p>
                      <h2 id="checkout-title">Where should<br />your gear arrive?</h2>
                      <p>We’ll use these details for delivery and your future invoice.</p>
                    </div>
                    <form className="delivery-form" onSubmit={submitDelivery} onChange={() => { setCheckoutError(''); setCheckoutIdempotencyKey(crypto.randomUUID()) }}>
                      <label className="wide">Recipient name<input name="recipientName" autoComplete="name" maxLength={160} defaultValue={authUser?.displayName ?? ''} required /></label>
                      <label>Phone<input name="phone" type="tel" autoComplete="tel" maxLength={40} required /></label>
                      <label>Email<input value={authUser?.email ?? ''} readOnly aria-readonly="true" /></label>
                      <label className="wide">Address<input name="addressLine1" autoComplete="address-line1" maxLength={200} required /></label>
                      <label className="wide">Apartment, suite or unit <small>Optional</small><input name="addressLine2" autoComplete="address-line2" maxLength={200} /></label>
                      <label>Suburb / city<input name="suburb" autoComplete="address-level2" maxLength={120} required /></label>
                      <label>State<select name="state" autoComplete="address-level1" defaultValue="VIC" required>
                        {['ACT', 'NSW', 'NT', 'QLD', 'SA', 'TAS', 'VIC', 'WA'].map((state) => <option key={state}>{state}</option>)}
                      </select></label>
                      <label>Postcode<input name="postcode" inputMode="numeric" autoComplete="postal-code" pattern="[0-9]{4}" maxLength={4} required /></label>
                      <label>Country<input value="Australia" readOnly aria-readonly="true" /></label>
                      {checkoutError && <p className="checkout-error wide" role="alert">{checkoutError}</p>}
                      <button className="checkout-primary wide" type="submit" disabled={isCheckoutSubmitting}>
                        {isCheckoutSubmitting ? 'Checking details…' : 'Review order'} <span aria-hidden="true">→</span>
                      </button>
                    </form>
                  </>
                )}

                {checkoutStep === 'review' && checkoutOrder && (
                  <>
                    <div className="checkout-heading">
                      <p className="eyebrow">Order / {checkoutOrder.orderReference}</p>
                      <h2 id="checkout-title">One last look.</h2>
                      <p>Confirm the products, delivery details and total before payment.</p>
                    </div>
                    <div className="checkout-review-lines">
                      {checkoutOrder.items.map((item) => (
                        <article key={item.variantId}>
                          <div><small>{item.brand}</small><strong>{item.productName}</strong><span>Qty {item.quantity}</span></div>
                          <b>{formatMoney(item.lineTotalCents, checkoutOrder.currency)}</b>
                        </article>
                      ))}
                    </div>
                    <div className="checkout-address-review">
                      <small>Deliver to</small>
                      <p>{checkoutOrder.delivery.recipientName}<br />{checkoutOrder.delivery.addressLine1}{checkoutOrder.delivery.addressLine2 ? `, ${checkoutOrder.delivery.addressLine2}` : ''}<br />{checkoutOrder.delivery.suburb} {checkoutOrder.delivery.state} {checkoutOrder.delivery.postcode}</p>
                      <button type="button" onClick={returnToDelivery} disabled={isCheckoutSubmitting}>Change details</button>
                    </div>
                    <div className="payment-block">
                      <div>
                        <p className="eyebrow">{demoPaymentEnabled ? 'Prototype payment' : 'Secure payment'}</p>
                        <h3>{demoPaymentEnabled ? 'No card will be charged.' : 'Payment provider required.'}</h3>
                        <span>{demoPaymentEnabled ? 'This local-only step proves the order, payment and confirmation flow. Production payment remains disabled until a trusted provider is connected.' : 'Checkout is ready for a trusted payment provider. Payment remains unavailable until that production connection is configured.'}</span>
                      </div>
                      {checkoutError && <p className="checkout-error" role="alert">{checkoutError}</p>}
                      <button className="checkout-primary" type="button" onClick={payForOrder} disabled={isCheckoutSubmitting || !demoPaymentEnabled}>
                        {isCheckoutSubmitting ? 'Confirming…' : demoPaymentEnabled ? `Confirm demo payment · ${formatMoney(checkoutOrder.totalCents, checkoutOrder.currency)}` : 'Payment unavailable'} <span aria-hidden="true">→</span>
                      </button>
                    </div>
                  </>
                )}

                {checkoutStep === 'complete' && checkoutOrder && checkoutPayment && (
                  <div className="checkout-complete">
                    <span className="checkout-complete-mark" aria-hidden="true">✓</span>
                    <p className="eyebrow">Payment confirmed</p>
                    <h2 id="checkout-title">Your world<br />is on its way.</h2>
                    <p>A confirmation for <strong>{checkoutOrder.orderReference}</strong> is ready for {checkoutOrder.customerEmail}.</p>
                    <dl>
                      <div><dt>Order</dt><dd>{checkoutOrder.orderReference}</dd></div>
                      <div><dt>Payment</dt><dd>{checkoutPayment.paymentReference}</dd></div>
                      {checkoutInvoice && <div><dt>Tax invoice</dt><dd>{checkoutInvoice.invoiceNumber}</dd></div>}
                      <div><dt>Total</dt><dd>{formatMoney(checkoutPayment.amountCents, checkoutPayment.currency)}</dd></div>
                    </dl>
                    {checkoutError && <p className="checkout-error" role="alert">{checkoutError}</p>}
                    <div className="checkout-complete-actions">
                      <button className="checkout-primary" type="button" onClick={openInvoice} disabled={!checkoutInvoice}>View tax invoice <span aria-hidden="true">→</span></button>
                      <button className="checkout-secondary" type="button" onClick={closeCheckout}>Continue exploring</button>
                    </div>
                  </div>
                )}
              </div>

              <aside className="checkout-summary" aria-label="Order summary">
                <div><p className="eyebrow">{checkoutDraft.source === 'BUY_NOW' ? 'Buy now' : 'Your cart'}</p><h3>{checkoutItems.length} {checkoutItems.length === 1 ? 'piece of gear' : 'pieces of gear'}</h3></div>
                <div className="checkout-summary-items">
                  {checkoutItems.map(({ product, quantity }) => (
                    <article key={product.variantId}><img src={product.image} alt="" /><div><small>{product.brand}</small><strong>{product.name}</strong><span>{quantity} × {formatPrice(product)}</span></div></article>
                  ))}
                </div>
                <div className="checkout-totals">
                  {checkoutOrder ? (
                    <>
                      <p><span>Subtotal before GST</span><strong>{formatMoney(checkoutOrder.subtotalExGstCents, checkoutOrder.currency)}</strong></p>
                      <p><span>GST</span><strong>{formatMoney(checkoutOrder.gstCents, checkoutOrder.currency)}</strong></p>
                      <p><span>Delivery</span><strong>{checkoutOrder.deliveryCents === 0 ? 'Included' : formatMoney(checkoutOrder.deliveryCents, checkoutOrder.currency)}</strong></p>
                      <p className="total"><span>Total</span><strong>{formatMoney(checkoutOrder.totalCents, checkoutOrder.currency)}</strong></p>
                    </>
                  ) : (
                    <><p className="total"><span>Total</span><strong>{formatMoney(checkoutItems.reduce((sum, item) => sum + item.product.priceCents * item.quantity, 0))}</strong></p><small>GST included. Final totals are confirmed by JG MOLI before payment.</small></>
                  )}
                </div>
              </aside>
            </div>
          </div>
        </section>
      )}

      {isOrdersOpen && (
        <section className="orders-overlay" role="dialog" aria-modal="true" aria-labelledby="orders-title">
          <div className="orders-shell">
            <header className="orders-header">
              <a className="brand" href="#top" onClick={(event) => { event.preventDefault(); leaveManagedView(() => setIsOrdersOpen(false)) }} aria-label="Close orders and return to JG MOLI">
                <span className="brand-monogram">JG</span><span>MOLI</span>
              </a>
              <button type="button" onClick={() => leaveManagedView(() => setIsOrdersOpen(false))} aria-label="Close orders">×</button>
            </header>

            <div className="orders-heading">
              <p className="eyebrow">Your JG MOLI / Order history</p>
              <h2 id="orders-title">Your gear,<br />remembered.</h2>
              <p>Every order and tax invoice stays connected to your account.</p>
            </div>

            {ordersError && <p className="orders-error" role="alert">{ordersError}</p>}
            {isOrdersLoading ? (
              <p className="orders-loading">Loading your orders…</p>
            ) : orders.length === 0 ? (
              <div className="orders-empty">
                <span>00</span>
                <h3>No orders yet.</h3>
                <p>When you choose your first piece of gear, it will appear here.</p>
                <button type="button" onClick={() => { setIsOrdersOpen(false); openCatalog({ title: 'All gaming gear', allowedCategories: allGearIds, source: 'direct' }) }}>Explore all gear <span aria-hidden="true">→</span></button>
              </div>
            ) : (
              <div className="orders-list">
                {orders.map((order) => (
                  <article className="order-card" key={order.id}>
                    <header>
                      <div>
                        <small>{new Intl.DateTimeFormat('en-AU', { dateStyle: 'medium', timeZone: 'Australia/Melbourne' }).format(new Date(order.createdAt))}</small>
                        <h3>{order.orderReference}</h3>
                      </div>
                      <span className={`order-status ${order.status.toLowerCase().replace('_', '-')}`}>{order.status.replaceAll('_', ' ')}</span>
                    </header>
                    <div className="order-items">
                      {order.items.map((item) => {
                        const catalogItem = catalogItems.find((product) => product.variantId === item.variantId)
                        return (
                          <div key={item.variantId}>
                            {catalogItem && <img src={catalogItem.image} alt="" />}
                            <p><small>{item.brand}</small><strong>{item.productName}</strong><span>Qty {item.quantity} · {formatMoney(item.lineTotalCents, order.currency)}</span></p>
                          </div>
                        )
                      })}
                    </div>
                    <footer>
                      <p><span>Total including GST</span><strong>{formatMoney(order.totalCents, order.currency)}</strong></p>
                      {order.status === 'PAID' && (
                        <button type="button" onClick={() => openOrderInvoice(order.orderReference)} disabled={loadingInvoiceReference === order.orderReference}>
                          {loadingInvoiceReference === order.orderReference ? 'Opening…' : 'View tax invoice'} <span aria-hidden="true">→</span>
                        </button>
                      )}
                    </footer>
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {isInvoiceOpen && checkoutInvoice && (
        <section className="invoice-overlay" role="dialog" aria-modal="true" aria-labelledby="invoice-title">
          <div className="invoice-toolbar">
            <a className="brand" href="#top" onClick={(event) => { event.preventDefault(); closeInvoice() }} aria-label="Close invoice and return to JG MOLI">
              <span className="brand-monogram">JG</span><span>MOLI</span>
            </a>
            <div>
              <button type="button" onClick={() => window.print()}>Print / save PDF <span aria-hidden="true">↓</span></button>
              <button type="button" onClick={closeInvoice}>Close</button>
            </div>
          </div>
          <article className="invoice-sheet">
            <header className="invoice-heading">
              <div>
                <p className="eyebrow">Payment confirmed / {checkoutInvoice.status}</p>
                <h2 id="invoice-title">Tax<br />invoice.</h2>
              </div>
              <div className="invoice-identity">
                <span>Invoice</span><strong>{checkoutInvoice.invoiceNumber}</strong>
                <span>Issued</span><strong>{new Intl.DateTimeFormat('en-AU', { dateStyle: 'long', timeZone: 'Australia/Melbourne' }).format(new Date(checkoutInvoice.issuedAt))}</strong>
              </div>
            </header>

            <div className="invoice-parties">
              <section>
                <small>From</small>
                <h3>{checkoutInvoice.sellerTradingName}</h3>
                <p>{checkoutInvoice.sellerLegalName}<br />ABN {checkoutInvoice.sellerAbn}<br />{checkoutInvoice.sellerAddress}</p>
                <p>{checkoutInvoice.sellerEmail}<br />{checkoutInvoice.sellerPhone}</p>
              </section>
              <section>
                <small>Bill to</small>
                <h3>{checkoutInvoice.buyerName}</h3>
                <p>{checkoutInvoice.buyerEmail}<br />{checkoutInvoice.buyerAddress.addressLine1}{checkoutInvoice.buyerAddress.addressLine2 ? <><br />{checkoutInvoice.buyerAddress.addressLine2}</> : null}<br />{checkoutInvoice.buyerAddress.suburb} {checkoutInvoice.buyerAddress.state} {checkoutInvoice.buyerAddress.postcode}<br />Australia</p>
              </section>
              <section className="invoice-references">
                <small>References</small>
                <p><span>Order</span><strong>{checkoutInvoice.orderReference}</strong></p>
                <p><span>Payment</span><strong>{checkoutInvoice.paymentReference}</strong></p>
                <p><span>Currency</span><strong>{checkoutInvoice.currency}</strong></p>
              </section>
            </div>

            <div className="invoice-lines">
              <div className="invoice-line invoice-line-head"><span>Item</span><span>Qty</span><span>Ex GST</span><span>GST</span><span>Total</span></div>
              {checkoutInvoice.lines.map((line) => (
                <div className="invoice-line" key={line.lineNumber}>
                  <span><small>{line.sku}</small><strong>{line.description}</strong><em>Taxable supply</em></span>
                  <span>{line.quantity}</span>
                  <span>{formatMoney(line.unitPriceExGstCents, checkoutInvoice.currency)}</span>
                  <span>{formatMoney(line.gstCents, checkoutInvoice.currency)}</span>
                  <span>{formatMoney(line.lineTotalIncGstCents, checkoutInvoice.currency)}</span>
                </div>
              ))}
            </div>

            <footer className="invoice-footer">
              <div>
                <p>Thank you for choosing JG MOLI.</p>
                <span>This document is your proof of purchase. All listed items are taxable supplies.</span>
              </div>
              <dl>
                <div><dt>Subtotal excluding GST</dt><dd>{formatMoney(checkoutInvoice.subtotalExGstCents, checkoutInvoice.currency)}</dd></div>
                <div><dt>GST</dt><dd>{formatMoney(checkoutInvoice.gstCents, checkoutInvoice.currency)}</dd></div>
                <div><dt>Delivery</dt><dd>{checkoutInvoice.deliveryCents ? formatMoney(checkoutInvoice.deliveryCents, checkoutInvoice.currency) : 'Included'}</dd></div>
                <div className="invoice-total"><dt>Total including GST</dt><dd>{formatMoney(checkoutInvoice.totalCents, checkoutInvoice.currency)}</dd></div>
                <div><dt>Amount paid</dt><dd>{formatMoney(checkoutInvoice.amountPaidCents, checkoutInvoice.currency)}</dd></div>
                <div><dt>Balance</dt><dd>{formatMoney(0, checkoutInvoice.currency)}</dd></div>
              </dl>
            </footer>
          </article>
        </section>
      )}

      {isAccountOpen && !authUser && (
        <section className="account-overlay" role="dialog" aria-modal="true" aria-labelledby="account-title">
          <button className="account-backdrop" type="button" onClick={closeAccount} aria-label="Close account" />
          <div className="account-panel">
            <button className="account-close" type="button" onClick={closeAccount} aria-label="Close account">×</button>
            <div className="account-content">
              <div className="account-tabs" aria-label="Account action">
                <button type="button" className={authMode === 'login' ? 'active' : ''} onClick={() => { setAuthMode('login'); setAuthError('') }}>Log in</button>
                <button type="button" className={authMode === 'register' ? 'active' : ''} onClick={() => { setAuthMode('register'); setAuthError('') }}>Create account</button>
              </div>
              <div className="account-form-heading">
                <p className="eyebrow">{authMode === 'login' ? 'Welcome back' : authMode === 'register' ? 'Start your profile' : 'Account access'}</p>
                <h2 id="account-title">{authMode === 'login' ? 'Enter your world.' : authMode === 'register' ? 'Make it yours.' : authMode === 'forgot' ? 'Find your way back.' : 'Choose a new key.'}</h2>
              </div>
              <form className="account-form" onSubmit={submitAuth}>
                {authMode === 'register' && (
                  <label>Name<input name="displayName" autoComplete="name" maxLength={120} required /></label>
                )}
                {authMode !== 'reset' && (
                  <label>Email<input name="email" type="email" autoComplete="email" maxLength={320} required /></label>
                )}
                {authMode !== 'forgot' && (
                  <label>{authMode === 'reset' ? 'New password' : 'Password'}<input name="password" type="password" autoComplete={authMode === 'login' ? 'current-password' : 'new-password'} minLength={8} maxLength={200} required /></label>
                )}
                {authError && <p className="account-error" role="alert">{authError}</p>}
                {authNotice && <p className="account-notice" role="status">{authNotice}</p>}
                <button className="account-primary" type="submit" disabled={isAuthSubmitting}>
                  {isAuthSubmitting ? 'One moment…' : authMode === 'login' ? 'Log in' : authMode === 'register' ? 'Create account' : authMode === 'forgot' ? 'Send reset link' : 'Update password'} <span aria-hidden="true">→</span>
                </button>
                {authMode === 'login' && <button className="account-secondary" type="button" onClick={() => { setAuthMode('forgot'); setAuthError(''); setAuthNotice('') }}>Forgot password?</button>}
                {(authMode === 'forgot' || authMode === 'reset') && <button className="account-secondary" type="button" onClick={() => { setAuthMode('login'); setAuthError(''); setAuthNotice('') }}>Back to log in</button>}
              </form>
            </div>
          </div>
        </section>
      )}
    </main>
  )
}
