import { useState, useEffect } from "react"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../../contexts/AuthContext"
import { get, del } from "../../services/api"
import "./SourceListPage.css"

function SourceListPage() {
  const [sources, setSources] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deleteConfirmations, setDeleteConfirmations] = useState({})
  const [deletingIds, setDeletingIds] = useState({})
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

  const handleDeleteSource = async (sourceId, event) => {
    event.stopPropagation()
    
    if (deleteConfirmations[sourceId]) {
      // Perform deletion
      try {
        setDeletingIds(prev => ({ ...prev, [sourceId]: true }))
        const response = await del(`/api/v1/sources/${sourceId}`)
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        // Remove from state
        setSources(prev => prev.filter(source => source.id !== sourceId))
        setDeleteConfirmations(prev => ({ ...prev, [sourceId]: false }))
      } catch (err) {
        console.error('Delete source error:', err)
        setError(`Failed to delete source: ${err.message}`)
      } finally {
        setDeletingIds(prev => ({ ...prev, [sourceId]: false }))
      }
    } else {
      // Show confirmation
      setDeleteConfirmations(prev => ({ ...prev, [sourceId]: true }))
      // Auto-hide confirmation after 3 seconds
      setTimeout(() => {
        setDeleteConfirmations(prev => ({ ...prev, [sourceId]: false }))
      }, 3000)
    }
  }

  const handleEditSource = (sourceId, event) => {
    event.stopPropagation()
    navigate(`/sources/${sourceId}/edit`)
  }

  const handleDownloadSource = async (sourceId, sourceName, event) => {
    event.stopPropagation()
    
    try {
      const response = await get(`/api/v1/sources/${sourceId}/contents`)
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      const content = data.content
      
      // Format content as JSON
      let formattedContent
      try {
        // If content is already JSON, parse and stringify it for proper formatting
        const parsedContent = JSON.parse(content)
        formattedContent = JSON.stringify(parsedContent, null, 2)
      } catch (e) {
        // If content is not valid JSON, wrap it in a JSON object
        formattedContent = JSON.stringify({ content: content }, null, 2)
      }
      
      // Create blob and trigger download
      const blob = new Blob([formattedContent], { type: 'application/json' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${sourceName}.json`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    } catch (err) {
      console.error('Download source error:', err)
      setError(`Failed to download source: ${err.message}`)
    }
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
                <div className="item-actions">
                  <button
                    className="button button-small button-download"
                    onClick={(e) => handleDownloadSource(source.id, source.name, e)}
                    title="Download source content"
                  >
                    <span className="material-icons">download</span>
                  </button>
                  <button
                    className="button button-small button-edit"
                    onClick={(e) => handleEditSource(source.id, e)}
                    title="Edit source"
                  >
                    <span className="material-icons">settings</span>
                  </button>
                 {deleteConfirmations[source.id] ? (
                   <button
                     className="button button-small button-delete-confirm"
                     onClick={(e) => handleDeleteSource(source.id, e)}
                     disabled={deletingIds[source.id]}
                     title="Confirm delete"
                   >
                     {deletingIds[source.id] ? '...' : 'confirm?'}
                   </button>
                 ) : (
                   <button
                     className="button button-small button-delete"
                     onClick={(e) => handleDeleteSource(source.id, e)}
                     disabled={deletingIds[source.id]}
                     title="Delete source"
                   >
                     <span className="material-icons">delete</span>
                   </button>
                 )}
                 <div className="source-arrow">→</div>
               </div>
             </div>
           ))
        )}
      </div>
    </div>
  )
}

export default SourceListPage
