import { useCallback, useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react'
import { getCurrentUser, loadToken, type AuthUser } from '../services/auth'
import {
  adjustAdminInventory,
  createAdminInventoryItem,
  getAdminInventory,
  getAdminMovements,
  getAdminOrder,
  getAdminOrders,
  getAdminOverview,
  getAdminProducts,
  updateAdminProduct,
  updateFulfilment,
  type AdminInventory,
  type AdminMovement,
  type AdminOrderDetail,
  type AdminOrderSummary,
  type AdminOverview,
  type AdminProduct,
} from '../services/admin'
import './AdminPage.css'

type AdminView = 'overview' | 'orders' | 'products' | 'inventory'

const money = (cents: number, currency = 'AUD') => new Intl.NumberFormat('en-AU', { style: 'currency', currency }).format(cents / 100)
const date = (value: string) => new Intl.DateTimeFormat('en-AU', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Australia/Melbourne' }).format(new Date(value))

export function AdminPage() {
  const token = loadToken()
  const [user, setUser] = useState<AuthUser | null>(null)
  const [view, setView] = useState<AdminView>('overview')
  const [overview, setOverview] = useState<AdminOverview | null>(null)
  const [orders, setOrders] = useState<AdminOrderSummary[]>([])
  const [products, setProducts] = useState<AdminProduct[]>([])
  const [inventory, setInventory] = useState<AdminInventory[]>([])
  const [movements, setMovements] = useState<AdminMovement[]>([])
  const [selectedOrder, setSelectedOrder] = useState<AdminOrderDetail | null>(null)
  const [selectedProduct, setSelectedProduct] = useState<AdminProduct | null>(null)
  const [selectedInventory, setSelectedInventory] = useState<AdminInventory | null>(null)
  const [isNewInventoryOpen, setIsNewInventoryOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [orderStatus, setOrderStatus] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(Boolean(token))

  const load = useCallback(async (nextView: AdminView, nextQuery = query, nextStatus = orderStatus) => {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      if (nextView === 'overview') setOverview(await getAdminOverview(token))
      if (nextView === 'orders') setOrders(await getAdminOrders(token, nextQuery, nextStatus))
      if (nextView === 'products') setProducts(await getAdminProducts(token))
      if (nextView === 'inventory') {
        const [stock, history] = await Promise.all([getAdminInventory(token), getAdminMovements(token)])
        setInventory(stock)
        setMovements(history)
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'The admin workspace is temporarily unavailable.')
    } finally {
      setLoading(false)
    }
  }, [orderStatus, query, token])

  useEffect(() => {
    if (!token) {
      return
    }
    getCurrentUser(token)
      .then((account) => {
        setUser(account)
        if (account.role === 'ADMIN') return getAdminOverview(token).then(setOverview)
      })
      .catch((reason) => setError(reason instanceof Error ? reason.message : 'Log in again to continue.'))
      .finally(() => setLoading(false))
  }, [token])

  const changeView = (next: AdminView) => {
    setView(next)
    setNotice('')
    setSelectedOrder(null)
    setSelectedProduct(null)
    setSelectedInventory(null)
    void load(next)
  }

  const openOrder = async (reference: string) => {
    if (!token) return
    setError('')
    try { setSelectedOrder(await getAdminOrder(token, reference)) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'That order is unavailable.') }
  }

  const submitOrderSearch = (event: FormEvent) => {
    event.preventDefault()
    void load('orders', query, orderStatus)
  }

  const saveFulfilment = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !selectedOrder) return
    const data = new FormData(event.currentTarget)
    try {
      const updated = await updateFulfilment(token, selectedOrder.summary.orderReference, String(data.get('status')), String(data.get('carrier') ?? ''), String(data.get('trackingNumber') ?? ''))
      setSelectedOrder(updated)
      setNotice('Order fulfilment updated and recorded.')
      await load('orders')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'The order could not be updated.') }
  }

  const saveProduct = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !selectedProduct) return
    const data = new FormData(event.currentTarget)
    try {
      const updated = await updateAdminProduct(token, selectedProduct.variantId, Math.round(Number(data.get('price')) * 100), String(data.get('status')), data.get('active') === 'on')
      setSelectedProduct(updated)
      setNotice('Product availability and GST-inclusive price updated.')
      await load('products')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'The product could not be updated.') }
  }

  const saveInventory = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token || !selectedInventory) return
    const data = new FormData(event.currentTarget)
    try {
      const updated = await adjustAdminInventory(token, selectedInventory.itemId, String(data.get('movementType')), Number(data.get('quantityDelta')), String(data.get('reason')), Number(data.get('reorderLevel')))
      setSelectedInventory(updated)
      setNotice('Inventory balance and movement history updated.')
      await load('inventory')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Inventory could not be updated.') }
  }

  const createInventoryItem = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    const data = new FormData(event.currentTarget)
    try {
      await createAdminInventoryItem(token, {
        sku: String(data.get('sku')),
        brand: String(data.get('brand')),
        productName: String(data.get('productName')),
        categoryCode: String(data.get('categoryCode')),
        productType: String(data.get('productType')),
        initialQuantity: Number(data.get('initialQuantity')),
        reorderLevel: Number(data.get('reorderLevel')),
        reason: String(data.get('reason')),
      })
      setIsNewInventoryOpen(false)
      setNotice('Warehouse item created. It remains separate from the customer catalogue.')
      await load('inventory')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'The warehouse item could not be created.') }
  }

  const filteredProducts = useMemo(() => products.filter((product) => `${product.brand} ${product.name} ${product.sku}`.toLowerCase().includes(query.toLowerCase())), [products, query])
  const filteredInventory = useMemo(() => inventory.filter((item) => `${item.brand} ${item.productName} ${item.sku}`.toLowerCase().includes(query.toLowerCase())), [inventory, query])

  if (loading && !user) return <div className="admin-gate"><span>JG MOLI / ADMIN</span><p>Opening the workspace…</p></div>
  if (!token || !user || user.role !== 'ADMIN') return (
    <div className="admin-gate">
      <span>JG MOLI / PRIVATE WORKSPACE</span>
      <h1>Admin access<br />required.</h1>
      <p>{error || 'Return to the store and log in with an authorised account.'}</p>
      <a href="#top">Return to JG MOLI →</a>
    </div>
  )

  return (
    <main className="admin-page">
      <aside className="admin-sidebar">
        <a className="admin-brand" href="#top"><b>JG</b><span>MOLI</span></a>
        <div className="admin-context"><small>Private workspace</small><strong>Operations</strong></div>
        <nav aria-label="Admin sections">
          {(['overview', 'orders', 'products', 'inventory'] as AdminView[]).map((item, index) => (
            <button key={item} className={view === item ? 'active' : ''} onClick={() => changeView(item)}><span>0{index + 1}</span>{item}</button>
          ))}
        </nav>
        <div className="admin-user"><small>Signed in</small><strong>{user.displayName}</strong><span>{user.email}</span><a href="#top">Return to store →</a></div>
      </aside>

      <section className="admin-workspace">
        <header className="admin-topbar"><div><span>JG MOLI / {view}</span><strong>{view === 'overview' ? 'What needs attention.' : view === 'orders' ? 'Every order, traceable.' : view === 'products' ? 'The gear customers see.' : 'Stock, without guesswork.'}</strong></div><time>{new Intl.DateTimeFormat('en-AU', { dateStyle: 'long' }).format(new Date())}</time></header>
        {error && <p className="admin-message error" role="alert">{error}</p>}
        {notice && <p className="admin-message">{notice}</p>}
        {loading && <div className="admin-loading">Updating workspace…</div>}

        {view === 'overview' && overview && <OverviewView data={overview} openOrder={(reference) => { setView('orders'); void openOrder(reference) }} />}

        {view === 'orders' && <>
          <form className="admin-tools" onSubmit={submitOrderSearch}><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Order, email, customer or SKU" aria-label="Search orders" /><select value={orderStatus} onChange={(event) => setOrderStatus(event.target.value)}><option value="">All order states</option><option>AWAITING_PAYMENT</option><option>PAID</option><option>UNFULFILLED</option><option>PROCESSING</option><option>SHIPPED</option><option>DELIVERED</option><option>CANCELLED</option></select><button>Search →</button></form>
          <OrderTable orders={orders} openOrder={openOrder} />
        </>}

        {view === 'products' && <><div className="admin-tools"><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search product or SKU" aria-label="Search products" /></div><ProductTable products={filteredProducts} select={setSelectedProduct} /></>}
        {view === 'inventory' && <><div className="admin-tools"><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search stock by product or SKU" aria-label="Search inventory" /><span>Melbourne Warehouse</span><button type="button" onClick={() => setIsNewInventoryOpen(true)}>Add warehouse item +</button></div><InventoryTable inventory={filteredInventory} select={setSelectedInventory} /><MovementList movements={movements} /></>}
      </section>

      {selectedOrder && <Drawer title={selectedOrder.summary.orderReference} eyebrow="Order detail" close={() => setSelectedOrder(null)}><OrderDrawer order={selectedOrder} submit={saveFulfilment} /></Drawer>}
      {selectedProduct && <Drawer title={selectedProduct.name} eyebrow={selectedProduct.sku} close={() => setSelectedProduct(null)}><ProductDrawer product={selectedProduct} submit={saveProduct} /></Drawer>}
      {selectedInventory && <Drawer title={selectedInventory.productName} eyebrow={selectedInventory.sku} close={() => setSelectedInventory(null)}><InventoryDrawer inventory={selectedInventory} submit={saveInventory} /></Drawer>}
      {isNewInventoryOpen && <Drawer title="Add warehouse item" eyebrow="Inventory / new SKU" close={() => setIsNewInventoryOpen(false)}><NewInventoryDrawer submit={createInventoryItem} /></Drawer>}
    </main>
  )
}

function deliveryLabel(status: string) {
  return ({ UNFULFILLED: 'Awaiting dispatch', PROCESSING: 'Preparing order', SHIPPED: 'Shipped', DELIVERED: 'Delivered', CANCELLED: 'Cancelled' } as Record<string, string>)[status] ?? status.replaceAll('_', ' ')
}

function OverviewView({ data, openOrder }: { data: AdminOverview, openOrder: (reference: string) => void }) {
  return <div className="admin-overview"><div className="admin-kpis"><article><span>Orders today</span><strong>{data.ordersToday}</strong></article><article><span>Paid today</span><strong>{money(data.paidRevenueTodayCents)}</strong></article><article><span>Awaiting payment</span><strong>{data.awaitingPayment}</strong></article><article><span>Ready to prepare</span><strong>{data.needsFulfilment}</strong></article><article className="attention"><span>At reorder level</span><strong>{data.lowStockProducts}</strong></article></div><section className="admin-section"><header><span>Recent orders</span><small>Newest first</small></header><OrderTable orders={data.recentOrders} openOrder={openOrder} /></section></div>
}

function OrderTable({ orders, openOrder }: { orders: AdminOrderSummary[], openOrder: (reference: string) => void }) {
  return <div className="admin-table"><div className="admin-row admin-row-head"><span>Order</span><span>Customer</span><span>Payment</span><span>Delivery progress</span><span>Total</span></div>{orders.map((order) => <button className="admin-row" key={order.id} onClick={() => openOrder(order.orderReference)}><span><strong>{order.orderReference}</strong><small>{date(order.createdAt)}</small></span><span><strong>{order.customerName}</strong><small>{order.customerEmail}</small></span><span><i className={`status ${order.status.toLowerCase()}`}>{order.status.replaceAll('_', ' ')}</i></span><span>{deliveryLabel(order.fulfillmentStatus)}</span><span><strong>{money(order.totalCents, order.currency)}</strong><small>{order.itemCount} item{order.itemCount === 1 ? '' : 's'} →</small></span></button>)}{orders.length === 0 && <p className="admin-empty">No orders match this view.</p>}</div>
}

function ProductTable({ products, select }: { products: AdminProduct[], select: (product: AdminProduct) => void }) {
  return <div className="admin-table"><div className="admin-row products admin-row-head"><span>Product</span><span>Category</span><span>Status</span><span>Price inc. GST</span></div>{products.map((product) => <button className="admin-row products" key={product.variantId} onClick={() => select(product)}><span><strong>{product.brand} · {product.name}</strong><small>{product.sku}</small></span><span>{product.category}<small>{product.subtype}</small></span><span><i className={`status ${product.active && product.status === 'ACTIVE' ? 'paid' : 'awaiting_payment'}`}>{product.active ? product.status : 'HIDDEN'}</i></span><span><strong>{money(product.priceCents, product.currency)}</strong><small>Edit →</small></span></button>)}</div>
}

function InventoryTable({ inventory, select }: { inventory: AdminInventory[], select: (item: AdminInventory) => void }) {
  return <div className="admin-table"><div className="admin-row inventory admin-row-head"><span>Warehouse item</span><span>On hand</span><span>Reserved</span><span>Available</span><span>Reorder</span></div>{inventory.map((item) => <button className="admin-row inventory" key={item.itemId} onClick={() => select(item)}><span><strong>{item.brand} · {item.productName}</strong><small>{item.sku} · {item.productType}{item.linkedToCatalog ? ' · Catalog linked' : ' · Warehouse only'}</small></span><span>{item.onHand}</span><span>{item.reserved}</span><span><strong>{item.available}</strong></span><span className={item.reorderLevel > 0 && item.available <= item.reorderLevel ? 'low-stock' : ''}>{item.reorderLevel}<small>Adjust →</small></span></button>)}</div>
}

function MovementList({ movements }: { movements: AdminMovement[] }) {
  return <section className="admin-section movements"><header><span>Movement history</span><small>Append-only record</small></header>{movements.length === 0 ? <p className="admin-empty">No stock movements yet.</p> : movements.slice(0, 8).map((movement) => <div className="movement" key={movement.id}><i>{movement.movementType}</i><strong>{movement.sku}</strong><span>{movement.onHandDelta > 0 ? '+' : ''}{movement.onHandDelta}</span><p>{movement.reason}</p><small>{movement.performedBy} · {date(movement.createdAt)}</small></div>)}</section>
}

function Drawer({ title, eyebrow, close, children }: { title: string, eyebrow: string, close: () => void, children: ReactNode }) {
  return <div className="admin-drawer-wrap"><button className="admin-drawer-backdrop" onClick={close} aria-label="Close detail" /><aside className="admin-drawer"><header><div><span>{eyebrow}</span><h2>{title}</h2></div><button onClick={close} aria-label="Close detail">×</button></header>{children}</aside></div>
}

function OrderDrawer({ order, submit }: { order: AdminOrderDetail, submit: (event: FormEvent<HTMLFormElement>) => void }) {
  const nextStatuses = order.summary.fulfillmentStatus === 'UNFULFILLED' ? ['UNFULFILLED', 'PROCESSING', 'CANCELLED'] : order.summary.fulfillmentStatus === 'PROCESSING' ? ['PROCESSING', 'SHIPPED', 'CANCELLED'] : order.summary.fulfillmentStatus === 'SHIPPED' ? ['SHIPPED', 'DELIVERED'] : [order.summary.fulfillmentStatus]
  return <div className="drawer-content"><section className="drawer-summary"><div><span>Payment</span><strong>{order.summary.status}</strong><small>{order.paymentReference || 'No completed payment'}</small></div><div><span>Invoice</span><strong>{order.invoiceNumber || 'Not issued'}</strong></div><div><span>Delivery</span><strong>{order.summary.customerName}</strong><small>{order.addressLine1}{order.addressLine2 ? `, ${order.addressLine2}` : ''}<br />{order.suburb} {order.state} {order.postcode}<br />{order.phone}</small></div></section><section className="drawer-lines">{order.items.map((item) => <div key={item.sku}><span>{item.quantity} ×</span><p><strong>{item.brand} {item.productName}</strong><small>{item.sku}</small></p><b>{money(item.lineTotalCents, order.summary.currency)}</b></div>)}<footer><span>Total including GST</span><strong>{money(order.summary.totalCents, order.summary.currency)}</strong></footer></section><form className="admin-form" onSubmit={submit}><h3>Delivery progress</h3><label>Status<select name="status" defaultValue={order.summary.fulfillmentStatus}>{nextStatuses.map((status) => <option key={status} value={status}>{deliveryLabel(status)}</option>)}</select></label><div><label>Carrier<input name="carrier" defaultValue={order.carrier ?? ''} /></label><label>Tracking number<input name="trackingNumber" defaultValue={order.trackingNumber ?? ''} /></label></div><button>Save delivery progress →</button></form></div>
}

function ProductDrawer({ product, submit }: { product: AdminProduct, submit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <form className="admin-form drawer-content" onSubmit={submit}><p className="drawer-intro">Change only what customers should see. Historical orders keep their original price snapshot.</p><label>Price including GST (AUD)<input name="price" type="number" min="0" step="0.01" defaultValue={(product.priceCents / 100).toFixed(2)} required /></label><label>Product status<select name="status" defaultValue={product.status}><option>ACTIVE</option><option>DRAFT</option><option>ARCHIVED</option></select></label><label className="admin-check"><input name="active" type="checkbox" defaultChecked={product.active} /> Allow checkout for this SKU</label><p className="field-help">Turn this off to pause this specific SKU without archiving the whole product.</p><button>Save product →</button></form>
}

function InventoryDrawer({ inventory, submit }: { inventory: AdminInventory, submit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <form className="admin-form drawer-content" onSubmit={submit}><div className="stock-balance"><div><span>On hand</span><strong>{inventory.onHand}</strong></div><div><span>Reserved</span><strong>{inventory.reserved}</strong></div><div><span>Available</span><strong>{inventory.available}</strong></div></div><p className="drawer-intro">Available stock is calculated from on-hand minus reserved. Every change creates a permanent movement record.</p><label>Movement<select name="movementType"><option value="RECEIPT">Receive stock</option><option value="ADJUSTMENT">Adjustment</option></select></label><div><label>Quantity change<input name="quantityDelta" type="number" defaultValue="1" required /></label><label>Reorder level<input name="reorderLevel" type="number" min="0" defaultValue={inventory.reorderLevel} required /></label></div><label>Reason<input name="reason" maxLength={240} placeholder="Delivery note or reason for adjustment" required /></label><button>Record movement →</button></form>
}

function NewInventoryDrawer({ submit }: { submit: (event: FormEvent<HTMLFormElement>) => void }) {
  return <form className="admin-form drawer-content" onSubmit={submit}><p className="drawer-intro">Create one SKU across Products and Inventory. The new product begins as a draft with checkout disabled until its price and storefront details are ready.</p><div><label>SKU<input name="sku" placeholder="e.g. LOGI-GPRO-WHEEL" required /></label><label>Store category<select name="categoryCode" defaultValue=""><option value="" disabled>Choose category</option><option value="displays">Displays</option><option value="controls">Controls</option><option value="audio">Audio</option><option value="sim">Sim Gear</option><option value="furniture">Furniture</option></select></label></div><div><label>Brand<input name="brand" required /></label><label>Product name<input name="productName" required /></label></div><label>Product type<input name="productType" placeholder="e.g. Racing wheels" required /></label><div><label>Opening stock<input name="initialQuantity" type="number" min="0" defaultValue="0" required /></label><label>Reorder level<input name="reorderLevel" type="number" min="0" defaultValue="0" required /></label></div><label>Opening balance note<input name="reason" maxLength={240} placeholder="Stock count, delivery note or supplier reference" required /></label><button>Create product + warehouse item →</button></form>
}
