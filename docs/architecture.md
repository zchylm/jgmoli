# Architecture

## Current foundation

The repository starts with the smallest complete vertical slice:

1. The React application requests `/api/health`.
2. Vite proxies the request to Spring Boot during local development.
3. Spring Boot returns a stable service-status contract.

This proves the frontend/backend boundary without inventing product entities before the product direction is approved.

## Frontend boundaries

- `app/` owns application-wide composition and design foundations.
- `pages/` owns route-level experiences.
- `services/` owns API access.
- Future customer capabilities belong in `features/`, grouped by experience such as `experience-composer`, `space-fit`, and `private-demo`.

Shared visual values live in design tokens. Page-specific styling should stay with the page or feature that owns it.

## Backend boundaries

- `shared/` contains cross-cutting contracts such as service health.
- Future business areas should be top-level packages such as `experience`, `product`, and `enquiry`.
- Each business area may contain its own controller, service, domain model, and repository as those layers become necessary.

Database, authentication, payments, and commerce are deliberately deferred until a real use case requires them.

## API convention

- Public application APIs use the `/api` prefix.
- Responses are JSON.
- Environment-specific URLs stay in configuration, never in components.

