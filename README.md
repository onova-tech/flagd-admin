# Flagd Admin

A web-based administration interface for managing [flagd](https://flagd.dev/) feature flag configurations. This project provides both a REST API and a user-friendly UI to create, edit, and manage feature flags stored in file-based configurations.

## Purpose

Flagd Admin simplifies the management of feature flags by providing:

- **Centralized Management**: Manage all your feature flag sources and flags from a single interface
- **Real-time Configuration**: Edit flags and see changes reflected immediately in flagd
- **Visual Editor**: Intuitive UI for creating and editing feature flags with complex targeting rules
- **Schema Validation**: Ensure flag configurations comply with the flagd specification
- **RESTful API**: Programmatic access to all flag management operations

## Architecture

The project consists of two main components:

### API Server
A Spring Boot REST API (`/api`) that provides:
- CRUD operations for feature flag sources
- Flag management endpoints
- Content validation and loading
- SQLite database for persistence
- Pluggable authentication (no-auth or basic auth)

### User Interface
A React-based web application (`/ui`) that provides:
- Source management interface
- Flag editor with real-time preview
- Targeting rule builder
- Schema validation
- Flag evaluation testing

## Quick Start

### Prerequisites

- **Java 21+** (for API)
- **Node.js 18+** (for UI)
- **Docker** (optional, for containerized deployment)

### Running Locally

#### Run the API

```bash
./run-api.sh
```

The API will be available at `http://localhost:9090`

#### Run the UI

```bash
./run-ui.sh
```

The UI will be available at `http://localhost:5173`

### Running with Docker

#### Build the Docker image

```bash
docker build -t flagd-admin .
```

#### Run the API in Docker

```bash
docker run -p 9090:9090 flagd-admin
```

The API will be available at `http://localhost:9090`

> **Note**: The Docker image currently includes the API only. Run the UI separately using `npm run dev` in the `ui` directory.

## Usage

### 1. Creating a Source

A **source** represents a flagd configuration file that contains feature flags.

1. Open the UI at `http://localhost:5173`
2. Click "New Source"
3. Fill in the source details:
   - **Name**: A descriptive name (e.g., "Production Flags")
   - **Description**: Optional description
   - **Source URI**: File path to the flagd configuration file (e.g., `file:///path/to/flags.json`)
4. Click "Save"

### 2. Managing Flags

Once a source is created, you can:

- **View all flags**: Click on a source to see its flags
- **Create a new flag**: Click "New Flag"
- **Edit a flag**: Click on any flag to edit it
- **Delete a flag**: Use the delete button on the flag list

### 3. Flag Configuration

Each flag can be configured with:

#### Basic Settings
- **Flag Key**: Unique identifier for the flag
- **Description**: Optional description
- **Enabled**: Toggle the flag on/off
- **Type**: boolean, string, number, or object

#### Variants
- Define multiple variants with different values
- Each variant has a name and a value
- The type of values must match the flag type

#### Default Variant
- Select which variant is returned when no targeting rules match

#### Targeting Rules
- Add conditional logic to return different variants based on context
- Support for multiple rule types:
  - **String**: starts_with, ends_with, contains, not contains
  - **Semantic Version**: sem_ver with range operators
  - **Array**: in list, not in list
  - **Boolean**: equals, strict equals, not equals, not strict equals, exists, not exists
  - **Numeric**: greater than, less than
- Set a "Default Rule" as a fallback

### 4. Testing Flags

The UI provides a live evaluation tester:

1. Enter JSON context representing your evaluation context
2. See which variant would be returned
3. Validate that targeting rules work as expected

### 5. Output and Validation

- View the generated flagd JSON configuration in real-time
- Use the "Validate" button to check schema compliance
- Save changes to persist them to the source file

## API Documentation

The API provides comprehensive RESTful endpoints. For detailed documentation, see [API Documentation](./api/README.md).

### Base URL
`http://localhost:9090/api/v1`

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sources` | List all sources |
| POST | `/sources` | Create a new source |
| PATCH | `/sources/{id}` | Update a source |
| GET | `/sources/{id}/contents` | Get source content |
| GET | `/sources/{id}/flags` | List all flags for a source |
| POST | `/sources/{id}/flags/{flagId}` | Create or update a flag |
| DELETE | `/sources/{id}/flags/{flagId}` | Delete a flag |

## UI Documentation

For detailed UI documentation, see [UI Documentation](./ui/README.md).

### Tech Stack

- **React 18.2.0** - UI framework
- **React Router DOM 7.12.0** - Client-side routing
- **Vite 5.2.0** - Build tool and dev server
- **@openfeature/flagd-core 1.1.0** - Flagd SDK

## Development

### API Development

```bash
cd api
./gradlew build      # Build the project
./gradlew test       # Run tests
./gradlew bootRun    # Run the application
```

### UI Development

```bash
cd ui
npm install          # Install dependencies
npm run dev          # Start development server
npm run build        # Build for production
npm run test         # Run tests
npm run lint         # Run ESLint
```

## Configuration

### API Configuration

Edit `api/src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 9090 | Server port |
| `application.auth.provider` | no_auth | Authentication provider (no_auth, basic) |
| `spring.datasource.url` | jdbc:sqlite:... | SQLite database URL |

### Authentication

The API supports multiple authentication providers:

- **no_auth**: No authentication required (default, development mode)
- **basic**: Form-based authentication with username/password

Configure via `application.auth.provider` property.

## Flagd Integration

Flagd Admin integrates with flagd by:

1. **Reading Configuration**: Loads flagd JSON configuration files from file URIs
2. **Validating Schema**: Ensures configurations comply with flagd specification
3. **Writing Configuration**: Saves edited flags back to flagd JSON files
4. **Evaluating Flags**: Uses the flagd SDK to test flag evaluations

To use Flagd Admin with flagd:

1. Configure a source pointing to your flagd configuration file
2. Make sure flagd has read/write access to the same file
3. Changes made in Flagd Admin will be immediately available to flagd

## Project Structure

```
flagd-admin/
├── api/                          # Spring Boot REST API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── tech/onova/flagd_admin_server/
│   │   │   │       ├── controller/      # REST endpoints
│   │   │   │       ├── domain/          # Domain layer
│   │   │   │       ├── infrastructure/  # Infrastructure layer
│   │   │   │       └── security/        # Security configuration
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   ├── build.gradle               # Gradle build configuration
│   └── README.md                  # API documentation
├── ui/                           # React UI application
│   ├── src/
│   │   ├── App.jsx              # Main application
│   │   ├── SourceSelection.jsx   # Sources list page
│   │   ├── SourceCreation.jsx   # Create source page
│   │   ├── FlagSelection.jsx    # Flags list page
│   │   ├── convertToFlagdFormat.js    # UI to flagd format
│   │   ├── convertFromFlagdFormat.js  # flagd to UI format
│   │   └── validateFlagdSchema.js     # Schema validation
│   ├── package.json
│   └── README.md                 # UI documentation
├── run-api.sh                   # Script to run API locally
├── run-ui.sh                    # Script to run UI locally
├── Dockerfile                   # Docker configuration
└── README.md                    # This file
```

## Testing

### API Tests

```bash
cd api
./gradlew test
```

### UI Tests

```bash
cd ui
npm run test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and linting
5. Submit a pull request

## License

See individual component licenses for more information.

## Resources

- [flagd Documentation](https://docs.flagd.dev/)
- [OpenFeature](https://openfeature.dev/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [React](https://react.dev/)
- [Vite](https://vitejs.dev/)
