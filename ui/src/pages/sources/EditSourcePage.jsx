import { useState, useEffect } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { get, patch } from "../../services/api"

function EditSourcePage() {
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [uri, setUri] = useState("")
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const [fetchLoading, setFetchLoading] = useState(true)
  const { sourceId } = useParams()
  const navigate = useNavigate()

  useEffect(() => {
    const fetchSource = async () => {
      try {
        const response = await get(`/api/v1/sources/${sourceId}`)
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        const source = await response.json()
        setName(source.name || "")
        setDescription(source.description || "")
        setUri(source.uri || "")
      } catch (err) {
        console.error('Fetch source error:', err)
        setError(`Failed to fetch source: ${err.message}`)
      } finally {
        setFetchLoading(false)
      }
    }

    if (sourceId) {
      fetchSource()
    }
  }, [sourceId])

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    setLoading(true)
    setError(null)

    const requestBody = {
      name,
      description: description || "",
      enabled: true
    }

    try {
      const response = await patch(`/api/v1/sources/${sourceId}`, requestBody)

      if (!response.ok) {
        let errorText
        try {
          errorText = await response.text()
        } catch {
          errorText = "Unknown error"
        }
        throw new Error(`HTTP error! status: ${response.status}, ${errorText}`)
      }

      let source
      try {
        source = await response.json()
      } catch {
        throw new Error("Invalid response from server")
      }
      navigate(`/sources/${source.id}/flags`)
    } catch (err) {
      console.error('Source update error:', err)
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = () => {
        navigate("/")
  }

  const handleBackToSources = () => {
    navigate("/")
  }

  if (fetchLoading) {
    return (
      <div className="app-container">
        <header className="app-header">
          <div className="header-breadcrumb">
            <button className="breadcrumb-link" onClick={handleBackToSources}>Sources</button>
            <span className="breadcrumb-separator">/</span>
            <span className="breadcrumb-current">Loading...</span>
          </div>
          <div className="header-actions"></div>
        </header>
        <div className="loading">Loading source data...</div>
      </div>
    )
  }

  if (error && !loading) {
    return (
      <div className="app-container">
        <header className="app-header">
          <div className="header-breadcrumb">
            <button className="breadcrumb-link" onClick={handleBackToSources}>Sources</button>
            <span className="breadcrumb-separator">/</span>
            <span className="breadcrumb-current">Error</span>
          </div>
          <div className="header-actions">
            <button className="button button-secondary" onClick={handleCancel}>Back</button>
          </div>
        </header>
        <div className="error-message">Error: {error}</div>
      </div>
    )
  }

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-breadcrumb">
          <button className="breadcrumb-link" onClick={handleBackToSources}>Sources</button>
          <span className="breadcrumb-separator">/</span>
          <span className="breadcrumb-current">Edit Source</span>
        </div>
        <div className="header-actions">
          <button className="button button-secondary" onClick={handleCancel} disabled={loading}>Cancel</button>
        </div>
      </header>
      <div className="app-layout">
        <div className="form-panel">
          <form onSubmit={handleSubmit} className="source-form">
            <div className="form-section">
              <span className="section-header">Source Configuration</span>
              <div className="form-group">
                <label htmlFor="name" className="form-label">Name</label>
                <input
                  id="name"
                  className="input"
                  placeholder="Enter source name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  disabled={loading}
                />
              </div>
              <div className="form-group">
                <label htmlFor="description" className="form-label">Description</label>
                <textarea
                  id="description"
                  className="input"
                  placeholder="Enter source description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={3}
                  disabled={loading}
                  style={{
                    fontFamily: 'var(--font-family-base)',
                    fontSize: 'var(--font-size-base)',
                    resize: 'vertical',
                    minHeight: '80px'
                  }}
                />
              </div>
              <div className="form-group">
                <label htmlFor="uri" className="form-label">Source URI</label>
                <input
                  id="uri"
                  className="input"
                  placeholder="file:///path/to/flags.json"
                  value={uri}
                  readOnly
                  disabled={true}
                  style={{ backgroundColor: '#f5f5f5', cursor: 'not-allowed' }}
                />
                <small style={{ color: '#666', fontSize: '0.875em', marginTop: '4px', display: 'block' }}>
                  Note: The source URI cannot be modified. To change the URI, delete this source and create a new one.
                </small>
              </div>
            </div>
            {error && (
              <div className="error-message">{error}</div>
            )}
            <div className="form-actions">
              <button
                type="submit"
                className="button button-primary"
                disabled={loading || !name.trim()}
              >
                {loading ? 'Updating...' : 'Update Source'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

export default EditSourcePage