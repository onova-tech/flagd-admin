import { describe, test, expect, vi, beforeEach } from 'vitest'
import { get, post, del, put } from './api'

// Mock the config module to avoid actual fetch calls
vi.mock('../config', () => ({
  getApiBaseUrl: vi.fn(() => Promise.resolve('http://localhost:9090'))
}))



describe('API Service', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Clear localStorage
    localStorage.clear()
  })

  describe('get method', () => {
    test('should make GET request with proper headers', async () => {
      // Given
      global.fetch = vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ data: 'test' })
        })
      )

      // When
      const response = await get('/test')

      // Then
      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:9090/test',
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json'
          }
        }
      )
      expect(response.ok).toBe(true)
      const data = await response.json()
      expect(data.data).toBe('test')
    })

    test('should include authorization header when token exists', async () => {
      // Given
      localStorage.setItem('accessToken', 'test-token')
      global.fetch = vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ data: 'test' })
        })
      )

      // When
      const response = await get('/test')

      // Then
      expect(fetch).toHaveBeenCalledTimes(1)
      const fetchCall = fetch.mock.calls[0]
      expect(fetchCall[0]).toContain('/test')
      expect(fetchCall[1]).toEqual(
        expect.objectContaining({
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer test-token'
          }
        })
      )
    })
  })

  describe('post method', () => {
    test('should make POST request with proper headers', async () => {
      // Given
      global.fetch = vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ success: true })
        })
      )

      // When
      const response = await post('/test', { data: 'value' })

      // Then
      expect(fetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ data: 'value' })
        })
      )
      expect(response.ok).toBe(true)
      const data = await response.json()
      expect(data.success).toBe(true)
    })
  })

  describe('del method', () => {
    test('should make DELETE request with proper headers', async () => {
      // Given
      global.fetch = vi.fn(() =>
        Promise.resolve({
          ok: true
        })
      )

      // When
      const response = await del('/test')

      // Then
      expect(fetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json'
          }
        })
      )
      expect(response.ok).toBe(true)
    })
  })

  describe('put method', () => {
    test('should make PUT request with proper headers', async () => {
      // Given
      global.fetch = vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ success: true })
        })
      )

      // When
      const response = await put('/test', { data: 'value' })

      // Then
      expect(fetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ data: 'value' })
        })
      )
      expect(response.ok).toBe(true)
      const data = await response.json()
      expect(data.success).toBe(true)
    })
  })

  describe('error handling', () => {
    test('should handle API error responses', async () => {
      // Given
      global.fetch = vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 500
        })
      )

      // When
      const response = await get('/test')

      // Then
      expect(response.ok).toBe(false)
      expect(response.status).toBe(500)
    })

    test('should handle JSON parsing errors', async () => {
      // Given
      global.fetch = vi.fn(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.reject(new Error('Invalid JSON'))
        })
      )

      // When & Then
      const response = await get('/test')
      await expect(response.json()).rejects.toThrow('Invalid JSON')
    })
  })
})