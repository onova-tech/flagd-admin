import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../../contexts/AuthContext"
import { get } from "../../services/api"
import "./SourceListPage.css"

function SourceListPage() {
  const [sources, setSources] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    fetchSources()
  }, [])

  const fetchSources = async () => {
    try {
      const response = await get('/api/v1/sources')
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      let data
      try {
        data = await response.json()
      } catch (jsonError) {
        console.error('JSON parsing error:', jsonError)
        data = []
      }
      if (Array.isArray(data)) {
        setSources(data)
      } else {
        console.error('Expected array but got:', typeof data, data)
        setError('Invalid response format from server')
      }
    } catch (err) {
      console.error('Fetch sources error:', err)
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleSourceClick = (sourceId) => {
    navigate(`/sources/${sourceId}/flags`)
  }

  const handleCreateSource = () => {
    navigate("/sources/new")
  }

  if (loading) {
    return (
<div className="source-selection-container">
        <header className="app-header">
          <div className="header-content">
            <h1>Feature Flag Sources</h1>
            <div className="header-actions">
              <span className="user-info">Welcome, {user?.username}</span>
              <button className="button button-secondary" onClick={logout}>
                Logout
              </button>
            </div>
          </div>
        </header>
        <div className="loading">Loading...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="source-selection-container">
        <header className="app-header">
          <h1>Sources</h1>
          <div className="header-actions">
            <button className="button button-primary" onClick={handleCreateSource}>New Source</button>
          </div>
        </header>
        <div className="error-message">Error: {error}</div>
      </div>
    )
  }

  return (
    <div className="source-selection-container">
      <header className="app-header">
        <h1>Sources</h1>
        <div className="header-actions">
          <button className="button button-primary" onClick={handleCreateSource}>New Source</button>
        </div>
      </header>
      <div className="sources-list">
        {sources.length === 0 ? (
          <div className="empty-state">
            <p>No sources found. Create your first source to get started.</p>
          </div>
        ) : (
          sources.map((source) => (
            <div
              key={source.id}
              className="source-item"
              onClick={() => handleSourceClick(source.id)}
            >
              <div className="source-info">
                <h3 className="source-name">{source.name}</h3>
                {source.description && (
                  <p className="source-description">{source.description}</p>
                )}
                {source.uri && (
                  <p className="source-uri">{source.uri}</p>
                )}
              </div>
              <div className="source-arrow">→</div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default SourceListPage
