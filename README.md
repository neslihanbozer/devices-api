# Devices API

A RESTful API for managing device resources, built with Java 21, Spring Boot, and PostgreSQL.

## Quick Start

### Prerequisites

- Docker Desktop (Windows/macOS) or Docker Engine with Docker Compose (Linux)
- Git

You do not need to install Java, Gradle, or PostgreSQL to run the application: the API is compiled inside Docker.

**Start Docker Desktop before running the startup script or the tests**, and wait until it reports that it is running.

### 1. Clone the repository

```bash
git clone https://github.com/neslihanbozer/devices-api.git
cd devices-api
```

### 2. Start the application

**macOS / Linux:**

```bash
bash start.sh
```

**Windows (PowerShell):**

```powershell
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

On first run, the startup script generates random passwords for the database and the two API users and stores them in a local `.env` file (ignored by Git). It then builds and starts the containers and **prints the API credentials**:

```
API credentials (HTTP Basic, also used in Swagger UI 'Authorize'):
  ADMIN  (full access): admin / 671018a2...
  VIEWER (read-only)  : viewer / ecc62da0...
```

The credentials are also in `.env` (`API_ADMIN_PASSWORD`, `API_VIEWER_PASSWORD`) if you need them later.

> **No shared or default credentials.** The repository contains no passwords. Every environment generates its own on first start, so nobody needs to send you a password. Use the values printed on your machine in Swagger UI, Postman, or curl.

<details>
<summary>Manual start without the scripts</summary>

Copy the template and fill in the passwords:

```bash
cp .env.example .env      # Windows: copy .env.example .env
# edit .env and set DB_PASSWORD, API_ADMIN_PASSWORD, API_VIEWER_PASSWORD
docker compose up --build -d
```

</details>

The API is ready when `docker compose ps` shows the `api` container as `healthy` (usually 20–40 seconds after the first build).

### 3. Access the API

| Resource | URL | Authentication |
|---|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html | Public |
| OpenAPI JSON | http://localhost:8080/v3/api-docs | Public |
| Health Check | http://localhost:8080/actuator/health | Public |
| Devices API | http://localhost:8080/api/devices | HTTP Basic |

## Authentication and Authorization

The API uses **HTTP Basic** authentication with two roles:

| User (default name) | Role | Allowed |
|---|---|---|
| `admin` | `ADMIN` | All operations: create, read, update, delete |
| `viewer` | `VIEWER` | Read only: `GET /api/devices`, `GET /api/devices/{id}` |

| Situation | Response |
|---|---|
| No or invalid credentials | `401 Unauthorized` |
| Valid credentials, but the role is not allowed (e.g. `viewer` sends `DELETE`) | `403 Forbidden` |

- Swagger UI, the OpenAPI document and the health check are public. Every other endpoint is denied.
- Passwords are required and have no default. The startup scripts generate them; the application refuses to start without them.
- Passwords are kept BCrypt-hashed in memory. The API is stateless: no sessions or cookies.

Example:

```bash
curl -u admin:<API_ADMIN_PASSWORD> http://localhost:8080/api/devices
```

### 4. Stop the application

```bash
docker compose down
```

Database data is preserved between restarts in a Docker volume. To start again, run the startup script again.

## Using Swagger UI

The API is documented with OpenAPI 3, and Swagger UI lets you try every endpoint from the browser.

To call an endpoint:

1. Open http://localhost:8080/swagger-ui/index.html. The page itself needs no login.
2. Click **Authorize** (top right), enter the username and password printed by the startup script (for example `admin` and the `API_ADMIN_PASSWORD` value), then click **Authorize** and **Close**.
3. Expand the **Devices** section and select an operation, for example `POST /api/devices`.
4. Click **Try it out**.
5. Edit the example request body or fill in the parameters, then click **Execute**.
6. Swagger shows the response body, status code, and an equivalent `curl` command below the request.

To see the role restrictions, click **Authorize → Logout**, log in as `viewer`, and try a `POST` or `DELETE`: the API returns `403`. Without logging in, every device endpoint returns `401`.

A typical flow:

1. `POST /api/devices` with `{"name": "iPhone 16", "brand": "Apple", "state": "AVAILABLE"}`. Copy the `id` from the response.
2. `GET /api/devices/{id}`: paste the `id` to fetch the device.
3. `PATCH /api/devices/{id}` with `{"state": "IN_USE"}`.
4. `PATCH /api/devices/{id}` with `{"name": "New name"}` returns **409**, because the device is in use.
5. `DELETE /api/devices/{id}` returns **409** for the same reason. Set the state back to `AVAILABLE`, and the delete returns **204**.

The **Schemas** section at the bottom of the page describes every request and response model.

## Postman Collection

`postman/Devices-API.postman_collection.json` contains 32 requests with automated checks. It covers every endpoint, the business rules (409 cases), validation errors, and authentication/authorization (401/403).

**In Postman:**

1. Click **Import** and select `postman/Devices-API.postman_collection.json`.
2. Start the application.
3. Open the collection, go to the **Variables** tab, and set `adminPassword` and `viewerPassword` to the values printed by the startup script (or from `.env`). Click **Save**.
4. Right-click the collection and select **Run collection**, then click **Run**. All checks should pass.

Requests use the `admin` user by default (collection-level Basic auth). The **Security** folder uses the `viewer` user or no credentials on purpose.

Run the requests in order. `Create device` stores the new `deviceId` and a unique `brand` in collection variables, and the following requests use them. You can also send single requests by hand after running `Create device` once.

**From the command line** (requires Node.js):

```bash
npx newman run postman/Devices-API.postman_collection.json \
  --env-var adminPassword=<API_ADMIN_PASSWORD> \
  --env-var viewerPassword=<API_VIEWER_PASSWORD>
