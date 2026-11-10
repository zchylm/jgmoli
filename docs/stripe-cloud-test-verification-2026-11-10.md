# Cloud Stripe sandbox verification — 2026-11-10

## Deployment boundaries

- Main site: `https://jgmoli.com.au/`; backend revision `jgmoli-backend--0000014`. Stripe payments and the independent live-payment switch remain disabled. Production demo payments are disabled.
- Payment preview: `https://jgmoli.com.au/payment-preview/`; separate backend `jgmoli-payment-test` and PostgreSQL schema `jgmoli_payment_test`. Checkout uses Stripe test credentials and is restricted to administrators.
- Preview authentication/cart storage uses its own prefix, so testing does not overwrite the main site's session or cart.
- Backend image: `jgmoliacr.azurecr.io/jgmoli-backend:payment-20261110-2`. Both application revisions were healthy with 100% traffic after deployment.
- Frontend deployment for commit `44fc5c9` completed successfully: <https://github.com/zchylm/jgmoli/actions/runs/42731984552>.

## Observed cloud flow

- Authenticated API created isolated synthetic order `JGM-20261110-001001` for AUD 299. Repeated Checkout creation returned the same Session.
- Browser cancel-return dialog resumed that order's hosted Checkout. Stripe visibly displayed **Sandbox**.
- Stripe's success test card completed Checkout and returned to the preview. The application displayed its payment confirmation dialog; desktop and 390px mobile layouts were reviewed.
- Actual Stripe event `evt_1Ua3PVQR61j783MU7X1PF0LU` had `livemode=false` and no pending webhooks when inspected. The cloud order became `PAID`; invoice `JGM-INV-2026-001001` became `ISSUED` and opened in the browser.
- Replaying the signed event returned HTTP 200. A query of the isolated schema confirmed exactly one successful payment and one invoice for that order.
- Reloading the success-return page recovered the confirmed order from the backend.
- Both backends reported `UP`. Anonymous payment configuration reported `enabled=false`, and unauthenticated Checkout requests returned 401.
- Latest local backend suite: 35 tests, zero failures/errors/skips. Frontend lint, production build and Git whitespace checks passed.

## Cloud email

- Synthetic recipient messages were explicitly suppressed before enabling dispatch in the isolated test backend.
- A second hosted sandbox payment completed for `JGM-20261110-001002`, using the recipient from the user's earlier local email test. Its invoice `JGM-INV-2026-001002` was issued.
- The cloud invoice email reached outbox status `ACCEPTED` on its first attempt, with Resend message ID `01a1c2fd-61cd-7cdd-8908-f5b65947d826`. This proves provider acceptance; inbox delivery has not been independently verified.
- Test backend revision `jgmoli-payment-test--0000004` was healthy with 100% traffic after mail configuration.
- Preview administrator access is in the ignored, owner-readable local file `backend/.env.payment-preview.local`; the credentials are specific to the isolated preview.

## Remaining launch verification

This is an online sandbox deployment, not approval to collect real payments. Live credentials/webhook, a controlled real payment and refund, final prices, physical inventory, delivery charges and fulfilment remain launch tasks. Sandbox success does not establish live settlement or refund handling.
