# Flagd Admin Server API

A Spring Boot REST API for managing flagd feature flag sources and flags. This server provides endpoints to create, update, and manage feature flag sources stored in file-based configurations.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Architecture](#architecture)
- [Logging](#logging)
- [Security](#security)
- [Running the Application](#running-the-application)
- [Testing](#testing)

## Overview

The Flagd Admin Server API provides a RESTful interface to:
- Manage feature flag sources (CRUD operations)
- Load and validate flagd configuration files
- Query and manage individual feature flags
- Handle content validation and source accessibility

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.1**
- **Spring Data JPA** with Hibernate
- **SQLite** database (H2 for testing)
- **Spring Security** with configurable authentication providers
- **AspectJ** for cross-cutting concerns (logging)
- **flagd Java SDK** (0.11.19) and OpenFeature SDK (1.20.0) for feature flag management

## Project Structure

```
api/
├── src/main/java/tech/onova/flagd_admin_server/
│   ├── FlagdAdminServerApplication.java     # Main Spring Boot application
│   ├── controller/
│   │   ├── SourcesController.java           # REST endpoints for sources and flags
│   │   ├── exception/
│   │   │   └── GlobalExceptionHandler.java  # Global error handling
│   │   └── DTOs/                            # Data Transfer Objects
│   │       ├── ErrorResponseDTO.java
│   │       ├── FlagConfigRequestDTO.java
│   │       ├── FlagDTO.java
│   │       ├── FlagsResponseDTO.java
│   │       ├── SourceContentResponseDTO.java
│   │       ├── SourcePatchRequestDTO.java
│   │       ├── SourcePostRequestDTO.java
│   │       ├── SourceResponseDTO.java
│   │       └── TargetingDTO.java
│   ├── domain/
│   │   ├── entity/                          # Domain entities
│   │   │   ├── Source.java                  # Source aggregate root
│   │   │   ├── SourceId.java                # Value object for source ID
│   │   │   └── SourceUri.java               # Value object for source URI
│   │   ├── exception/
│   │   │   ├── ContentValidationException.java
│   │   │   ├── DomainException.java
│   │   │   ├── SourceContentAccessException.java
│   │   │   ├── SourceContentNotFoundException.java
│   │   │   ├── SourceNotFoundException.java
│   │   │   └── UnsupportedSourceUriException.java
│   │   ├── repository/
│   │   │   └── SourceRepository.java        # JPA repository
│   │   └── service/
│   │       ├── ContentValidator.java
│   │       ├── FlagService.java             # Flag management service
│   │       ├── SourceContentLoader.java
│   │       ├── SourceContentService.java    # Content management service
│   │       └── impl/
│   │           ├── FileSourceContentLoader.java
│   │           ├── FlagServiceImpl.java
│   │           ├── FlagdContentValidator.java
│   │           └── SourceContentServiceImpl.java
│   ├── infrastructure/
│   │   ├── annotation/
│   │   │   └── Log.java                     # Custom logging annotation
│   │   └── aspect/
│   │       └── LoggingAspect.java           # AOP logging implementation
│   └── security/
│       ├── SecurityConfig.java              # Spring Security configuration
│       └── providers/
│           ├── AuthProvider.java            # Authentication provider interface
│           ├── BasicAuthProvider.java       # Basic auth implementation
│           └── NoAuth.java                  # No-auth implementation
├── src/main/resources/
│   ├── application.properties                # Application configuration
│   └── app.db                              # SQLite database
└── build.gradle                            # Gradle build configuration
```

## Configuration

### Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 9090 | Server port |
| `application.auth.provider` | no_auth | Authentication provider (no_auth, basic) |
| `application.auth.provider.basic.user.name` | user | Basic auth username |
| `application.auth.provider.basic.user.encoded_password` | $2a$12$... | Encoded password (BCrypt) |
| `application.auth.login.default_redirect_uri` | http://localhost:9090/ | Redirect URI after login |
| `spring.datasource.url` | jdbc:sqlite:src/main/resources/app.db | SQLite database URL |

### Authentication Providers

The application supports multiple authentication providers:
- **no_auth**: No authentication required (default)
- **basic**: Form-based authentication with username/password

Configure via `application.auth.provider` property.

## API Endpoints

Base URL: `http://localhost:9090/api/v1`

### Sources

#### Get All Sources
```
GET /api/v1/sources
```

**Query Parameters:**
- `isEnabled` (optional, default: true) - Filter by enabled status

**Response (200 OK):**
```json
[
  {
    "id": "uuid",
    "name": "Source Name",
    "description": "Source Description",
    "uri": "file:///path/to/config.json",
    "enabled": true,
    "creationDateTime": "2026-01-18T10:00:00Z",
    "lastUpdateDateTime": "2026-01-18T10:00:00Z",
    "lastUpdateUserName": "system"
  }
]
```

#### Get Source by ID
```
GET /api/v1/sources/{sourceId}
```

**Response (200 OK):**
```json
{
  "id": "uuid",
  "name": "Source Name",
  "description": "Source Description",
  "uri": "file:///path/to/config.json",
  "enabled": true,
  "creationDateTime": "2026-01-18T10:00:00Z",
  "lastUpdateDateTime": "2026-01-18T10:00:00Z",
  "lastUpdateUserName": "system"
}
```

**Response (404 Not Found):** Source not found

#### Create Source
```
POST /api/v1/sources
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "My Feature Flags",
  "description": "Production feature flags",
  "uri": "file:///path/to/flags.json"
}
```

**Response (201 Created):** Returns the created source object

#### Update Source
```
PATCH /api/v1/sources/{sourceId}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "Updated Name",
  "description": "Updated Description",
  "uri": "file:///new/path/to/flags.json",
  "enabled": false
}
```

**Response (200 OK):** Returns the updated source object

#### Get Source Content
```
GET /api/v1/sources/{sourceId}/contents
```

**Response (200 OK):**
```json
{
  "content": "{\"flags\": {...}}"
}
```

### Flags

#### Get All Flags for Source
```
GET /api/v1/sources/{sourceId}/flags
```

**Response (200 OK):**
```json
{
  "flags": [
    {
      "flagId": "my-feature",
      "name": "My Feature",
      "description": "Feature description",
      "state": "ENABLED",
      "defaultVariant": "on",
      "variants": {
        "on": true,
        "off": false
      },
      "targeting": {
        "targetingKey": {
          "userId": "string"
        },
        "rule": "if (userId in ['user1', 'user2']) return true"
      }
    }
  ]
}
```

#### Get Flag by ID
```
GET /api/v1/sources/{sourceId}/flags/{flagId}
```

**Response (200 OK):** Returns a single `FlagDTO` object

#### Create or Update Flag
```
POST /api/v1/sources/{sourceId}/flags/{flagId}
Content-Type: application/json
```

**Request Body:**
```json
{
  "name": "My Feature",
  "description": "Feature description",
  "state": "ENABLED",
  "defaultVariant": "on",
  "variants": {
    "on": true,
    "off": false
  },
  "targeting": {
    "targetingKey": {
      "userId": "string"
    },
    "rule": "if (userId in ['user1', 'user2']) return true"
  }
}
```

**Response (204 No Content):** Success

#### Delete Flag
```
DELETE /api/v1/sources/{sourceId}/flags/{flagId}
```

**Response (204 No Content):** Success

## Data Models

### SourceResponseDTO
```java
{
  "id": UUID,
  "name": String,
  "description": String,
  "uri": String,
  "enabled": boolean,
  "creationDateTime": ZonedDateTime,
  "lastUpdateDateTime": ZonedDateTime,
  "lastUpdateUserName": String
}
```

### SourcePostRequestDTO
```java
{
  "name": String (required, @NotBlank),
  "description": String (required, @NotBlank),
  "uri": String (required, must match ^file://.*$)
}
```

### SourcePatchRequestDTO
```java
{
  "name": String (required, @NotBlank),
  "description": String (required, @NotBlank),
  "uri": String (required, must match ^file://.*$),
  "enabled": boolean
}
```

### SourceContentInitRequestDTO
```java
{
  "content": String (required, @NotBlank)
}
```

### FlagDTO
```java
{
  "flagId": String,
  "name": String,
  "description": String,
  "state": String,
  "defaultVariant": String,
  "variants": Map<String, Object>,
  "targeting": TargetingDTO
}
```

### FlagConfigRequestDTO
```java
{
  "name": String (required, @NotBlank),
  "description": String,
  "state": String (required, @NotBlank),
  "defaultVariant": String,
  "variants": Map<String, Object>,
  "targeting": TargetingDTO
}
```

### TargetingDTO
```java
{
  "targetingKey": Map<String, String>,
  "rule": String
}
```

### ErrorResponseDTO
```java
{
  "errorCode": String,
  "message": String,
  "timestamp": ZonedDateTime
}
```

## Error Handling

All endpoints may return error responses in the following format:

```json
{
  "errorCode": "ERROR_CODE",
  "message": "Error message description",
  "timestamp": "2026-01-18T10:00:00Z"
}
```

### Common Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `SOURCE_NOT_FOUND` | 404 | Source with given ID not found |
| `CONTENT_NOT_FOUND` | 404 | Source content file not found |
| `CONTENT_ACCESS_ERROR` | 500 | Unable to access source content |
| `CONTENT_VALIDATION_ERROR` | 400 | Invalid flagd configuration |
| `UNSUPPORTED_URI` | 400 | Source URI format not supported |
| `INVALID_ARGUMENT` | 400 | Invalid request parameters |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error |

## Architecture

### Layered Architecture

The application follows a clean, layered architecture:

1. **Controller Layer**: Handles HTTP requests/responses and routing
2. **Service Layer**: Business logic and domain operations
3. **Repository Layer**: Data access and persistence
4. **Domain Layer**: Core business entities and value objects
5. **Infrastructure Layer**: Cross-cutting concerns (logging, security)

### Design Patterns

- **DTO Pattern**: Separates API contracts from domain models
- **Repository Pattern**: Abstracts data access logic
- **Service Pattern**: Encapsulates business logic
- **Value Objects**: Immutable objects (`SourceId`, `SourceUri`)
- **AOP (Aspect-Oriented Programming)**: Centralized logging via `@Log` annotation

## Logging

The application uses AOP for automatic request/response logging:

- **Annotation**: `@Log` on controller/service methods
- **Aspect**: `LoggingAspect` intercepts annotated methods
- **Logs**: Request arguments, response values, errors, and execution time

## Security

Spring Security is configured with pluggable authentication providers:

- **NoAuth**: Disables authentication and CSRF protection (useful for development). All requests are permitted.
- **BasicAuthProvider**: Form-based authentication with username/password and BCrypt password encoding. Includes login form and logout support.

Both providers are configured with CORS support, allowing all origins, methods, headers, and credentials.

Configure via `application.auth.provider` property.

## Running the Application

### Prerequisites

- Java 21+
- Gradle 8+

### Build and Run

```bash
# Navigate to api directory
cd api

# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The server will start on `http://localhost:9090`

### Build JAR and Run

```bash
# Build executable JAR
./gradlew build

# Run JAR
java -jar build/libs/flagd-admin-server-0.0.1-SNAPSHOT.jar
```

## Testing

### Run Tests

```bash
./gradlew test
```

### Test Coverage

- **Unit Tests**: JUnit 5 with Mockito
- **Integration Tests**: Spring Boot Test
- **Test Database**: H2 (in-memory)
- **Test Utilities**: TestDataBuilder for creating test data

### Test Structure

```
src/test/java/tech/onova/flagd_admin_server/
├── FlagdAdminServerApplicationTests.java
├── testutil/
│   └── TestDataBuilder.java
├── controller/
│   ├── SourcesControllerTest.java
│   └── exception/
│       └── GlobalExceptionHandlerTest.java
├── domain/
│   ├── entity/
│   │   ├── SourceIdTest.java
│   │   ├── SourceTest.java
│   │   └── SourceUriTest.java
│   ├── exception/
│   │   ├── ContentValidationExceptionTest.java
│   │   ├── DomainExceptionTest.java
│   │   ├── SourceContentAccessExceptionTest.java
│   │   ├── SourceContentNotFoundExceptionTest.java
│   │   ├── SourceNotFoundExceptionTest.java
│   │   └── UnsupportedSourceUriExceptionTest.java
│   └── service/
│       ├── FileSourceContentLoaderTest.java
│       ├── FlagdContentValidatorTest.java
│       ├── FlagServiceImplTest.java
│       └── SourceContentServiceImplTest.java
├── infrastructure/
│   └── aspect/
│       └── LoggingAspectTest.java
└── security/
    └── providers/
        ├── AuthProviderInterfaceTest.java
        ├── BasicAuthProviderTest.java
        └── NoAuthProviderTest.java
```

## Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [flagd Documentation](https://docs.flagd.dev/)
- [OpenFeature SDK](https://openfeature.dev/)