```

**Alternative:** Postman can generate requests from the OpenAPI spec. Click **Import** and paste `http://localhost:8080/v3/api-docs`. Postman creates the requests without the checks; set Basic auth on the generated collection.

## API Overview

Base path: `/api/devices`

| Method | Path | Description | Role | Success | Errors |
|---|---|---|---|---|---|
| `POST` | `/api/devices` | Create a device | `ADMIN` | `201` | `400`, `401`, `403` |
| `GET` | `/api/devices/{id}` | Fetch a single device | `VIEWER`, `ADMIN` | `200` | `400`, `401`, `404` |
| `GET` | `/api/devices` | Fetch all devices (optional `brand` / `state` filters) | `VIEWER`, `ADMIN` | `200` | `400`, `401` |
| `PUT` | `/api/devices/{id}` | Fully update a device | `ADMIN` | `200` | `400`, `401`, `403`, `404`, `409` |
| `PATCH` | `/api/devices/{id}` | Partially update a device | `ADMIN` | `200` | `400`, `401`, `403`, `404`, `409` |
| `DELETE` | `/api/devices/{id}` | Delete a device | `ADMIN` | `204` | `401`, `403`, `404`, `409` |

### Device

| Field | Description |
|---|---|
| `id` | Server-generated UUID |
| `name` | Device name (required, max 255 characters) |
| `brand` | Device brand (required, max 255 characters) |
| `state` | `AVAILABLE`, `IN_USE`, or `INACTIVE` |
| `createdAt` | Creation timestamp (UTC), set by the server and immutable |

### Listing and filtering

`GET /api/devices` returns all matching devices as a JSON array.

| Parameter | Notes |
|---|---|
| `brand` | Exact, case-sensitive match. A blank value is ignored. |
| `state` | `AVAILABLE`, `IN_USE`, or `INACTIVE` |

Both parameters are optional and can be combined.

### PUT vs PATCH

- `PUT` replaces all mutable fields. `name`, `brand`, and `state` are all required.
- `PATCH` updates only the fields present in the body. Explicit `null` values and unknown fields are rejected with `400`.

### Examples (curl)

```bash
ADMIN="admin:<API_ADMIN_PASSWORD>"

# Create
curl -u "$ADMIN" -X POST http://localhost:8080/api/devices \
  -H "Content-Type: application/json" \
  -d '{"name": "iPhone 16", "brand": "Apple", "state": "AVAILABLE"}'

# List available Apple devices
curl -u "$ADMIN" "http://localhost:8080/api/devices?brand=Apple&state=AVAILABLE"

# Partially update
curl -u "$ADMIN" -X PATCH http://localhost:8080/api/devices/{id} \
  -H "Content-Type: application/json" \
  -d '{"state": "IN_USE"}'

# Delete
curl -u "$ADMIN" -X DELETE http://localhost:8080/api/devices/{id}
```

### Error format

