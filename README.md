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

The project offers two deployment architectures:

### Unified Architecture (Recommended)
A single container deployment that includes both services:
- **Supervisor Process Manager**: Orchestrates both API and UI services
- **Nginx Reverse Proxy**: Serves the UI on port 8080 and proxies API requests
- **Internal Communication**: UI communicates with API via nginx proxy, no CORS required
- **Single Entry Point**: Everything accessible through `http://localhost:8080`

### Separated Architecture
Two independent services (original design):

#### API Server
A Spring Boot REST API (`/api`) that provides:
- CRUD operations for feature flag sources
- Flag management endpoints
- Content validation and loading
- SQLite database for persistence
- JWT-based authentication with login/logout functionality

#### User Interface
A React-based web application (`/ui`) that provides:
- Source management interface
- Flag editor with real-time preview
- Targeting rule builder
- Schema validation
- Flag evaluation testing

## Quick Start

### Prerequisites

- **Java 21+** (for API)
- **Node.js 20+** (for UI)
- **Docker** or **Podman** (optional, for containerized deployment)

### Running Locally

#### Run the API

```bash
cd api && ./run-api.sh
```

The API will be available at `http://localhost:9090`

#### Run the UI

```bash
cd ui && ./run-ui.sh
```

The UI will be available at `http://localhost:5173`

#### Run with Docker

**Option 1: Single Container Image (Recommended)**

The unified Docker image includes both API and UI services running behind nginx:

```bash
# Build the unified image
docker build -t flagd-admin .

# Run with auto-generation (development)
docker run -d -p 8080:8080 --name flagd-admin flagd-admin

# Run with explicit configuration (production)
docker run -d -p 8080:8080 \
  -e FLAGD_JWT_SECRET="$(./api/scripts/generate-jwt-secret.sh | tail -1)" \
  -e FLAGD_ADMIN_USERNAME="myadmin" \
  -e FLAGD_ADMIN_PASSWORD_HASH="$(./api/scripts/generate-password-hash.sh mysecretpass)" \
  --name flagd-admin flagd-admin
```

The unified application will be available at `http://localhost:8080`

**Option 2: Separate Container Images**

**API Server:**
```bash
docker run -d -p 9090:9090 --name flagd-admin-api flagd-admin-api
```

**UI Application:**
```bash
docker run -d -p 5173:80 --name flagd-admin-ui flagd-admin-ui
```

> **Note:** To configure a custom API URL for the UI, create a `config.json` file:
> ```bash
> cat > config.json << EOF
> {
>   "apiBaseUrl": "http://your-api-host:9090"
> }
> EOF
> docker run -d -p 5173:80 -v $(pwd)/config.json:/usr/share/nginx/html/config.json --name flagd-admin-ui flagd-admin-ui
> ```

#### Run with Podman

**Option 1: Single Container Image (Recommended)**

```bash
# Build unified image
podman build --format docker -t flagd-admin .

# Run with auto-generation (development)
podman run -d -p 8080:8080 --name flagd-admin flagd-admin

# Run with explicit configuration (production)
podman run -d -p 8080:8080 \
  -e FLAGD_JWT_SECRET="$(./api/scripts/generate-jwt-secret.sh | tail -1)" \
  -e FLAGD_ADMIN_USERNAME="myadmin" \
  -e FLAGD_ADMIN_PASSWORD_HASH="$(./api/scripts/generate-password-hash.sh mysecretpass)" \
  --name flagd-admin flagd-admin
```

**Option 2: Separate Container Images**

**API Server:**
```bash
# Build api image
podman build --format docker -t flagd-admin-api .

# Development with auto-generation
podman run -d -p 9090:9090 --name flagd-admin-api flagd-admin-api

# Production with explicit configuration
podman run -d -p 9090:9090 \
  -e FLAGD_JWT_SECRET="$(./api/scripts/generate-jwt-secret.sh | tail -1)" \
  -e FLAGD_ADMIN_USERNAME="myadmin" \
  -e FLAGD_ADMIN_PASSWORD_HASH="$(./api/scripts/generate-password-hash.sh mysecretpass)" \
  --name flagd-admin-api flagd-admin-api
```

**UI Application:**
```bash
# Build ui image
podman build --format docker -t flagd-admin-ui .

# Run ui image
podman run -d -p 5173:80 --name flagd-admin-ui flagd-admin-ui
```

The unified application will be available at `http://localhost:8080`
The separate UI application will be available at `http://localhost:5173`

#### Stop and remove containers

**Unified Container:**

**Docker:**
```bash
docker stop flagd-admin
docker rm flagd-admin
```

**Podman:**
```bash
podman stop flagd-admin
podman rm flagd-admin
```

**Separate Containers:**

**Docker:**
```bash
docker stop flagd-admin-api flagd-admin-ui
docker rm flagd-admin-api flagd-admin-ui
```

**Podman:**
```bash
podman stop flagd-admin-api flagd-admin-ui
podman rm flagd-admin-api flagd-admin-ui
```

#### View logs

**Unified Container:**

**Docker:**
```bash
docker logs flagd-admin
```

**Podman:**
```bash
podman logs flagd-admin
```

**Separate Containers:**

**Docker:**
```bash
docker logs flagd-admin-api
docker logs flagd-admin-ui
```

**Podman:**
```bash
podman logs flagd-admin-api
podman logs flagd-admin-ui
```

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

> **Note:** The UI uses `config.json` to connect to the API. Default is `http://localhost:9090`. Modify `config.json` to use a different API endpoint.

## Configuration

### Environment Variables

