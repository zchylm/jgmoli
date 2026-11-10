# JG MOLI

Clean foundation for the JG MOLI gaming-setup experience.

## Structure

- `frontend/` — React, Vite and TypeScript
- `backend/` — Spring Boot REST API
- `docs/` — product and architecture decisions

The previous static prototype and supplier material remain outside this repository as research references. They are not source code or approved product data.

## Run locally

Requirements:

- Node.js 22+
- Java 17+
- Maven 3.9+

Start the backend:

```bash
cd backend
export JGMOLI_CLAUDE_API_KEY='your-Claude-API-key'
mvn spring-boot:run
```

`JGMOLI_CLAUDE_API_KEY` is read only by the backend and must never be added to the frontend or committed to source control. The optional `JGMOLI_CLAUDE_MODEL` variable defaults to `claude-sonnet-5`.

Local secrets can also be configured in `backend/src/main/resources/application-local-private.properties`, matching JON.PC's local Spring configuration. This file is ignored by Git, excluded from the packaged JAR, and loaded only by the local profile. A blank example is provided alongside it. Stripe test payments require `jgmoli.stripe.test-enabled=true`, a test secret key and a webhook signing secret. No macOS Keychain setup is required. Use a fresh checkout without local secrets for deployment builds.

Start the frontend in another terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` to the backend on port `8080`.

## Verify

```bash
cd frontend
npm run lint
npm run build

cd ../backend
mvn test
```

## Working principles

- Build around customer experiences, not an unverified product catalogue.
- Keep product facts and commercial claims out of the UI until approved.
- Add only reviewed assets with a known source and usage status.
- Keep feature boundaries explicit; avoid global CSS patches and version-specific overrides.
