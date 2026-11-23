import { Modal } from '../../components/Modal'
import { ExpenseForm } from './ExpenseForm'

interface AddExpenseModalProps {
  open: boolean
  onClose: () => void
  onSubmit: Parameters<typeof ExpenseForm>[0]['onSubmit']
  formError?: string | null
}

export function AddExpenseModal({ open, onClose, onSubmit, formError }: AddExpenseModalProps) {
  return (
    <Modal title="Add expense" open={open} onClose={onClose}>
      <ExpenseForm onSubmit={onSubmit} onCancel={onClose} submitLabel="Add expense" formError={formError} />
    </Modal>
  )
}