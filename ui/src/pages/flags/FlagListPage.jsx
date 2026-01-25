import { useState, useEffect, useCallback } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { get, del } from "../../services/api"
import "./FlagListPage.css"

function FlagListPage() {
  const [flags, setFlags] = useState([])
  const [source, setSource] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [deleteConfirmations, setDeleteConfirmations] = useState({})
  const [deletingIds, setDeletingIds] = useState({})
  const { sourceId } = useParams()
  const navigate = useNavigate()

  const fetchSourceAndFlags = useCallback(async () => {
    try {
      const [sourceResponse, flagsResponse] = await Promise.all([
        get(`/api/v1/sources/${sourceId}`),
        get(`/api/v1/sources/${sourceId}/flags`)
      ])

      if (!sourceResponse.ok || !flagsResponse.ok) {
        throw new Error(`HTTP error! status: ${sourceResponse.status} or ${flagsResponse.status}`)
      }

      let sourceData, flagsData
      try {
        sourceData = await sourceResponse.json()
      } catch {
        console.error('Source JSON parsing error')
        sourceData = null
      }
      
      try {
        flagsData = await flagsResponse.json()
      } catch {
        console.error('Flags JSON parsing error')
        flagsData = { flags: [] }
      }

      setSource(sourceData)
      const flagsArray = flagsData && flagsData.flags ? flagsData.flags : []
      setFlags(flagsArray)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [sourceId])

  useEffect(() => {
    if (sourceId) {
      fetchSourceAndFlags()
    }
  }, [sourceId, fetchSourceAndFlags])

  const handleFlagClick = (flagId) => {
    navigate(`/sources/${sourceId}/flags/${flagId}`)
  }

  const handleCreateFlag = () => {
    navigate(`/sources/${sourceId}/flags/new`)
  }

  const handleBackToSources = () => {
    navigate("/")
  }

  const handleDeleteFlag = async (flagId, event) => {
    event.stopPropagation()
    
    if (deleteConfirmations[flagId]) {
      // Perform deletion
      try {
        setDeletingIds(prev => ({ ...prev, [flagId]: true }))
        const response = await del(`/api/v1/sources/${sourceId}/flags/${flagId}`)
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        // Remove from state
        setFlags(prev => prev.filter(flag => flag.flagId !== flagId))
        setDeleteConfirmations(prev => ({ ...prev, [flagId]: false }))
      } catch (err) {
        console.error('Delete flag error:', err)
        setError(`Failed to delete flag: ${err.message}`)
      } finally {
        setDeletingIds(prev => ({ ...prev, [flagId]: false }))
      }
    } else {
      // Show confirmation
      setDeleteConfirmations(prev => ({ ...prev, [flagId]: true }))
      // Auto-hide confirmation after 3 seconds
      setTimeout(() => {
        setDeleteConfirmations(prev => ({ ...prev, [flagId]: false }))
      }, 3000)
    }
  }

  if (loading) {
    return (
      <div className="flag-selection-container">
        <header className="app-header">
          <div className="header-breadcrumb">
            <button className="breadcrumb-link" onClick={handleBackToSources}>Sources</button>
            <span className="breadcrumb-separator">/</span>
            <span className="breadcrumb-current">Loading...</span>
          </div>
          <div className="header-actions"></div>
        </header>
        <div className="loading">Loading...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flag-selection-container">
        <header className="app-header">
          <div className="header-breadcrumb">
            <button className="breadcrumb-link" onClick={handleBackToSources}>Sources</button>
            <span className="breadcrumb-separator">/</span>
            <span className="breadcrumb-current">Error</span>
          </div>
          <div className="header-actions"></div>
        </header>
        <div className="error-message">Error: {error}</div>
      </div>
    )
  }

  return (
    <div className="flag-selection-container">
      <header className="app-header">
        <div className="header-breadcrumb">
          <button className="breadcrumb-link" onClick={handleBackToSources}>Sources</button>
          <span className="breadcrumb-separator">/</span>
          <span className="breadcrumb-current">{source?.name || 'Flags'}</span>
        </div>
        <div className="header-actions">
          <button className="button button-primary" onClick={handleCreateFlag}>New Flag</button>
        </div>
      </header>
      <div className="flags-list">
        {flags.length === 0 ? (
          <div className="empty-state">
            <p>No flags found in this source. Create your first flag to get started.</p>
          </div>
        ) : (
          flags.map((flag) => {
            const variantKeys = Object.keys(flag.variants || {})
            const inferType = () => {
              if (variantKeys.length === 0) return 'string'
              const firstValue = flag.variants[variantKeys[0]]
              if (typeof firstValue === "boolean") return "boolean"
              if (typeof firstValue === "number") return "number"
              if (typeof firstValue === "object" && firstValue !== null) return "object"
              return "string"
            }
            
            return (
              <div
                key={flag.flagId}
                className="flag-item"
                onClick={() => handleFlagClick(flag.flagId)}
              >
                <div className="flag-info">
                  <div className="flag-header">
                    <h3 className="flag-name">{flag.flagId}</h3>
                    <span className={`flag-status ${flag.state === 'ENABLED' ? 'enabled' : 'disabled'}`}>
                      {flag.state === 'ENABLED' ? 'Enabled' : 'Disabled'}
                    </span>
                  </div>
                  {flag.description && (
                    <p className="flag-description">{flag.description}</p>
                  )}
                  <div className="flag-meta">
                    <span className="flag-type">{inferType()}</span>
                    {variantKeys.length > 0 && (
                      <span className="flag-variants">{variantKeys.length} variant(s)</span>
                    )}
                  </div>
                </div>
                <div className="item-actions">
                  {deleteConfirmations[flag.flagId] ? (
                    <button
                      className="button button-small button-delete-confirm"
                      onClick={(e) => handleDeleteFlag(flag.flagId, e)}
                      disabled={deletingIds[flag.flagId]}
                      title="Confirm delete"
                    >
                      {deletingIds[flag.flagId] ? '...' : 'confirm?'}
                    </button>
                  ) : (
                    <button
                      className="button button-small button-delete"
                      onClick={(e) => handleDeleteFlag(flag.flagId, e)}
                      disabled={deletingIds[flag.flagId]}
                      title="Delete flag"
                    >
                      <span className="material-icons">delete</span>
                    </button>
                  )}
                  <div className="flag-arrow">→</div>
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}

export default FlagListPage
