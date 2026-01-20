import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { getApiBaseUrl } from "./config"
import "./SourceSelection.css"

function SourceSelection() {
  const [sources, setSources] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [apiBaseUrl, setApiBaseUrl] = useState('http://localhost:9090')
  const navigate = useNavigate()

  useEffect(() => {
    getApiBaseUrl().then((url) => {
      setApiBaseUrl(url)
      fetchSources(url)
    }).catch(() => {
      fetchSources('http://localhost:9090')
    })
  }, [])

  const fetchSources = async (baseUrl = apiBaseUrl) => {
    try {
      const response = await fetch(`${baseUrl}/api/v1/sources`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const data = await response.json()
      setSources(data)
    } catch (err) {
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
          <h1>Sources</h1>
          <div className="header-actions"></div>
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

export default SourceSelection
