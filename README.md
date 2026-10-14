# Devices API

A RESTful API for managing device resources, built with Java 21, Spring Boot, and PostgreSQL.

## Quick Start

### Prerequisites

- Docker Desktop (Windows/macOS) or Docker Engine with Docker Compose (Linux)
- Git

Java, Gradle, and PostgreSQL do not need to be installed separately.

### 1. Clone the repository

```bash
git clone https://github.com/neslihanbozer/devices-api.git
cd devices-api
```

### 2. Start the application

**Windows (PowerShell):**

```powershell
powershell -ExecutionPolicy Bypass -File .\start.ps1
```

The startup script automatically generates a secure PostgreSQL password on first run, stores it locally in `.env`, and starts the API and database containers.

No manual database configuration is required.

### 3. Access the API

| Resource | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health Check | http://localhost:8080/actuator/health |
| Devices API | http://localhost:8080/api/devices |

### 4. Stop the application

```bash
docker compose down
```

Database data is preserved between restarts using a Docker volume.

To restart on Windows, run the startup script again.

## Technology Stack

- Java 21
- Spring Boot 4.0.8
- Gradle
- PostgreSQL 17
- Spring Data JPA / Hibernate
- Flyway database migrations
- Docker and Docker Compose
- Testcontainers
- JUnit and MockMvc
- Springdoc OpenAPI / Swagger UI

## API Features

The API supports:

- Creating devices
- Retrieving a device by ID
- Listing all devices
- Filtering devices by brand or state
- Updating device information
- Deleting devices

Each device contains:

| Field | Description |
|---|---|
| `id` | Unique UUID identifier |
| `name` | Device name |
| `brand` | Device brand |
| `state` | `AVAILABLE`, `IN_USE`, or `INACTIVE` |
| `createdAt` | Immutable creation timestamp |

## Business Rules

- Creation time cannot be modified.
- Name and brand cannot be updated while a device is in use.
- Devices in the `IN_USE` state cannot be deleted.

## Database

PostgreSQL is automatically initialized when Docker Compose starts for the first time.

Flyway manages database schema migrations.

Database data is persisted using a Docker volume.

## Testing

Run the automated tests on Windows:

```powershell
.\gradlew.bat clean test
```

Integration tests use Testcontainers with PostgreSQL. Docker must be running.

To run the full build:

```powershell
.\gradlew.bat clean build
```

## Troubleshooting

Check running containers:

```bash
docker compose ps
```

View API logs:

```bash
docker compose logs --tail=100 api
```

Stop containers without deleting database data:

```bash
docker compose down
```

**Warning:** `docker compose down -v` deletes Docker-managed volumes and may permanently remove database data.