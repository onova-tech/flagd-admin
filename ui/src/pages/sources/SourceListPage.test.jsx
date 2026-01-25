import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../../contexts/AuthContext'
import SourceListPage from './SourceListPage'
import { get, del } from '../../services/api'
import { vi, beforeEach } from 'vitest'

// Mock API module
vi.mock('../../services/api', () => ({
  get: vi.fn(),
  del: vi.fn()
}))

// Mock useNavigate
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate
  }
})

// Mock AuthContext
vi.mock('../../contexts/AuthContext', () => ({
  AuthProvider: ({ children }) => children,
  useAuth: () => ({
    user: { name: 'test-user' },
    logout: vi.fn()
  })
}))

describe('SourceListPage', () => {
  const mockSources = [
    {
      id: '00000000-0000-0000-0000-000000001',
      name: 'Test Source 1',
      description: 'Test Description 1',
      uri: 'file://test1',
      enabled: true,
      creationDateTime: '2024-01-01T00:00:00Z',
      lastUpdateDateTime: '2024-01-01T00:00:00Z',
      lastUpdateUserName: 'test-user'
    },
    {
      id: '00000000-0000-0000-0000-000000002',
      name: 'Test Source 2',
      description: 'Test Description 2',
      uri: 'file://test2',
      enabled: false,
      creationDateTime: '2024-01-02T00:00:00Z',
      lastUpdateDateTime: '2024-01-02T00:00:00Z',
      lastUpdateUserName: 'test-user'
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('should display sources list', async () => {
    // Given
    get.mockResolvedValue({
      ok: true,
      json: async () => mockSources
    })

    // When
    render(
      <BrowserRouter>
        <AuthProvider>
          <SourceListPage />
        </AuthProvider>
      </BrowserRouter>
    )

    // Then
    await waitFor(() => {
      expect(screen.getByText('Sources')).toBeInTheDocument()
      expect(screen.getByText('Test Source 1')).toBeInTheDocument()
      expect(screen.getByText('Test Description 1')).toBeInTheDocument()
      expect(screen.getByText('Test Source 2')).toBeInTheDocument()
      expect(screen.getByText('Test Description 2')).toBeInTheDocument()
    })

    expect(get).toHaveBeenCalledWith('/api/v1/sources')
  })

  test('should handle API error', async () => {
    // Given
    get.mockResolvedValue({
      ok: false,
      status: 500
    })

    // When
    render(
      <BrowserRouter>
        <AuthProvider>
          <SourceListPage />
        </AuthProvider>
      </BrowserRouter>
    )

    // Then
    await waitFor(() => {
      expect(screen.getByText(/HTTP error/)).toBeInTheDocument()
    })
  })

  test('should handle source deletion', async () => {
    // Given
    get.mockResolvedValue({
      ok: true,
      json: async () => mockSources
    })
    del.mockResolvedValue({
      ok: true
    })

    render(
      <BrowserRouter>
        <AuthProvider>
          <SourceListPage />
        </AuthProvider>
      </BrowserRouter>
    )

    // When
    await waitFor(() => {
      expect(screen.getByText('Test Source 1')).toBeInTheDocument()
    })

    await act(async () => {
      const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
      fireEvent.click(deleteButtons[0])
    })

    // Confirm deletion - button already shows "confirm?"
    await waitFor(() => {
      expect(screen.getByText('confirm?')).toBeInTheDocument()
    })

    const confirmButton = screen.getByRole('button', { name: /confirm\?/i })
    fireEvent.click(confirmButton)

    // Then
    expect(del).toHaveBeenCalledWith('/api/v1/sources/00000000-0000-0000-0000-000000001')
  })
})