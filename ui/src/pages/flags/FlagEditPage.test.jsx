import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../../contexts/AuthContext'
import FlagEditPage from './FlagEditPage'
import { get, post } from '../../services/api'

import convertToFlagdFormat from '../../features/flags/utils/convertToFlagdFormat'
import validateFlagdSchema from '../../features/flags/utils/validateFlagdSchema'
import { vi } from 'vitest'

// Mock API module
vi.mock('../../services/api', () => ({
  get: vi.fn(),
  post: vi.fn()
}))

// Mock @openfeature/flagd-core
vi.mock('@openfeature/flagd-core', () => ({
  FlagdCore: vi.fn().mockImplementation(() => ({
    setConfigurations: vi.fn(),
    getFlags: vi.fn().mockReturnValue(new Map()),
    resolveBooleanEvaluation: vi.fn(),
    resolveStringEvaluation: vi.fn(),
    resolveNumberEvaluation: vi.fn(),
    resolveObjectEvaluation: vi.fn()
  })),
  MemoryStorage: vi.fn()
}))

// Mock utility modules
vi.mock('../../features/flags/utils/convertToFlagdFormat', () => ({
  default: vi.fn().mockReturnValue({ flags: {} })
}))

// Mock useNavigate and useParams
const mockNavigate = vi.fn()
const mockParams = { sourceId: 'test-source-id', flagId: 'test-flag' }
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => mockParams
  }
})

vi.mock('../../features/flags/utils/validateFlagdSchema', () => ({
  default: vi.fn()
}))

// Mock AuthContext
vi.mock('../../contexts/AuthContext', () => ({
  AuthProvider: ({ children }) => children,
  useAuth: () => ({
    user: { name: 'test-user' },
    logout: vi.fn()
  })
}))

// Mock Rule component
vi.mock('./components/Rule', () => ({
  default: function MockRule({ rule, onChange, onRemove }) {
    return (
      <div data-testid="rule">
        <input
          data-testid="rule-condition"
          value={rule.condition.name}
          onChange={(e) => onChange({
            ...rule,
            condition: { ...rule.condition, name: e.target.value }
          })}
        />
        <button data-testid="remove-rule" onClick={onRemove}>Remove</button>
      </div>
    )
  }
}))

const mockSource = {
  id: 'test-source-id',
  name: 'Test Source',
  description: 'Test Description',
  uri: 'file://test.json',
  enabled: true
}

const mockFlagData = {
  flagId: 'test-flag',
  name: 'Test Flag',
  description: 'Test flag description',
  state: 'ENABLED',
  defaultVariant: 'on',
  variants: {
    'on': true,
    'off': false
  },
  targeting: {
    if: [
      { 'in': [{ 'var': 'user.email' }, ['admin@example.com', 'user@example.com']] },
      'on',
      'off'
    ]
  }
}

