# Flagd Admin UI

A modern React-based web interface for managing [flagd](https://flagd.dev/) feature flags. This UI provides a user-friendly interface to create, edit, and manage feature flag configurations in real-time.

## Features

- **Source Management**: Create and manage feature flag sources with file-based configurations
- **Flag Editor**: Full-featured editor for creating and editing feature flags
- **Flag Types**: Support for boolean, string, number, and object flag types
- **Targeting Rules**: Configure complex targeting rules with multiple conditions
- **Real-time Evaluation**: Test flag evaluations with custom context JSON
- **JSON Validation**: Validate flagd schema in real-time
- **Live Preview**: See generated flagd JSON configuration as you edit
- **Responsive Design**: Works seamlessly on desktop and mobile devices

## Tech Stack

- **React 18.2.0** - UI framework
- **React Router DOM 7.12.0** - Client-side routing
- **Vite 5.2.0** - Build tool and dev server
- **@openfeature/flagd-core 1.1.0** - Flagd SDK for evaluation
- **Vitest 1.4.0** - Testing framework
- **ESLint** - Code linting

## Project Structure

```
ui/
├── src/
│   ├── App.jsx                 # Main application and flag edit page
│   ├── SourceSelection.jsx     # Sources list page
│   ├── SourceCreation.jsx       # Create new source page
│   ├── FlagSelection.jsx       # Flags list for a source
│   ├── Rule.jsx                # Targeting rule component
│   ├── main.jsx                # Application entry point
│   ├── convertToFlagdFormat.js # Convert UI model to flagd format
│   ├── convertFromFlagdFormat.js # Convert flagd format to UI model
│   ├── validateFlagdSchema.js  # Schema validation
│   ├── App.css                 # App layout and structure
│   ├── SourceSelection.css     # Source page styles
│   ├── FlagSelection.css       # Flag list styles
│   ├── SourceCreation.css      # Source creation styles
│   ├── Rule.css                # Rule component styles
│   ├── components.css          # Reusable component styles
│   ├── tokens.css              # CSS variables (tokens)
│   └── index.css               # Global styles
├── public/                     # Static assets
├── package.json                # Dependencies and scripts
├── vite.config.js              # Vite configuration
└── README.md                   # This file
```

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn
- Flagd Admin Server API (running on http://localhost:9090 by default)

### Installation

```bash
# Install dependencies
npm install
```

### Development

```bash
# Start the development server
npm run dev

# The app will be available at http://localhost:5173
```

### Build for Production

```bash
# Build the application
npm run build

# Preview the production build
npm preview
```

## Available Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start development server with hot reload |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build locally |
| `npm run test` | Run tests with Vitest |
| `npm run coverage` | Run tests with coverage report |
| `npm run lint` | Run ESLint for code quality checks |

## Usage

### 1. Sources Page
Navigate to the root URL (`/`) to see all configured sources. Click on a source to view its flags, or click "New Source" to create a new one.

### 2. Create Source
Create a new source by providing:
- **Name**: A descriptive name for the source
- **Description**: Optional description
- **Source URI**: File path to the flagd configuration file (e.g., `file:///path/to/flags.json`)

### 3. Flag List
View all flags in a source. Click on a flag to edit it, or click "New Flag" to create a new one.

### 4. Flag Editor
Create or edit flags with the following options:

**Flag Configuration**
- **Flag Key**: Unique identifier for the flag
- **Description**: Optional description
- **Enabled**: Toggle flag on/off
- **Type**: Select boolean, string, number, or object

**Variants**
- Add, edit, or remove flag variants
- Define variant names and values based on flag type

**Default Variant**
- Select which variant to return when no rules match

**Targeting Rules**
- Enable targeting to add conditional logic
- Add "If" and "Else If" rules with conditions
- Configure rule operators:
  - String: starts_with, ends_with, contains, not contains
  - Semantic Version: sem_ver with range operators
  - Array: in list, not in list
  - Boolean: equals, strict equals, not equals, not strict equals, exists, not exists
  - Numeric: greater than, less than, and variants

- Set "Default Rule" as a fallback

**Evaluation**
- Test flag evaluation with custom JSON context
- See real-time evaluation results

### 5. Output Panel
View the generated flagd JSON configuration in real-time. Use the "Validate" button to check schema compliance and "Save" to persist changes.

## API Integration

The UI communicates with the Flagd Admin Server API:

- **Base URL**: `http://localhost:9090/api/v1`
- **Sources**: GET `/sources`, POST `/sources`, PATCH `/sources/{id}`, GET `/sources/{id}/contents`
- **Flags**: GET `/sources/{id}/flags`, GET `/sources/{id}/flags/{flagId}`, POST `/sources/{id}/flags/{flagId}`, DELETE `/sources/{id}/flags/{flagId}`

See the [API documentation](../api/README.md) for more details.

## Testing

```bash
# Run all tests
npm test

# Run tests in watch mode
npm test -- --watch

# Run tests with coverage
npm run coverage
```

### Test Files
- `convertToFlagdFormat.test.js` - Tests for converting UI model to flagd format
- `convertFromFlagdFormat.test.js` - Tests for converting flagd format to UI model
- `validateFlagdSchema.test.js` - Tests for schema validation

## Styling

The UI uses a custom design system with CSS tokens:

- **Tokens**: Defined in `tokens.css` (colors, spacing, typography, shadows, transitions)
- **Components**: Reusable component styles in `components.css`
- **Pages**: Page-specific styles in individual CSS files
- **Dark Mode**: Automatic dark mode support via `prefers-color-scheme`

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)

## Contributing

1. Follow the existing code style
2. Run `npm run lint` before committing
3. Add tests for new features
4. Ensure all tests pass

## License

See project root for license information.

## Resources

- [flagd Documentation](https://docs.flagd.dev/)
- [OpenFeature](https://openfeature.dev/)
- [React Router](https://reactrouter.com/)
- [Vite](https://vitejs.dev/)
- [Flagd Admin Server API](../api/README.md)
