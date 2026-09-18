import { Save } from 'lucide-react'
import { errorMessage } from '@/api/client'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { ShortcutHint } from '@/components/ui/Kbd'

interface EditFormActionsProps {
  busy: boolean
  error: unknown
  onCancel: () => void
}

/** Footer of every edit dialog: inline error + Save / Cancel. */
export function EditFormActions({ busy, error, onCancel }: EditFormActionsProps) {
  return (
    <>
      {error != null && (
        <Alert tone="danger" title="Saving failed">
          {errorMessage(error)}
        </Alert>
      )}
      <div className="flex flex-wrap items-center justify-end gap-3">
        <ShortcutHint keys={['Ctrl', 'Enter']} label="save" />
        <Button disabled={busy} onClick={onCancel}>
          Cancel
        </Button>
        <Button
          type="submit"
          variant="primary"
          loading={busy}
          icon={<Save className="size-4" aria-hidden />}
        >
          Save changes
        </Button>
      </div>
    </>
  )
}