The Flagd Admin API supports configuration via environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `FLAGD_AUTH_PROVIDER` | jwt | Authentication provider |
| `FLAGD_JWT_SECRET` | (auto-generated) | JWT signing secret (required for production) |
| `FLAGD_ADMIN_USERNAME` | admin | Default admin username |
| `FLAGD_ADMIN_PASSWORD_HASH` | (auto-generated for "pass") | BCrypt hash of admin password |
| `FLAGD_ACCESS_TOKEN_EXPIRATION` | 900000 | Access token expiration (ms) |
| `FLAGD_REFRESH_TOKEN_EXPIRATION` | 604800000 | Refresh token expiration (ms) |
| `FLAGD_LOGIN_REDIRECT_URI` | http://localhost:9090/ | Login redirect URL |

### API Configuration

Edit `api/src/main/resources/application.properties` or use environment variables above.

### Authentication

The API uses JWT-based authentication:

- **jwt**: JSON Web Token authentication with login/logout endpoints
- **Login**: `POST /api/v1/auth/login` with username/password
- **Refresh**: `POST /api/v1/auth/refresh` with refresh token
- **Protected**: All API endpoints require valid JWT token

#### Password Hash Generation
```bash
# Generate hash for custom password
./api/scripts/generate-password-hash.sh yourpassword

# Interactive password entry
./api/scripts/generate-password-hash.sh
```

#### JWT Secret Generation
```bash
# Generate secure JWT secret
./api/scripts/generate-jwt-secret.sh
```

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
│   ├── Dockerfile                 # Docker configuration for API (separate deployment)
│   ├── entrypoint.sh             # API container entrypoint script
│   ├── run-api.sh                 # Script to run API locally
│   └── README.md                  # API documentation
├── ui/                           # React UI application
│   ├── src/
│   │   ├── main.jsx             # Application entry point
│   │   ├── pages/               # Page components
│   │   │   ├── auth/            # Authentication pages
│   │   │   │   └── Login.jsx    # Login page
│   │   │   ├── sources/        # Source management pages
│   │   │   │   ├── SourceListPage.jsx    # Sources list page
│   │   │   │   └── CreateSourcePage.jsx  # Create source page
│   │   │   └── flags/           # Flag management pages
│   │   │       ├── FlagListPage.jsx      # Flags list page
│   │   │       └── FlagEditPage.jsx      # Flag edit page
│   │   ├── components/          # Reusable components
│   │   │   └── common/          # Common components
│   │   │       ├── ProtectedRoute.jsx    # Authentication wrapper
│   │   │       └── ErrorBoundary.jsx     # Error handling
│   │   ├── contexts/            # React contexts
│   │   │   └── AuthContext.jsx  # Authentication context
│   │   ├── services/            # API services
│   │   │   └── api.js           # API client
│   │   ├── features/            # Feature-specific utilities
│   │   │   └── flags/
│   │   │       └── utils/       # Flag utilities
│   │   │           ├── convertToFlagdFormat.js     # UI to flagd format
│   │   │           ├── convertFromFlagdFormat.js   # flagd to UI format
│   │   │           └── validateFlagdSchema.js      # Schema validation
│   │   └── config.js            # Application configuration
│   ├── package.json
│   ├── config.json              # Runtime API configuration
│   ├── Dockerfile               # Docker configuration for UI (separate deployment)
│   ├── nginx.conf               # Nginx configuration for UI
│   ├── run-ui.sh                # Script to run UI locally
│   └── README.md                # UI documentation
├── docker/                      # Unified deployment configuration
│   ├── supervisord.conf          # Supervisord process manager configuration
│   ├── nginx.conf               # Nginx configuration for unified deployment
│   └── entrypoint.sh            # Unified container entrypoint script
├── Dockerfile                   # Unified Docker configuration (recommended)
└── README.md                    # This file
```

## S3 Deployment

The UI can be deployed to AWS S3 for static hosting with runtime API configuration.

### Configuration

The UI uses `config.json` to set the API endpoint at runtime. This allows you to:
- Deploy the same build to multiple environments
- Update API URL without rebuilding
- Manage configuration separately from code

### Setup

1. **Create environment-specific config files:**

   `config.production.json`:
   ```json
   {
     "apiBaseUrl": "https://api.production.com"
   }
   ```

   `config.staging.json`:
   ```json
   {
     "apiBaseUrl": "https://api.staging.com"
   }
   ```

2. **Build the UI:**
   ```bash
   cd ui
   npm run build
   ```

3. **Deploy to S3:**

   **Production:**
   ```bash
   aws s3 cp dist/ s3://your-production-bucket/ --recursive
   aws s3 cp config.production.json s3://your-production-bucket/config.json --content-type application/json
   aws cloudfront create-invalidation --distribution-id YOUR_DISTRIBUTION_ID --paths "/*"
   ```

   **Staging:**
   ```bash
   aws s3 cp dist/ s3://your-staging-bucket/ --recursive
   aws s3 cp config.staging.json s3://your-staging-bucket/config.json --content-type application/json
   aws cloudfront create-invalidation --distribution-id YOUR_DISTRIBUTION_ID --paths "/*"
   ```

### Updating API URL

To change the API URL without redeploying:

```bash
aws s3 cp config.production.json s3://your-production-bucket/config.json --content-type application/json
aws cloudfront create-invalidation --distribution-id YOUR_DISTRIBUTION_ID --paths "/config.json"
```

### Local Development

For local development, the UI uses `config.json` with the local API URL:

```bash
npm run dev
```

Or override at runtime by editing `config.json`:

```json
{
  "apiBaseUrl": "http://localhost:9090"
}
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
