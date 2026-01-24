import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import ProtectedRoute from './components/common/ProtectedRoute'
import Login from './pages/auth/Login'
import SourceSelection from './pages/sources/SourceListPage'
import SourceCreation from './pages/sources/CreateSourcePage'
import FlagSelection from './pages/flags/FlagListPage'
import FlagEdit from './pages/flags/FlagEditPage'
import ErrorBoundary from './components/common/ErrorBoundary'
import './index.css'
import './components.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={
              <ProtectedRoute>
                <SourceSelection />
              </ProtectedRoute>
            } />
            <Route path="/sources/new" element={
              <ProtectedRoute>
                <SourceCreation />
              </ProtectedRoute>
            } />
            <Route path="/sources/:sourceId/flags" element={
              <ProtectedRoute>
                <FlagSelection />
              </ProtectedRoute>
            } />
            <Route path="/sources/:sourceId/flags/:flagId" element={
              <ProtectedRoute>
                <FlagEdit />
              </ProtectedRoute>
            } />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  </React.StrictMode>,
)
