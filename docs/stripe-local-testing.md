# Stripe local test payments

The integration supports explicit `test` and `live` configurations. Real-money payments remain disabled by default and are not launch-verified. Live mode requires both `JGMOLI_STRIPE_MODE=live` and the independent `JGMOLI_STRIPE_ALLOW_LIVE_PAYMENTS=true` switch, matching credentials and an HTTPS return URL. Inventory and final pricing remain a separate launch task.

## Local configuration

Use `backend/src/main/resources/application-local-private.properties` (ignored by Git and excluded from the JAR):

```properties
jgmoli.stripe.test-enabled=true
jgmoli.stripe.secret-key=YOUR_STRIPE_TEST_SECRET_KEY
jgmoli.stripe.webhook-secret=
jgmoli.stripe.return-url=http://localhost:5175/
jgmoli.email.dispatch-enabled=false
```

From the repository root, start the listener with `node scripts/stripe-listen.mjs`. It reads the test key without printing it, saves the local listener's signing secret, and forwards only Checkout events to port 8080. Keep the listener running. Restart the backend after the listener writes its secret.

Start the backend from `backend/` with `mvn spring-boot:run`. Start the frontend from `frontend/` with `npm run dev -- --port 5173 --strictPort`. Set the private return URL to `http://localhost:5173/` to match. The frontend reads payment availability from the authenticated backend configuration endpoint in both development and production builds. The old `VITE_STRIPE_TEST_ENABLED` flag is no longer used. Frontend environment files must never contain Stripe secret keys.

For cloud configuration use `JGMOLI_STRIPE_ENABLED`, `JGMOLI_STRIPE_MODE`, `JGMOLI_STRIPE_SECRET_KEY`, `JGMOLI_STRIPE_WEBHOOK_SECRET` and `JGMOLI_STRIPE_RETURN_URL`. Production test Checkout is restricted to administrators by default through `JGMOLI_STRIPE_TEST_ADMIN_ONLY=true`. Test/live credential mismatches and cross-environment reuse of an order are rejected. An isolated cloud test schema and frontend preview should be used before enabling any payment capability on the main site.

Use synthetic customer details and Stripe test cards only. A successful return URL never settles an order; the browser reads the authenticated server order after webhook processing. Cancelled Checkout can be resumed from the return notice. A card decline keeps the Session open for retry. An expired Session is marked cancelled, allowing a new attempt; an unresolved attempt older than 23 hours is blocked for review to avoid reusing an expired provider idempotency key.

## Verification boundaries

The automated suite covers invalid signatures/amounts, disabled/live-key rejection, unpaid/failed events and duplicate successful delivery. Manual Stripe sandbox validation must additionally cover hosted Checkout, decline, success, expiry, invoice download and return/reload. Test payments do not verify live settlement, warehouse availability, delivery pricing or mailbox delivery.

Keep automatic email dispatch disabled for synthetic tests. For deliberate mailbox testing with a real recipient, configure `jgmoli.email.provider=resend` and `jgmoli.email.resend.api-key` in the private local file, suppress synthetic outbox rows, then enable `jgmoli.email.dispatch-enabled=true` and restart the backend. IntelliJ-only environment variables are not inherited by a backend launched from Maven in another terminal. An outbox status of `ACCEPTED` proves provider acceptance, not delivery to an inbox.

A future production integration must deliberately handle refund/reconciliation and fulfillment before public checkout is enabled.
