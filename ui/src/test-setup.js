import { vi } from 'vitest'
import '@testing-library/jest-dom'

// Create a mock localStorage store
const localStorageStore = {}

// Mock localStorage
Object.defineProperty(window, 'localStorage', {
  value: {
    getItem: vi.fn((key) => localStorageStore[key] || null),
    setItem: vi.fn((key, value) => {
      localStorageStore[key] = value
    }),
    removeItem: vi.fn((key) => {
      delete localStorageStore[key]
    }),
    clear: vi.fn(() => {
      Object.keys(localStorageStore).forEach(key => delete localStorageStore[key])
    }),
  },
  writable: true,
})

// Mock fetch
Object.defineProperty(global, 'fetch', {
  value: vi.fn(),
  writable: true,
})

// Mock window.location.href
Object.defineProperty(window, 'location', {
  value: {
    href: '',
  },
  writable: true,
})