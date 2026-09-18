import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '@/components/layout/AppShell'
import { HomePage } from '@/features/home/HomePage'
import { NotFoundPage } from '@/features/home/NotFoundPage'
import { LufthansaNewTaskPage } from '@/features/lufthansa/NewTaskPage'
import { LufthansaReportPage } from '@/features/lufthansa/ReportPage'
import { LufthansaTasksPage } from '@/features/lufthansa/TasksPage'
import { TrecomNewTaskPage } from '@/features/trecom/NewTaskPage'
import { TrecomReportPage } from '@/features/trecom/ReportPage'
import { TrecomTasksPage } from '@/features/trecom/TasksPage'

export function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<HomePage />} />
        <Route path="lufthansa">
          <Route index element={<Navigate to="new-task" replace />} />
          <Route path="new-task" element={<LufthansaNewTaskPage />} />
          <Route path="tasks" element={<LufthansaTasksPage />} />
          <Route path="report" element={<LufthansaReportPage />} />
        </Route>
        <Route path="trecom">
          <Route index element={<Navigate to="new-task" replace />} />
          <Route path="new-task" element={<TrecomNewTaskPage />} />
          <Route path="tasks" element={<TrecomTasksPage />} />
          <Route path="report" element={<TrecomReportPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
