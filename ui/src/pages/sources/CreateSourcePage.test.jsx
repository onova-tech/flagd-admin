import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../../contexts/AuthContext'
import CreateSourcePage from './CreateSourcePage'
import { post } from '../../services/api'
import { vi, beforeEach, describe, it, expect } from 'vitest'

// Mock API module
vi.mock('../../services/api', () => ({
  post: vi.fn()
}))

// Create a single mockNavigate that persists across tests
const mockNavigate = vi.fn()

// Mock useNavigate
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

describe('CreateSourcePage', () => {
  const renderCreateSourcePage = () => {
    return render(
      <BrowserRouter>
        <AuthProvider>
          <CreateSourcePage />
        </AuthProvider>
      </BrowserRouter>
    )
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockNavigate.mockClear()
  })

  describe('Form rendering', () => {
    test('should render source creation form', () => {
      renderCreateSourcePage()

      expect(screen.getByText('New Source')).toBeInTheDocument()
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/description/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/source uri/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /create/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument()
    })

    test('should have proper form structure', () => {
      renderCreateSourcePage()

      expect(screen.getByText('Source Configuration')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('Enter source name')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('Enter source description')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('file:///path/to/flags.json')).toBeInTheDocument()
    })

    test('should show navigation breadcrumb', () => {
      renderCreateSourcePage()

      expect(screen.getByText('Sources')).toBeInTheDocument()
      expect(screen.getByText('New Source')).toBeInTheDocument()
      const separator = screen.getByText('/')
      expect(separator).toBeInTheDocument()
    })
  })

  describe('Form inputs', () => {
    test('should update name input', () => {
      renderCreateSourcePage()
      
      const nameInput = screen.getByLabelText(/name/i)
      fireEvent.change(nameInput, { target: { value: 'Test Source' } })

      expect(nameInput).toHaveValue('Test Source')
    })

    test('should update description input', () => {
      renderCreateSourcePage()
      
      const descriptionInput = screen.getByLabelText(/description/i)
      fireEvent.change(descriptionInput, { target: { value: 'Test Description' } })

      expect(descriptionInput).toHaveValue('Test Description')
    })

    test('should update URI input', () => {
      renderCreateSourcePage()
      
      const uriInput = screen.getByLabelText(/source uri/i)
      fireEvent.change(uriInput, { target: { value: 'file://test/flags.json' } })

      expect(uriInput).toHaveValue('file://test/flags.json')
    })

    test('should have required fields', () => {
      renderCreateSourcePage()
      
      const nameInput = screen.getByLabelText(/name/i)
      const uriInput = screen.getByLabelText(/source uri/i)
      
      expect(nameInput).toBeRequired()
      expect(uriInput).toBeRequired()
    })

    test('should have optional description field', () => {
      renderCreateSourcePage()
      
      const descriptionInput = screen.getByLabelText(/description/i)
      
      expect(descriptionInput).not.toBeRequired()
    })
  })

  describe('Form validation', () => {
    test('should not submit with empty name', async () => {
      renderCreateSourcePage()
      
      const submitButton = screen.getByRole('button', { name: /create source/i })
      
      // Try to submit form without required fields
      fireEvent.click(submitButton)
      
      // HTML5 validation should prevent submission
      expect(post).not.toHaveBeenCalled()
    })

    test('should not submit with empty URI', async () => {
      renderCreateSourcePage()
      
      const nameInput = screen.getByLabelText(/name/i)
      fireEvent.change(nameInput, { target: { value: 'Test Source' } })
      
      const submitButton = screen.getByRole('button', { name: /create source/i })
      fireEvent.click(submitButton)
      
      expect(post).not.toHaveBeenCalled()
    })

    test('should submit with all fields filled', async () => {
      post.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: 'test-source-id' })
      })

      renderCreateSourcePage()
      
      // Fill form
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Source' } })
      fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'Test Description' } })
      fireEvent.change(screen.getByLabelText(/source uri/i), { target: { value: 'file://test/flags.json' } })
      
      // Submit form
      const submitButton = screen.getByRole('button', { name: /create/i })
      fireEvent.click(submitButton)
      
      await waitFor(() => {
        expect(post).toHaveBeenCalledWith('/api/v1/sources', {
          name: 'Test Source',
          description: 'Test Description',
          uri: 'file://test/flags.json'
        })
      })
    })

    test('should submit with optional description empty', async () => {
      post.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: 'test-source-id' })
      })

      renderCreateSourcePage()
      
      // Fill required fields only
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Source' } })
      fireEvent.change(screen.getByLabelText(/source uri/i), { target: { value: 'file://test/flags.json' } })
      
      // Submit form
      const submitButton = screen.getByRole('button', { name: /create/i })
      fireEvent.click(submitButton)
      
      await waitFor(() => {
        expect(post).toHaveBeenCalledWith('/api/v1/sources', {
          name: 'Test Source',
          description: null,
          uri: 'file://test/flags.json'
        })
      })
    })
  })

  describe('API interactions', () => {
    test('should handle successful source creation', async () => {
      const mockSource = { id: 'test-source-id', name: 'Test Source' }
      post.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockSource)
      })

      renderCreateSourcePage()
      
      // Fill and submit form
      await act(async () => {
        fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Source' } })
        fireEvent.change(screen.getByLabelText(/source uri/i), { target: { value: 'file://test/flags.json' } })
        fireEvent.click(screen.getByRole('button', { name: /create/i }))
      })
      
       // Should show loading state
       expect(screen.getByRole('button', { name: /creating/i })).toBeDisabled()
      expect(screen.getByLabelText(/name/i)).toBeDisabled()
      expect(screen.getByLabelText(/description/i)).toBeDisabled()
      expect(screen.getByLabelText(/source uri/i)).toBeDisabled()
      expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled()

       // Wait for async operation to complete
       await new Promise(resolve => setTimeout(resolve, 0))
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /create source/i })).not.toBeDisabled()
      })
    })

    test('should clear error on new submission', async () => {
      // First submission fails
      post.mockRejectedValueOnce(new Error('First error'))
      
      renderCreateSourcePage()
      
      // Submit and get error
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Source' } })
      fireEvent.change(screen.getByLabelText(/source uri/i), { target: { value: 'file://test/flags.json' } })
      fireEvent.click(screen.getByRole('button', { name: /create/i }))
      
      await waitFor(() => {
        expect(screen.getByText(/First error/i)).toBeInTheDocument()
      })

      // Second submission succeeds
      post.mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({ id: 'test-source-id' })
      })
      
      fireEvent.click(screen.getByRole('button', { name: /create/i }))
      
      await waitFor(() => {
        expect(screen.queryByText(/First error/i)).not.toBeInTheDocument()
      })
    })
  })

  describe('Navigation', () => {
    test('should navigate back on cancel', () => {
      renderCreateSourcePage()
      
      const cancelButton = screen.getByRole('button', { name: /cancel/i })
      fireEvent.click(cancelButton)
      
      expect(mockNavigate).toHaveBeenCalledWith('/')
    })

    test('should navigate back on sources breadcrumb click', () => {
      renderCreateSourcePage()
      
      const sourcesLink = screen.getByText('Sources')
      fireEvent.click(sourcesLink)
      
      expect(mockNavigate).toHaveBeenCalledWith('/')
    })

    test('should navigate to flags page after successful creation', async () => {
      post.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: 'test-source-id' })
      })

      renderCreateSourcePage()
      
      // Fill and submit form
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Source' } })
      fireEvent.change(screen.getByLabelText(/description/i), { target: { value: 'Test Description' } })
      fireEvent.change(screen.getByLabelText(/source uri/i), { target: { value: 'file://test/flags.json' } })
      fireEvent.click(screen.getByRole('button', { name: /create/i }))
      
      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/sources/test-source-id/flags')
      })
    })
  })

  describe('URI validation', () => {
    test('should accept valid file URI', async () => {
      post.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: 'test-source-id' })
      })

      renderCreateSourcePage()
      
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Source' } })
      fireEvent.change(screen.getByLabelText(/source uri/i), { 
        target: { value: 'file:///path/to/flags.json' } 
      })
      fireEvent.click(screen.getByRole('button', { name: /create source/i }))
      
      await waitFor(() => {
        expect(post).toHaveBeenCalledWith('/api/v1/sources', expect.objectContaining({
          uri: 'file:///path/to/flags.json'
        }))
      })
    })

    test('should accept relative file path', async () => {
      post.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ id: 'test-source-id' })
      })

      renderCreateSourcePage()
      
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Source' } })
      fireEvent.change(screen.getByLabelText(/source uri/i), { 
        target: { value: './flags.json' } 
      })
      fireEvent.click(screen.getByRole('button', { name: /create source/i }))
      
      await waitFor(() => {
        expect(post).toHaveBeenCalledWith('/api/v1/sources', expect.objectContaining({
          uri: './flags.json'
        }))
      })
    })
  })

  describe('Form reset and cleanup', () => {
    test('should handle error state cleanup', async () => {
      // First submission fails
      post.mockRejectedValueOnce(new Error('Error message'))
      
      renderCreateSourcePage()
      
      // Submit and get error
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Test Source' } })
      fireEvent.change(screen.getByLabelText(/source uri/i), { target: { value: 'file://test/flags.json' } })
      fireEvent.click(screen.getByRole('button', { name: /create/i }))
      
      await waitFor(() => {
        expect(screen.getByText(/Error message/i)).toBeInTheDocument()
      })

      // Clear error by changing input
      fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Updated Source' } })
      
      await waitFor(() => {
        // Error should still be there until new submission
        expect(screen.getByText(/Error message/i)).toBeInTheDocument()
      })
    })
  })
})