All errors use [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`):

```json
{
  "status": 409,
  "title": "Business rule violation",
  "detail": "Cannot change name or brand while device is IN_USE",
  "instance": "/api/devices/3f6c…"
}
```

Validation errors also include the invalid fields:

```json
{
  "status": 400,
  "title": "Validation failed",
  "detail": "One or more fields are invalid.",
  "instance": "/api/devices",
  "errors": { "name": "must not be blank", "state": "must not be null" }
}
```

## Business Rules

- Creation time cannot be modified.
- Name and brand cannot be updated while a device is `IN_USE`. The rule is evaluated against the state *before* the update, so releasing and renaming a device in the same request is rejected.
- Devices in the `IN_USE` state cannot be deleted.
- Concurrent modifications are detected with optimistic locking and return `409 Conflict`.

The rules are implemented in the `Device` entity (`update`, `ensureDeletable`), so every write path enforces them the same way.

## Architecture

```
controller  →  service  →  repository  →  PostgreSQL
   (DTOs)      (transactions)   (Spring Data JPA)
                 ↓
               domain (Device entity + business rules)
```

| Package | Responsibility |
|---|---|
| `controller` | HTTP endpoints, request validation, OpenAPI annotations |
| `service` | Transaction boundaries, orchestration |
| `domain` | `Device` entity and its business rules |
| `repository` | Spring Data JPA queries |
| `dto` | Request/response records (entities are never exposed) |
| `common.exception` | Domain exceptions and the global Problem Details handler |
| `config` | Security (HTTP Basic, roles) and OpenAPI configuration |

## Technology Stack

- Java 21, Spring Boot 4
- Spring Security (HTTP Basic, role-based authorization)
- Spring Data JPA / Hibernate, PostgreSQL 17, Flyway migrations
- Springdoc OpenAPI / Swagger UI
- JUnit 5, Mockito, MockMvc, Testcontainers, JaCoCo
- Docker, Docker Compose, GitHub Actions

## Database

- Flyway manages the schema (`src/main/resources/db/migration`). Hibernate only validates it (`ddl-auto: validate`).
- Indexes exist on `brand` and `state` for the filter queries.
- A database `CHECK` constraint enforces valid states.
- Data is persisted in the `postgres_data` Docker volume.

## Testing

Integration tests use Testcontainers, so Docker must be running.

```bash
./gradlew clean build        # macOS / Linux
.\gradlew.bat clean build    # Windows
```

`build` runs all tests, generates the coverage report, and fails if line coverage drops below 90%.

- Coverage report: `build/reports/jacoco/test/html/index.html`
- Test types:
  - **Unit tests:** domain rules and services with a mocked repository.
  - **Web slice tests:** `@WebMvcTest` for error mapping.
  - **Integration tests:** full HTTP → PostgreSQL. All integration tests share one PostgreSQL container and one Spring context (see `AbstractIntegrationTest`).
  - **Security tests:** `SecurityIntegrationTest` checks 401/403 and both roles with real HTTP Basic credentials.

## Running the API from Gradle / IDE (development)

Requires Java 21. Start a standalone PostgreSQL container, then run the API against it:

```bash
docker run -d --name devices-db -p 5432:5432 \
  -e POSTGRES_DB=devices_db -e POSTGRES_USER=devices_user -e POSTGRES_PASSWORD=localpass \
  postgres:17

DB_PASSWORD=localpass API_ADMIN_PASSWORD=admin123 API_VIEWER_PASSWORD=viewer123 ./gradlew bootRun
```

On Windows PowerShell, set the variables first (`$env:DB_PASSWORD="localpass"`, `$env:API_ADMIN_PASSWORD="admin123"`, `$env:API_VIEWER_PASSWORD="viewer123"`), then run `.\gradlew.bat bootRun`.

## Configuration

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/devices_db` | JDBC URL |
| `DB_USERNAME` | `devices_user` | Database user |
| `DB_PASSWORD` | — (required) | Database password |
| `API_ADMIN_USERNAME` | `admin` | Username of the `ADMIN` user |
| `API_ADMIN_PASSWORD` | — (required) | Password of the `ADMIN` user |
| `API_VIEWER_USERNAME` | `viewer` | Username of the `VIEWER` user |
| `API_VIEWER_PASSWORD` | — (required) | Password of the `VIEWER` user |

The startup scripts generate all required passwords into `.env`.

## Known Limitations and Future Improvements

- **Pagination:** `GET /api/devices` returns all matching devices; add paging and sorting as data grows.
- **Authentication:** replace HTTP Basic with a dedicated identity provider (e.g. Keycloak, Auth0, Azure AD). HTTPS is required in production. Basic auth verifies the BCrypt password hash on every request, which adds noticeable latency per call; token-based authentication removes this cost.
- **Authorization:** finer-grained roles and per-record permissions, plus brute-force protection (rate limiting).
- **Observability:** add a professional metrics, tracing and error-monitoring tool (e.g. Prometheus + Grafana, OpenTelemetry, Sentry, Datadog).
- **Concurrency:** client-side optimistic concurrency control to prevent lost updates (ETag / If-Match).
- **Search:** case-insensitive brand filtering.
- **Domain:** state-transition rules, soft delete and change history.

Detailed assumptions and design decisions are in [ASSUMPTIONS.md](ASSUMPTIONS.md).

## Troubleshooting

```bash
docker compose ps                     # container status
docker compose logs --tail=100 api    # API logs
docker compose down                   # stop, keep data
```

**Warning:** `docker compose down -v` deletes the database volume and all stored devices.

If you delete `.env` but keep the volume, the new password will not match the existing database. Either restore `.env` or run `docker compose down -v` to start fresh.
