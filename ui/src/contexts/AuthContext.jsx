import { createContext, useContext, useState, useEffect } from 'react'
import { getApiBaseUrl } from '../config'
import PropTypes from 'prop-types'

/* eslint-disable react-refresh/only-export-components */

const AuthContext = createContext()

export const useAuth = () => {
  return useContext(AuthContext)
}

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null)
  const [accessToken, setAccessToken] = useState(null)
  const [refreshToken, setRefreshToken] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const savedAccessToken = localStorage.getItem('accessToken')
    const savedRefreshToken = localStorage.getItem('refreshToken')
    const savedUser = localStorage.getItem('user')

    if (savedAccessToken && savedRefreshToken && savedUser) {
      setAccessToken(savedAccessToken)
      setRefreshToken(savedRefreshToken)
      setUser(JSON.parse(savedUser))
    }
    setLoading(false)
  }, [])

  const login = async (username, password) => {
    try {
      const apiBaseUrl = await getApiBaseUrl()
      const response = await fetch(`${apiBaseUrl}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      })

      if (!response.ok) {
        throw new Error('Login failed')
      }

      const data = await response.json()
      setAccessToken(data.accessToken)
      setRefreshToken(data.refreshToken)
      setUser({ username })

      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('user', JSON.stringify({ username }))

      return true
    } catch (error) {
      console.error('Login error:', error)
      throw error
    }
  }

  const refreshAccessToken = async () => {
    try {
      const apiBaseUrl = await getApiBaseUrl()
      const response = await fetch(`${apiBaseUrl}/api/v1/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      })

      if (!response.ok) {
        logout()
        throw new Error('Token refresh failed')
      }

      const data = await response.json()
      setAccessToken(data.accessToken)
      setRefreshToken(data.refreshToken)

      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)

      return data.accessToken
    } catch (error) {
      console.error('Token refresh error:', error)
      throw error
    }
  }

  const logout = async () => {
    try {
      if (refreshToken) {
        const apiBaseUrl = await getApiBaseUrl()
        await fetch(`${apiBaseUrl}/api/v1/auth/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        })
      }
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      setAccessToken(null)
      setRefreshToken(null)
      setUser(null)
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
    }
  }

  const value = {
    user,
    accessToken,
    refreshToken,
    loading,
    login,
    logout,
    refreshAccessToken
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

AuthProvider.propTypes = {
  children: PropTypes.node.isRequired
}