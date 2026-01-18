# Ticket Booking Engine

A high-concurrency booking engine demonstrating database and Redis locking strategies with live WebSocket updates and Prometheus-ready metrics.

## Tech Stack
- Java 25 + Spring Boot 4
- PostgreSQL + Redis
- WebSocket (STOMP)
- Prometheus + Grafana
- React 19 + Tailwind (frontend in `/frontend`)

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
- **Java version mismatch**: If Java 25 is not available locally, align `pom.xml` with your JDK (e.g., set `<java.version>21</java.version>` and a compatible Spring Boot version).
- **Prometheus shows no data**: Ensure the app is running on port 8080 and `/actuator/prometheus` is reachable from the Prometheus container.
- **WebSocket connection fails**: Verify `VITE_WS_URL` points to `ws://localhost:8080/ws` and that `/ws` is exposed by the backend.
- **Booking conflicts on startup**: Seed the database with clean seat data or truncate the `seats` table between runs.

## Known Limitations / Future Work
- No partial payment handling; this assumes a single-transaction checkout flow.
- Seat inventory seeding is manual; consider a bootstrapper or migration-based fixture.
- No backpressure or rate limiting on booking endpoints.
- UI currently assumes a 10x10 seat grid; should be driven by API metadata.


