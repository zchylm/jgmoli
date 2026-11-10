# Local Stripe sandbox verification — 2026-11-10

Scope: local PostgreSQL `jgmoli_db`, local backend on 8080, temporary test frontend on 5175, Stripe test API key and Stripe CLI forwarding. No deployment or real-money transaction. Synthetic customer/address/card data only; outbound application email dispatch disabled.

Observed using the actual Stripe sandbox:

- Order `JGM-20261110-001068`: Checkout created by the backend; a repeated create request returned the same Session.
- Hosted Checkout visibly displayed Sandbox and AUD 299.00.
- Stripe decline test card was rejected; authenticated order remained `AWAITING_PAYMENT`.
- Successful test card completed hosted Checkout. Actual event `evt_1Ua2SIQR61j783MUWxUNNZfG` reached the local webhook and returned HTTP 200.
- Order became `PAID`; invoice `JGM-INV-2026-001061` became `ISSUED`; browser returned to the application and displayed server-confirmed payment.
- The actual signed event was replayed to the handler, returning 200. Database query confirmed exactly one successful payment and one invoice.
- Order `JGM-20261110-001071`: Session expired through the Stripe test API. Actual expiry webhook returned 200; retry created a different Session. Order remained unpaid with no invoice.
- Cancel-return page checked the server and provided Continue payment; clicking it opened that order's hosted sandbox Checkout again.

Automated backend suite, frontend lint and production build passed after the payment changes. The browser test establishes the hosted payment/return portion; the initial synthetic order for this verification was created through the application's authenticated API.

Not established: live card processing/settlement, production webhooks, real mailbox delivery, refund handling, warehouse inventory, final delivery fees, exhaustive network/concurrency fault injection, or complete site purchase regression. These remain launch gates; the production payment configuration remains disabled.
