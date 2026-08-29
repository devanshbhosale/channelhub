# ChannelHub

A reseller order and credit portal for IT distribution — the working counterpart of what a
platform like Ingram Micro Xvantage offers channel partners: catalog, ordering against a
credit limit, order tracking, and distributor-side fulfilment and analytics.

One Spring Boot fat JAR serves the React SPA and the REST API from the same artifact
(same origin, no CORS).

## Stack

- Backend: Java 21, Spring Boot 3.5.x, Spring Data JPA (Hibernate), Spring Security (JWT), JUnit 5, JaCoCo
- Frontend: React 18 + Vite, custom CSS design system, Chart.js
- Data: PostgreSQL (Neon) in production; H2 in PostgreSQL-compat mode locally and in tests

## Run locally

1. Build the frontend (outputs into the backend's static resources):
   `cd frontend && npm install && npm run build`
2. Run the backend from the repo root:
   `./mvnw spring-boot:run`
3. Open http://localhost:8080 — API docs at /swagger-ui.html.

## Deployment

Single Render free web service (Docker), Neon free PostgreSQL, keep-alive pings to
/actuator/health during IST business hours (see render.yaml header comment).

## Status

Backend and frontend complete: domain, JWT auth (with read-only demo admin), order engine
with credit and stock rules, GST invoice PDF, dashboards, and all portal screens.
Test suite: 22/22 green (`./mvnw test`). Next: deployment (Render + Neon) and pitch kit.
