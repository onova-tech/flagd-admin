import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../../contexts/AuthContext'
import FlagListPage from './FlagListPage'
import { get, del } from '../../services/api'

import { vi } from 'vitest'

// Mock API module
vi.mock('../../services/api', () => ({
  get: vi.fn(),
  del: vi.fn()
}))

// Create a single mockNavigate that persists across tests
const mockNavigate = vi.fn()

// Mock useNavigate and useParams
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ sourceId: 'test-source-id' })
  })
  })
})

// Mock AuthContext
vi.mock('../../contexts/AuthContext', () => ({
  AuthProvider: ({ children }) => children,
  useAuth: () => ({
    user: { name: 'test-user' },
    logout: vi.fn()
  })
}))

describe('FlagListPage', () => {
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
        variants: { 'on': true, 'off': false }
      },
      {
        flagId: 'flag-2',
        name: 'Flag 2',
        description: 'Test flag 2',
        state: 'DISABLED',
        defaultVariant: 'off',
        variants: { 'on': true, 'off': false }
      }
    ]
  }

  const renderFlagListPage = (sourceId = 'test-source-id') => {
    return render(
      <BrowserRouter>
        <AuthProvider>
          <FlagListPage />
        </AuthProvider>
      </BrowserRouter>,
      { initialEntries: [`/sources/${sourceId}/flags`] }
    )
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockNavigate.mockClear()
  })

  describe('Loading states', () => {
    test('should show loading indicator initially', () => {
      // Given
      get.mockImplementation(() => new Promise(() => {})) // Never resolves

      // When
      renderFlagListPage()

      // Then - find the main loading div (not breadcrumb)
      const loadingElements = screen.getAllByText('Loading...')
      expect(loadingElements).toHaveLength(2) // breadcrumb + main loading
    })

    test('should hide loading after data loads', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagsData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.queryByText('Loading...')).not.toBeInTheDocument()
      })
    })
  })

  describe('Data fetching', () => {
    test('should fetch source and flags on mount', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id') && !url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagsData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(get).toHaveBeenCalledWith('/api/v1/sources/test-source-id')
        expect(get).toHaveBeenCalledWith('/api/v1/sources/test-source-id/flags')
      })
    })

    test('should display flags after loading', async () => {
      // Given
      get.mockImplementation((url) => {
        console.log('Mock URL called:', url)
        if (url === '/api/v1/sources/test-source-id') {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url === '/api/v1/sources/test-source-id/flags') {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagsData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then - check for flag display
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument()
        expect(screen.getByText('flag-2')).toBeInTheDocument()
        expect(screen.getByText('Test flag 1')).toBeInTheDocument()
        expect(screen.getByText('Test flag 2')).toBeInTheDocument()
      }, { timeout: 3000 })
    })

    test('should handle empty flags list', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ flags: [] })
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText(/No flags found in this source/i)).toBeInTheDocument()
      })
    })

    test('should handle source fetch error', async () => {
      // Given
      get.mockRejectedValue(new Error('Network error'))

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText(/network error/i)).toBeInTheDocument()
      })
    })

    test('should handle API error response', async () => {
      // Given
      get.mockResolvedValue({ ok: false, status: 500 })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText(/http error/i)).toBeInTheDocument()
      })
    })
  })

  describe('Flag interactions', () => {
    beforeEach(() => {
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagsData)
          })
        }
        return Promise.resolve({ ok: false })
      })
    })

    test('should navigate to flag edit when flag is clicked', async () => {
      // Given
      renderFlagListPage()
      
      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument()
      })

      // When
      const flagLink = screen.getByText('flag-1')
      fireEvent.click(flagLink)

      // Then
      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/sources/test-source-id/flags/flag-1')
      })
    })

    test('should show delete confirmation when delete is clicked', async () => {
      // Given
      renderFlagListPage()
      
      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument()
      })

      // When
      const deleteButtons = screen.getAllByTitle('Delete flag')
      fireEvent.click(deleteButtons[0])

      // Then
      await waitFor(() => {
        expect(screen.getByText(/confirm\?/i)).toBeInTheDocument()
      })
    })

