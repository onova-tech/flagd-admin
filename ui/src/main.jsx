import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import SourceSelection from './SourceSelection.jsx'
import SourceCreation from './SourceCreation.jsx'
import FlagSelection from './FlagSelection.jsx'
import FlagEdit from './App.jsx'
import './index.css'
import './components.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<SourceSelection />} />
        <Route path="/sources/new" element={<SourceCreation />} />
        <Route path="/sources/:sourceId/flags" element={<FlagSelection />} />
        <Route path="/sources/:sourceId/flags/:flagId" element={<FlagEdit />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>,
)