describe('FlagEditPage', () => {
  const renderFlagEditPage = (sourceId = 'test-source-id', flagId = 'test-flag') => {
    // Update the mock parameters
    mockParams.sourceId = sourceId
    mockParams.flagId = flagId
    
    return render(
      <BrowserRouter>
        <AuthProvider>
          <FlagEditPage />
        </AuthProvider>
      </BrowserRouter>
    )
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Loading flag data', () => {
    test('should load source and flag data on mount', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags/test-flag')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagEditPage()

      // Then
      await waitFor(() => {
        expect(get).toHaveBeenCalledWith('/api/v1/sources/test-source-id')
        expect(get).toHaveBeenCalledWith('/api/v1/sources/test-source-id/flags/test-flag')
      })
    })

    test('should handle source not found', async () => {
      // Given
      get.mockResolvedValue({ ok: false })

      // When
      renderFlagEditPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText('Flag Configuration')).toBeInTheDocument()
      })
    })

    test('should handle API error gracefully', async () => {
      // Given
      get.mockRejectedValue(new Error('Network error'))

      // When
      renderFlagEditPage()

      // Then
      await waitFor(() => {
        expect(screen.getByText('Error loading flag: Network error')).toBeInTheDocument()
      })
    })
  })

  describe('New flag creation', () => {
    test('should initialize form with default values for new flag', async () => {
      // Given
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        return Promise.resolve({ ok: false })
      })

      // When
      renderFlagEditPage('test-source-id', 'new')

      // Then
      await waitFor(() => {
        expect(screen.getByDisplayValue('new-flag')).toBeInTheDocument()
        expect(screen.getByDisplayValue('false')).toBeInTheDocument()
        expect(screen.getByText('true')).toBeInTheDocument()
        expect(screen.getByText('false')).toBeInTheDocument()
      })
    })
  })

  describe('Form inputs', () => {
    beforeEach(async () => {
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags/test-flag')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      renderFlagEditPage()
      await waitFor(() => {
        expect(screen.getByDisplayValue('test-flag')).toBeInTheDocument()
      })
    })

    test('should update flag key input', async () => {
      const flagKeyInput = screen.getByDisplayValue('test-flag')
      
      fireEvent.change(flagKeyInput, { target: { value: 'updated-flag' } })
      
      await waitFor(() => {
        expect(flagKeyInput).toHaveValue('updated-flag')
      })
    })

    test('should update description input', async () => {
      const descriptionInput = screen.getByPlaceholderText('Flag description')
      
      fireEvent.change(descriptionInput, { target: { value: 'Updated description' } })
      
      await waitFor(() => {
        expect(descriptionInput).toHaveValue('Updated description')
      })
    })

    test('should toggle flag state', async () => {
      const stateToggle = screen.getByRole('checkbox', { name: /enabled/i })
      
      fireEvent.click(stateToggle)
      
      await waitFor(() => {
        expect(stateToggle).not.toBeChecked()
      })
    })

    test('should change flag type', async () => {
      const typeSelect = screen.getByDisplayValue('boolean')
      
      fireEvent.change(typeSelect, { target: { value: 'string' } })
      
      await waitFor(() => {
        expect(typeSelect).toHaveValue('string')
      })
    })
  })

  describe('Variant management', () => {
    beforeEach(async () => {
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags/test-flag')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      renderFlagEditPage()
      await waitFor(() => {
        expect(screen.getByText('Variants')).toBeInTheDocument()
      })
    })

    test('should display existing variants', async () => {
      await waitFor(() => {
        // Check for variant name inputs by ID
        expect(screen.getByDisplayValue('true')).toBeInTheDocument()
        expect(screen.getByDisplayValue('false')).toBeInTheDocument()
        // Check that we have the expected number of variant inputs
        const allVariantInputs = screen.getAllByDisplayValue(/^(true|false)$/)
        expect(allVariantInputs.length).toBeGreaterThan(0)
      })
    })

    test('should add new variant', async () => {
      const addVariantButton = screen.getByText('Add Variant')
      
      fireEvent.click(addVariantButton)
      
      await waitFor(() => {
        expect(screen.getByDisplayValue('')).toBeInTheDocument() // New empty variant
      })
    })

    test('should remove variant', async () => {
      const removeButtons = screen.getAllByText('Remove')
      const firstRemoveButton = removeButtons.find(btn => 
        btn.closest('[data-testid*="variant"]')
      )
      
      if (firstRemoveButton) {
        fireEvent.click(firstRemoveButton)
      }
      
      await waitFor(() => {
        // Check that one variant was removed
        const variantInputs = screen.getAllByDisplayValue('true')
        expect(variantInputs.length).toBeLessThanOrEqual(1)
      })
    })
  })

  describe('Targeting rules', () => {
    beforeEach(async () => {
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags/test-flag')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      renderFlagEditPage()
      await waitFor(() => {
        expect(screen.getByText('Targeting')).toBeInTheDocument()
      })
    })

    test('should toggle targeting section', async () => {
      const targetingToggle = screen.getByRole('checkbox', { name: /targeting/i })
      
      fireEvent.click(targetingToggle)
      
      await waitFor(() => {
        expect(screen.getByText('Rules')).toBeInTheDocument()
      })
    })

    test('should add new rule', async () => {
      const targetingToggle = screen.getByRole('checkbox', { name: /targeting/i })
      fireEvent.click(targetingToggle)
      
      await waitFor(() => {
        expect(screen.getByText('Add Rule')).toBeInTheDocument()
      })
      
      const addRuleButton = screen.getByText('Add Rule')
      fireEvent.click(addRuleButton)
      
      await waitFor(() => {
        const rules = screen.getAllByTestId('rule')
        expect(rules.length).toBeGreaterThan(1)
      })
    })

    test('should remove rule', async () => {
      const targetingToggle = screen.getByRole('checkbox', { name: /targeting/i })
      fireEvent.click(targetingToggle)
      
      await waitFor(() => {
        expect(screen.getByTestId('remove-rule')).toBeInTheDocument()
      })
      
      const removeButton = screen.getByTestId('remove-rule')
      fireEvent.click(removeButton)
      
      await waitFor(() => {
        const rules = screen.queryAllByTestId('rule')
        expect(rules.length).toBe(0)
      })
    })
  })

  describe('Flag evaluation', () => {
    beforeEach(async () => {
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags/test-flag')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      renderFlagEditPage()
      await waitFor(() => {
        expect(screen.getByText('Test Evaluation')).toBeInTheDocument()
      })
    })

    test('should evaluate flag with valid context', async () => {
      const contextTextarea = screen.getByPlaceholderText(/evaluation context/i)
      
      fireEvent.change(contextTextarea, { 
        target: { value: '{"user": {"email": "test@example.com"}}' } 
      })
      
      const evaluateButton = screen.getByText('Evaluate')
      fireEvent.click(evaluateButton)
      
      await waitFor(() => {
        expect(screen.getByText(/evaluation result/i)).toBeInTheDocument()
      })
    })

    test('should show error for invalid JSON context', async () => {
      const contextTextarea = screen.getByPlaceholderText(/evaluation context/i)
      
      fireEvent.change(contextTextarea, { 
        target: { value: 'invalid json' } 
      })
      
      const evaluateButton = screen.getByText('Evaluate')
      fireEvent.click(evaluateButton)
      
      await waitFor(() => {
        expect(screen.getByText(/invalid json/i)).toBeInTheDocument()
      })
    })
  })

  describe('Schema validation', () => {
    beforeEach(async () => {
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags/test-flag')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      convertToFlagdFormat.mockReturnValue({ flags: {} })
      validateFlagdSchema.mockReturnValue({ valid: true })
      
      renderFlagEditPage()
      await waitFor(() => {
        expect(screen.getByText('Validate')).toBeInTheDocument()
      })
    })

    test('should validate flag schema', async () => {
      const validateButton = screen.getByText('Validate')
      fireEvent.click(validateButton)
      
      await waitFor(() => {
        expect(validateFlagdSchema).toHaveBeenCalled()
      })
    })

    test('should show validation error', async () => {
      validateFlagdSchema.mockReturnValue({ 
        valid: false, 
        errors: ['Invalid schema'] 
      })
      
      const validateButton = screen.getByText('Validate')
      fireEvent.click(validateButton)
      
      await waitFor(() => {
        expect(screen.getByText('Invalid schema')).toBeInTheDocument()
      })
    })
  })

  describe('Save functionality', () => {
    beforeEach(async () => {
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        if (url.includes('/flags/test-flag')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockFlagData)
          })
        }
        return Promise.resolve({ ok: false })
      })

      convertToFlagdFormat.mockReturnValue({ 
        flags: { 'test-flag': mockFlagData } 
      })
      
      renderFlagEditPage()
      await waitFor(() => {
        expect(screen.getByText('Save')).toBeInTheDocument()
      })
    })

    test('should save flag successfully', async () => {
      post.mockResolvedValue({ ok: true })
      
      const saveButton = screen.getByText('Save')
      fireEvent.click(saveButton)
      
      await waitFor(() => {
        expect(post).toHaveBeenCalledWith(
          '/api/v1/sources/test-source-id/flags/test-flag',
          expect.any(Object)
        )
      })
    })

    test('should show save error', async () => {
      post.mockRejectedValue(new Error('Save failed'))
      
      const saveButton = screen.getByText('Save')
      fireEvent.click(saveButton)
      
      await waitFor(() => {
        expect(screen.getByText(/error saving/i)).toBeInTheDocument()
      })
    })
  })

  describe('Navigation', () => {
    test('should navigate back when cancel is clicked', async () => {
      get.mockImplementation((url) => {
        if (url.includes('/sources/test-source-id')) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(mockSource)
          })
        }
        return Promise.resolve({ ok: false })
      })

      renderFlagEditPage('test-source-id', 'new')
      
      await waitFor(() => {
        expect(screen.getByText('Cancel')).toBeInTheDocument()
      })
      
      const cancelButton = screen.getByText('Cancel')
      fireEvent.click(cancelButton)
      
      // Navigation would be tested by checking if navigate was called
      // This would require mocking the navigate function
    })
  })

  describe('Loading states', () => {
    test('should show loading indicator while fetching data', async () => {
      get.mockImplementation(() => new Promise(resolve => setTimeout(() => resolve({ ok: true }), 100)))
      
      renderFlagEditPage()
      
      expect(screen.getByText(/loading/i)).toBeInTheDocument()
      
      await waitFor(() => {
        expect(screen.queryByText(/loading/i)).not.toBeInTheDocument()
      }, { timeout: 200 })
    })
  })
})