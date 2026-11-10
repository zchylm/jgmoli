import { StrictMode } from 'react'
import { createRoot, hydrateRoot } from 'react-dom/client'
import { App } from './app/App'
import { loadToken } from './services/auth'
import './app/styles/tokens.css'
import './app/styles/global.css'

if ('scrollRestoration' in window.history) window.history.scrollRestoration = 'manual'

const root = document.getElementById('root')!
const privateEntry = Boolean(window.location.search) || window.location.hash.startsWith('#/')
if (privateEntry) document.querySelector('meta[name="robots"]')?.setAttribute('content', 'noindex, follow')

const app = (
  <StrictMode>
    <App />
  </StrictMode>
)

if (root.hasChildNodes() && !privateEntry && !loadToken()) hydrateRoot(root, app)
else createRoot(root).render(app)
