import { createBrowserRouter, Navigate } from 'react-router-dom'
import { AppLayout } from './layouts/AppLayout'
import { DashboardPage } from './features/dashboard/DashboardPage'
import { AnalyticsPage } from './features/analytics/AnalyticsPage'
import { AddExpensePage, ExpensesPage } from './features/expenses/ExpensesPage'
import { EditExpensePage } from './features/expenses/EditExpensePage'
import { ExpenseDetailsPage } from './features/expenses/ExpenseDetailsPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'expenses', element: <ExpensesPage /> },
      { path: 'expenses/new', element: <AddExpensePage /> },
      { path: 'expenses/:id', element: <ExpenseDetailsPage /> },
      { path: 'expenses/:id/edit', element: <EditExpensePage /> },
      { path: 'analytics', element: <AnalyticsPage /> },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
])