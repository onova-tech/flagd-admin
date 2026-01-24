import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { post } from "../../services/api"

function CreateSourcePage() {
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [uri, setUri] = useState("")
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    setLoading(true)
    setError(null)

    const requestBody = {
      name,
      description: description || null,
      uri
    }

    try {
      const response = await post(`/api/v1/sources`, requestBody)

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
      console.error('Source creation error:', err)
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

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-breadcrumb">
          <button className="breadcrumb-link" onClick={handleBackToSources}>Sources</button>
          <span className="breadcrumb-separator">/</span>
          <span className="breadcrumb-current">New Source</span>
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
                  onChange={(e) => setUri(e.target.value)}
                  required
                  disabled={loading}
                />
              </div>
            </div>
            {error && (
              <div className="error-message">{error}</div>
            )}
            <div className="form-actions">
              <button
                type="submit"
                className="button button-primary"
                disabled={loading || !name.trim() || !uri.trim()}
              >
                {loading ? 'Creating...' : 'Create Source'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}

export default CreateSourcePage
