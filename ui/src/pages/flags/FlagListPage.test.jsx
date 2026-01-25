import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'

import FlagListPage from './FlagListPage'
import { get, del } from '../../services/api'

// =====================
// API mocks
// =====================
vi.mock('../../services/api', () => ({
  get: vi.fn(),
  del: vi.fn()
}))

// =====================
// Router mocks
// =====================
const mockNavigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ sourceId: 'test-source-id' })
  }
})

// =====================
// Auth mock
// =====================
vi.mock('../../contexts/AuthContext', () => ({
  AuthProvider: ({ children }) => children,
  useAuth: () => ({
    user: { name: 'test-user' },
    logout: vi.fn()
  })
}))

// =====================
// Test data
// =====================
const mockSource = {
  id: 'test-source-id',
  name: 'Test Source',
  description: 'Test Description',
  uri: 'file://test.json',
  enabled: true
}

const mockFlagsData = {
  flags: [
    {
      flagId: 'flag-1',
      name: 'Flag 1',
      description: 'Test flag 1',
      state: 'ENABLED',
      defaultVariant: 'on',
      variants: { on: true, off: false }
    },
    {
      flagId: 'flag-2',
      name: 'Flag 2',
      description: 'Test flag 2',
      state: 'DISABLED',
      defaultVariant: 'off',
      variants: { on: true, off: false }
    }
  ]
}

// =====================
// Helpers
// =====================
const renderPage = () =>
  render(
    <MemoryRouter initialEntries={['/sources/test-source-id/flags']}>
      <FlagListPage />
    </MemoryRouter>
  )

beforeEach(() => {
  vi.clearAllMocks()
  mockNavigate.mockClear()
})

// =====================
// Tests
// =====================
describe('FlagListPage', () => {
  describe('Data fetching', () => {
    test('fetches source and flags', async () => {
      get.mockImplementation(url => {
        if (url.endsWith('/sources/test-source-id')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve(mockSource) })
        }
        if (url.endsWith('/flags')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve(mockFlagsData) })
        }
      })

      renderPage()

      await waitFor(() => {
        expect(get).toHaveBeenCalledWith('/api/v1/sources/test-source-id')
        expect(get).toHaveBeenCalledWith('/api/v1/sources/test-source-id/flags')
      })
    })
  })

  describe('Flags rendering', () => {
    test('handles empty flags list', async () => {
      get.mockImplementation(url => {
        if (url.includes('/sources')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve(mockSource) })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve({ flags: [] }) })
        }
      })

      renderPage()

      await waitFor(() => {
        expect(
          screen.getByText(/no flags found in this source/i)
        ).toBeInTheDocument()
      })
    })
  })

  describe('Navigation', () => {
    test('navigates to new flag page', async () => {
      get.mockImplementation(url => {
        if (url.includes('/sources')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve(mockSource) })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve({ flags: [] }) })
        }
      })

      renderPage()

      await waitFor(() =>
        expect(screen.getByText(/no flags found/i)).toBeInTheDocument()
      )

      fireEvent.click(screen.getByText('New Flag'))

      expect(mockNavigate).toHaveBeenCalledWith(
        '/sources/test-source-id/flags/new'
      )
    })

    test('navigates back to sources', async () => {
      get.mockImplementation(url => {
        if (url.includes('/sources')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve(mockSource) })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve({ flags: [] }) })
        }
      })

      renderPage()
      await waitFor(() => screen.getByText('Sources'))

      fireEvent.click(screen.getByText('Sources'))

      expect(mockNavigate).toHaveBeenCalledWith('/')
    })
  })

  describe('Source display', () => {
    test('shows source name', async () => {
      get.mockImplementation(url => {
        if (url.includes('/sources')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve(mockSource) })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({ ok: true, json: () => Promise.resolve({ flags: [] }) })
        }
      })

      renderPage()

      await waitFor(() => {
        expect(screen.getByText('Test Source')).toBeInTheDocument()
      })
    })
  })
})
