import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import ProtectedRoute from './components/ProtectedRoute'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Tasks from './pages/Tasks'
import Templates from './pages/Templates'
import TaskTemplateData from './pages/TaskTemplateData'
import SmtpConfigurations from './pages/SmtpConfigurations'
import TaskExecutions from './pages/TaskExecutions'
import DeadLetterQueue from './pages/DeadLetterQueue'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Dashboard />} />
        <Route path="/tasks" element={<Tasks />} />
        <Route path="/templates" element={<Templates />} />
        <Route path="/template-data" element={<TaskTemplateData />} />
        <Route path="/smtp" element={<SmtpConfigurations />} />
        <Route path="/executions" element={<TaskExecutions />} />
        <Route path="/dlq" element={<DeadLetterQueue />} />
      </Route>
    </Routes>
  )
}
