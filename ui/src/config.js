export const getApiBaseUrl = async () => {
  try {
    const response = await fetch('/config.json')
    const config = await response.json()
    return config.apiBaseUrl || ''
  } catch (error) {
    console.warn('Failed to load config.json, using default:', error)
    return 'http://localhost:9090'
  }
}

export const getEnvVar = async (key, defaultValue = '') => {
  try {
    const response = await fetch('/config.json')
    const config = await response.json()
    return config[key] || defaultValue
  } catch (error) {
    console.warn(`Failed to load config.${key}, using default:`, error)
    return defaultValue
  }
}
