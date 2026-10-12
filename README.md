# Devices API

A RESTful API for managing device resources.

## Technology Stack

- Java 21
- Spring Boot 4.0.8
- Gradle
- PostgreSQL
- Spring Data JPA
- Flyway

## Requirements

The API will support creating, retrieving, updating, filtering, and deleting devices.

Each device has an ID, name, brand, state, and creation time.

## Business Rules

- Creation time cannot be updated.
- Name and brand cannot be updated while a device is in use.
- In-use devices cannot be deleted.

## Project Status

Initial Spring Boot project setup is complete. Database configuration and API implementation are pending.