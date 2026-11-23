import { Modal } from '../../components/Modal'
import type { ExpenseResponse } from '../../types/api'
import { ExpenseForm } from './ExpenseForm'

interface EditExpenseModalProps {
  open: boolean
  expense: ExpenseResponse | null
  onClose: () => void
  onSubmit: Parameters<typeof ExpenseForm>[0]['onSubmit']
  formError?: string | null
}

export function EditExpenseModal({ open, expense, onClose, onSubmit, formError }: EditExpenseModalProps) {
  if (!expense) return null
  return (
    <Modal title="Edit expense" open={open} onClose={onClose}>
      <ExpenseForm
        key={expense.id}
        initial={expense}
        onSubmit={onSubmit}
        onCancel={onClose}
        submitLabel="Save changes"
        formError={formError}
      />
    </Modal>
  )
}