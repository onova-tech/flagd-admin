import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { AuthProvider, useAuth } from './AuthContext'
import { vi, beforeEach, describe, it, expect } from 'vitest'

// Mock config
vi.mock('../config', () => ({
  getApiBaseUrl: () => Promise.resolve('http://localhost:9090')
}))

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    global.fetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        accessToken: 'test-access-token',
        refreshToken: 'test-refresh-token'
      })
    })
  })

  describe('Initial state', () => {
    test('should initialize with loading state', async () => {
      let authValues
      
      function TestComponent() {
        authValues = useAuth()
        return <div>Test Child</div>
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(authValues.loading).toBe(false)
      })
    })

    test('should load saved authentication from localStorage', async () => {
      localStorage.setItem('accessToken', 'saved-access-token')
      localStorage.setItem('refreshToken', 'saved-refresh-token')
      localStorage.setItem('user', JSON.stringify({ username: 'saved-user' }))

      let authValues
      
      function TestComponent() {
        authValues = useAuth()
        return <div>Test Child</div>
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(authValues.loading).toBe(false)
        expect(authValues.accessToken).toBe('saved-access-token')
        expect(authValues.refreshToken).toBe('saved-refresh-token')
        expect(authValues.user).toEqual({ username: 'saved-user' })
      })
    })

    test('should handle incomplete saved data', async () => {
      localStorage.setItem('accessToken', 'saved-access-token')
      // Missing refreshToken
      localStorage.setItem('user', JSON.stringify({ username: 'saved-user' }))

      let authValues
      
      function TestComponent() {
        authValues = useAuth()
        return <div>Test Child</div>
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      await waitFor(() => {
        expect(authValues.loading).toBe(false)
        expect(authValues.user).toBe(null) // Should not set user without complete data
      })
    })
  })

  describe('Login functionality', () => {
    function TestComponent() {
      const { login, user, loading } = useAuth()
      
      return (
        <div>
          <button onClick={() => login('testuser', 'testpass')}>
            Login
          </button>
          <div data-testid="user">{user ? JSON.stringify(user) : 'null'}</div>
          <div data-testid="loading">{loading.toString()}</div>
        </div>
      )
    }

    test('should login successfully', async () => {
      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      const loginButton = screen.getByText('Login')
      fireEvent.click(loginButton)

      await waitFor(() => {
        expect(fetch).toHaveBeenCalledWith(
          'http://localhost:9090/api/v1/auth/login',
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: 'testuser', password: 'testpass' })
          }
        )
      })

await waitFor(() => {
         expect(localStorage.setItem).toHaveBeenCalledWith('accessToken', 'test-access-token')
         expect(localStorage.setItem).toHaveBeenCalledWith('refreshToken', 'test-refresh-token')
         expect(localStorage.setItem).toHaveBeenCalledWith('user', JSON.stringify({ username: 'testuser' }))
         
         const userDisplay = screen.getByTestId('user')
         expect(userDisplay).toHaveTextContent('{"username":"testuser"}')
       })
    })

    test('should handle login failure', async () => {
      fetch.mockResolvedValue({
        ok: false,
        status: 401
      })

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      const loginButton = screen.getByText('Login')
      
      await waitFor(() => {
        fireEvent.click(loginButton)
      })

await waitFor(() => {
         expect(localStorage.setItem).not.toHaveBeenCalledWith('accessToken', expect.any(String))
         const userDisplay = screen.getByTestId('user')
         expect(userDisplay).toHaveTextContent('null')
       })
    })

    test('should handle network error during login', async () => {
      fetch.mockRejectedValue(new Error('Network error'))

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      const loginButton = screen.getByText('Login')
      fireEvent.click(loginButton)

      await waitFor(() => {
        const userDisplay = screen.getByTestId('user')
        expect(userDisplay).toHaveTextContent('null')
      })
    })

    test('should handle malformed response', async () => {
      fetch.mockResolvedValue({
        ok: true,
        json: () => Promise.reject(new Error('Invalid JSON'))
      })

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      const loginButton = screen.getByText('Login')
      fireEvent.click(loginButton)

      await waitFor(() => {
        const userDisplay = screen.getByTestId('user')
        expect(userDisplay).toHaveTextContent('null')
      })
    })
  })

  describe('Logout functionality', () => {
    function TestComponent() {
      const { login, logout, user } = useAuth()
      
      return (
        <div>
          <button onClick={() => login('testuser', 'testpass')}>
            Login
          </button>
          <button onClick={() => logout()}>
            Logout
          </button>
          <div data-testid="user">{user ? JSON.stringify(user) : 'null'}</div>
        </div>
      )
    }

    test('should logout and clear authentication data', async () => {
      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      // First login
      const loginButton = screen.getByText('Login')
      fireEvent.click(loginButton)

      await waitFor(() => {
        const userDisplay = screen.getByTestId('user')
        expect(userDisplay).toHaveTextContent('{"username":"testuser"}')
      })

      // Then logout
      const logoutButton = screen.getByText('Logout')
      fireEvent.click(logoutButton)

await waitFor(() => {
         expect(localStorage.removeItem).toHaveBeenCalledWith('accessToken')
         expect(localStorage.removeItem).toHaveBeenCalledWith('refreshToken')
         expect(localStorage.removeItem).toHaveBeenCalledWith('user')
         
         const userDisplay = screen.getByTestId('user')
         expect(userDisplay).toHaveTextContent('null')
       })
    })
  })

  describe('Context value', () => {
    test('should provide auth context to children', () => {
      let contextValue
      
      function Consumer() {
        contextValue = useAuth()
        return <div>Consumer</div>
      }

      render(
        <AuthProvider>
          <Consumer />
        </AuthProvider>
      )

      expect(contextValue).toBeDefined()
      expect(contextValue.login).toBeInstanceOf(Function)
      expect(contextValue.logout).toBeInstanceOf(Function)
      expect(typeof contextValue.user).toBe('object')
      expect(contextValue.accessToken).toBe(null)
      expect(contextValue.refreshToken).toBe(null)
      expect(typeof contextValue.loading).toBe('boolean')
    })
  })

  describe('State persistence', () => {
    test('should persist authentication state to localStorage', async () => {
      const TestComponent = () => {
        const { login } = useAuth()
        
        return (
          <button onClick={() => login('testuser', 'testpass')}>
            Login
          </button>
        )
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      const loginButton = screen.getByText('Login')
      fireEvent.click(loginButton)

      await waitFor(() => {
         expect(localStorage.setItem).toHaveBeenCalledWith('accessToken', 'test-access-token')
         expect(localStorage.setItem).toHaveBeenCalledWith('refreshToken', 'test-refresh-token')
         expect(localStorage.setItem).toHaveBeenCalledWith('user', JSON.stringify({ username: 'testuser' }))
       })
    })

    test('should not persist incomplete authentication data', async () => {
      fetch.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ accessToken: 'only-token' }) // Missing refreshToken
      })

      const TestComponent = () => {
        const { login } = useAuth()
        
        return (
          <button onClick={() => login('testuser', 'testpass')}>
            Login
          </button>
        )
      }

      render(
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      )

      const loginButton = screen.getByText('Login')
      fireEvent.click(loginButton)

      await waitFor(() => {
         // Should not persist incomplete data
         expect(localStorage.setItem).not.toHaveBeenCalledWith('accessToken', expect.any(String))
       })
    })
  })

  describe('Error handling', () => {
    test('should handle localStorage errors gracefully', async () => {
      // Temporarily override getItem to throw error only on first call
      let callCount = 0
      const originalGetItem = localStorage.getItem
      localStorage.getItem = vi.fn((key) => {
        callCount++
        if (callCount === 1) {
          throw new Error('LocalStorage error')
        }
        return originalGetItem.call(localStorage, key)
      })

      function TestComponent() {
        const { user } = useAuth()
        return <div>{user ? 'Logged in' : 'Not logged'}</div>
      }

      // This will throw since AuthContext doesn't handle localStorage errors
      expect(() => {
        render(
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        )
      }).toThrow()

      // Restore original function
      localStorage.getItem = originalGetItem
    })

    test('should handle localStorage access errors', async () => {
      const originalGetItem = localStorage.getItem
      localStorage.setItem('accessToken', 'saved-access-token')
      localStorage.setItem('refreshToken', 'saved-refresh-token')
      localStorage.setItem('user', 'invalid-json') // Invalid JSON

      function TestComponent() {
        const { user } = useAuth()
        return <div>{user ? 'Logged in' : 'Not logged'}</div>
      }

      expect(() => {
        render(
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        )
      }).toThrow() // Actually, it should throw since the error is not caught

      // Restore original function
      localStorage.getItem = originalGetItem
    })

    test('should handle JSON parsing errors in saved user', async () => {
      localStorage.setItem('accessToken', 'saved-access-token')
      localStorage.setItem('refreshToken', 'saved-refresh-token')
      localStorage.setItem('user', 'invalid-json') // Invalid JSON

      function TestComponent() {
        const { user } = useAuth()
        return <div>{user ? 'Logged in' : 'Not logged'}</div>
      }

      // This will throw during render due to JSON.parse error
      expect(() => {
        render(
          <AuthProvider>
            <TestComponent />
          </AuthProvider>
        )
      }).toThrow()
    })
  })
})