import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../../contexts/AuthContext'
import Login from './Login'
import { vi, beforeEach } from 'vitest'

// Mock useNavigate
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate
  }
})

describe('Login', () => {
  const renderLoginPage = () => {
    return render(
      <BrowserRouter>
        <AuthProvider>
          <Login />
        </AuthProvider>
      </BrowserRouter>
    )
  }

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    
    // Mock successful login response
    global.fetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token'
        })
      })
    )
  })

  describe('Form rendering', () => {
    test('should render login form', () => {
      renderLoginPage()
      
      expect(screen.getByText('Welcome to Flagd Admin')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('Enter your username')).toBeInTheDocument()
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
    })

    test('should have password field with correct attributes', () => {
      renderLoginPage()

      const passwordInput = screen.getByLabelText(/password/i)
      expect(passwordInput.type).toBe('password')
      expect(passwordInput.getAttribute('autoComplete')).toBe('current-password')
    })
  })

  describe('Form inputs', () => {
    test('should update username input', () => {
      renderLoginPage()
      
      const usernameInput = screen.getByLabelText(/username/i)
      fireEvent.change(usernameInput, { target: { value: 'testuser' } })

      expect(usernameInput.value).toBe('testuser')
    })

    test('should update password input', () => {
      renderLoginPage()
      
      const passwordInput = screen.getByLabelText(/password/i)
      fireEvent.change(passwordInput, { target: { value: 'testpass' } })

      expect(passwordInput.value).toBe('testpass')
    })

    test('should validate required fields', () => {
      renderLoginPage()
      
      const usernameInput = screen.getByLabelText(/username/i)
      const passwordInput = screen.getByLabelText(/password/i)
      
      expect(usernameInput.required).toBe(true)
      expect(passwordInput.required).toBe(true)
    })
  })

  describe('Form submission', () => {
    test('should call login with credentials', async () => {
      renderLoginPage()
      
      fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
      fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'testpass' } })
      fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledWith(
          expect.stringContaining('/api/v1/auth/login'),
          expect.objectContaining({
            method: 'POST',
            body: JSON.stringify({ username: 'testuser', password: 'testpass' })
          })
        )
      })
    })

    test('should navigate on successful login', async () => {
      renderLoginPage()
      
      fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
      fireEvent.change(screen.getByLabelText(/^password$/i), { target: { value: 'testpass' } })
      fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/')
      })
    })

    test('should show error on failed login', async () => {
      global.fetch = vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 401
        })
      )

      renderLoginPage()
      
      fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
      fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'testpass' } })
      fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

      await waitFor(() => {
        expect(screen.getByText(/Invalid username or password/)).toBeInTheDocument()
      })
    })

    test('should handle network errors', async () => {
      global.fetch = vi.fn(() =>
        Promise.reject(new Error('Network error'))
      )

      renderLoginPage()
      
      fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
      fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'testpass' } })
      fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

      await waitFor(() => {
        expect(screen.getByText(/Invalid username or password/)).toBeInTheDocument()
      })
    })
  })

  describe('Form state', () => {
    test('should disable submit button during submission', async () => {
      // Mock a slow login request
      global.fetch = vi.fn(() => new Promise(resolve => setTimeout(() => resolve({
        ok: true,
        json: () => Promise.resolve({
          accessToken: 'test-access-token',
          refreshToken: 'test-refresh-token'
        })
      }), 100)))

      renderLoginPage()
      
      const submitButton = screen.getByRole('button', { name: /sign in/i })
      
      fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'testuser' } })
      fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'testpass' } })
      fireEvent.click(submitButton)

      // Button should be disabled immediately after click
      expect(submitButton.disabled).toBe(true)
      
      // Wait for request to complete
      await waitFor(() => {
        expect(submitButton.disabled).toBe(false)
      })
    })
  })
})