import { useEffect, useState } from 'react'
import { AdminPage } from '../pages/AdminPage'
import { HomePage } from '../pages/HomePage'

export function App() {
  const [isAdminRoute, setIsAdminRoute] = useState(() => window.location.hash.startsWith('#/admin'))

  useEffect(() => {
    const updateRoute = () => setIsAdminRoute(window.location.hash.startsWith('#/admin'))
    window.addEventListener('hashchange', updateRoute)
    return () => window.removeEventListener('hashchange', updateRoute)
  }, [])

  return isAdminRoute ? <AdminPage /> : <HomePage />
}