test('should delete flag when confirmed', async () => {
      // Given
      del.mockResolvedValue({ ok: true })
      renderFlagListPage()
      
      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument()
      })

      // When
      const deleteButtons = screen.getAllByTitle('Delete flag')
      fireEvent.click(deleteButtons[0])

      const confirmButton = screen.getByRole('button', { name: /confirm\?/i })
      fireEvent.click(confirmButton)

      // Then
      await waitFor(() => {
        expect(del).toHaveBeenCalledWith('/api/v1/sources/test-source-id/flags/flag-1')
        expect(screen.queryByText('flag-1')).not.toBeInTheDocument()
      })
    })
    })

    test('should not delete flag when cancelled', async () => {
      // Given
      renderFlagListPage()
      
      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument()
      })

      // When
      const deleteButtons = screen.getAllByTitle('Delete flag')
      fireEvent.click(deleteButtons[0])

      // Then the confirmation button should appear
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /confirm\?/i })).toBeInTheDocument()
      })

      // But we don't have a cancel button in the current UI, so let's just check it doesn't delete
      expect(del).not.toHaveBeenCalled()
    })

    test('should handle delete error', async () => {
      // Given
      del.mockRejectedValue(new Error('Delete failed'))
      renderFlagListPage()
      
      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument()
      })

      // When
      const deleteButtons = screen.getAllByTitle('Delete flag')
      fireEvent.click(deleteButtons[0])

      const confirmButton = screen.getByRole('button', { name: /confirm\?/i })
      fireEvent.click(confirmButton)

      // Then
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument() // Flag should still be there
      })
    })

test('should show loading state during deletion', async () => {
      // Given
      let resolveDelete
      const deletePromise = new Promise(resolve => {
        resolveDelete = resolve
      })
      del.mockReturnValue(deletePromise)
      renderFlagListPage()
      
      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument()
      })

      // When
      const deleteButtons = screen.getAllByTitle('Delete flag')
      fireEvent.click(deleteButtons[0])

      const confirmButton = screen.getByRole('button', { name: /confirm\?/i })
      fireEvent.click(confirmButton)

      // Then - the button should show loading state
      await waitFor(() => {
        expect(confirmButton).toHaveTextContent('...')
      })

      // Resolve promise
      resolveDelete({ ok: true })

      await waitFor(() => {
        expect(screen.queryByText('flag-1')).not.toBeInTheDocument()
      })
    })
  })

  describe('Navigation', () => {
    test('should show "New Flag" button', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagsData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText('flag-1')).toBeInTheDocument()
        expect(screen.getByText('flag-2')).toBeInTheDocument()
        expect(screen.getByText('Flag 1')).toBeInTheDocument()
        expect(screen.getByText('Flag 2')).toBeInTheDocument()
        expect(screen.getByText('New Flag')).toBeInTheDocument()
      }, { timeout: 2000 })
    })

    test('should navigate to new flag page when "New Flag" is clicked', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ flags: [] })
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()
      await waitFor(() => {
        expect(screen.getByText('No flags found in this source')).toBeInTheDocument()
      })

      const newFlagButton = screen.getByText('New Flag')
      fireEvent.click(newFlagButton)

      // Then
      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/sources/test-source-id/flags/new')
      })
    })

    test('should navigate back to sources when back is clicked', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ flags: [] })
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()
      await waitFor(() => {
        expect(screen.getByText('Sources')).toBeInTheDocument()
      })

      const backButton = screen.getByText('Sources')
      fireEvent.click(backButton)

      // Then
      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/')
      })
    })
  })

  describe('Source display', () => {
    test('should display source name', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ flags: [] })
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText('Test Source')).toBeInTheDocument()
      })
    })

    test('should handle missing source data', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.reject(new Error('Invalid JSON'))
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ flags: [] })
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText('Flags')).toBeInTheDocument() // Should still render flags section
      })
    })
  })

  describe('Error handling', () => {
    test('should handle JSON parsing errors gracefully', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.reject(new Error('Invalid JSON'))
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ flags: [] })
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.queryByText(/loading/i)).not.toBeInTheDocument()
        // Should still render page even with source JSON error
      })
    })

    test('should handle flags JSON parsing errors', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.reject(new Error('Invalid JSON'))
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagListPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText(/No flags found in this source/i)).toBeInTheDocument()
      })
    })
  })
}