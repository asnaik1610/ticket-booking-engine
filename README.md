# Ticket Booking Engine

A high-concurrency booking engine that prevents double-booking under heavy contention, pushes real-time seat updates to the UI, and exposes operational metrics for observability.

## Why this exists (the real-world problem)
Ticketing platforms face flash-crowd traffic where many users attempt to book the same seat at the same time. The hard part is guaranteeing **single-winner booking** while keeping latency low and the UI current. This project demonstrates a clean, production-style approach to consistency, concurrency, and real-time updates.

## Core guarantees
- **No double booking**: once a seat is claimed, all concurrent attempts fail with a conflict.
- **Authoritative source of truth**: PostgreSQL stores the final seat state.
- **Real-time convergence**: all clients see the same seat state via WebSocket broadcasts.

## How we enforce consistency
We provide two interchangeable strategies:
- **Database locking**: a `PESSIMISTIC_WRITE` row lock serializes writers at the DB layer.
- **Distributed locking (Redis/Redisson)**: a fast-fail distributed lock coordinates across multiple app instances.

The Strategy pattern keeps business logic stable while letting us switch locking modes per deployment.

## Tech stack
- **Java 21 + Spring Boot 3.3.x**
- **PostgreSQL** (source of truth)
- **Redis + Redisson** (distributed locks)
- **WebSocket + STOMP** (real-time seat updates)
- **Prometheus + Grafana** (metrics/observability)
- **React 19 + Tailwind** (frontend in `/frontend`)

## Core flow (booking)
1. Client calls `POST /api/v1/bookings` with seat ID + user ID.
2. Service chooses a locking strategy.
3. Seat is locked (DB row lock or Redis distributed lock).
4. If already booked, a 409 is returned.
5. Booking is persisted to PostgreSQL.
6. Updated seat status is broadcast to `/topic/seats`.

## APIs
- `POST /api/v1/bookings`  
  Request: `{ "seatId": 1, "userId": "u-123", "strategy": "REDIS" }`
- `GET /api/v1/seats`  
  Returns the current seat inventory for UI hydration.

## Real-time updates
The backend broadcasts seat state changes to `/topic/seats` via STOMP over WebSocket.  
The React UI subscribes and updates seat colors immediately (green = available, red = booked).

## Observability
- `GET /actuator/health` (liveness/readiness)
- `GET /actuator/prometheus` (Prometheus scrape endpoint)
- Custom metric: `booking.conflict.total`

## Local Development
1. Start infrastructure:
   ```bash
   docker compose up -d
   ```
2. Run the backend:
   ```bash
   mvn spring-boot:run
   ```
3. Run the frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Local Troubleshooting
- **Java version mismatch**: Ensure Java 21 is configured in IntelliJ and Maven.
- **DB connection refused**: Postgres must be running on `localhost:5432` (via Docker or local install).
- **WebSocket connection fails**: `VITE_WS_URL` should be `ws://localhost:8080/ws`.
- **Prometheus shows no data**: Verify `/actuator/prometheus` is reachable from the Prometheus container.

## Known Limitations / Future Work
- No payment workflow; booking is a single-transaction flow.
- Seat inventory seeding is manual (a migration/bootstrapper would be better).
- No rate limiting or abuse prevention on booking endpoints.
- UI assumes a fixed 10x10 grid; should be driven by API metadata.


