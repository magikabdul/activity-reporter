import { useWatch, type UseFormReturn } from 'react-hook-form'
import type { Salesman } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { TextAreaField, TextField } from '@/components/ui/Field'
import { DescriptionField, TaskDateField } from '@/features/tasks/fields'
import type { TrecomSuggestions } from '@/lib/suggestions'
import { fullName } from './salesman'
import type { TrecomTaskForm } from './schema'

const RECENT_SALESMEN = 5

interface TrecomTaskFieldsProps {
  form: UseFormReturn<TrecomTaskForm>
  suggestions: TrecomSuggestions
  descriptionMax: number
  disabled?: boolean
  autoFocus?: boolean
}

/** The Trecom task fields — the same block in "New task" and in the edit dialog. */
export function TrecomTaskFields({
  form,
  suggestions,
  descriptionMax,
  disabled,
  autoFocus,
}: TrecomTaskFieldsProps) {
  const {
    register,
    control,
    setValue,
    formState: { errors },
  } = form
  const [description, createdAt] = useWatch({ control, name: ['description', 'createdAt'] })
  const recentSalesmen = suggestions.salesmen.slice(0, RECENT_SALESMEN)

  function pickSalesman(salesman: Salesman) {
    setValue('salesman.firstName', salesman.firstName, { shouldValidate: true, shouldDirty: true })
    setValue('salesman.lastName', salesman.lastName, { shouldValidate: true, shouldDirty: true })
  }

  return (
    <>
      <div className="grid gap-5 sm:grid-cols-[1fr_12rem_7rem]">
        <TextField
          label="Customer"
          placeholder="e.g. ORLEN"
          suggestions={suggestions.customers}
          error={errors.customer?.message}
          disabled={disabled}
          autoFocus={autoFocus}
          {...register('customer')}
        />
        <TaskDateField
          registration={register('createdAt')}
          value={createdAt}
          error={errors.createdAt?.message}
          disabled={disabled}
        />
        <TextField
          label="Hours spent"
          type="number"
          inputMode="numeric"
          min={1}
          step={1}
          error={errors.hoursSpent?.message}
          disabled={disabled}
          {...register('hoursSpent', { valueAsNumber: true })}
        />
      </div>

      <DescriptionField
        registration={register('description')}
        value={description}
        max={descriptionMax}
        placeholder="e.g. Konfiguracja i wdrożenie klastra firewalli w siedzibie klienta…"
        error={errors.description?.message}
        disabled={disabled}
      />

      <fieldset className="flex flex-col gap-3">
        <legend className="sr-only">Salesman</legend>
        <div className="grid gap-5 sm:grid-cols-2">
          <TextField
            label="Salesman first name"
            placeholder="e.g. Anna"
            suggestions={[...new Set(suggestions.salesmen.map((s) => s.firstName))]}
            error={errors.salesman?.firstName?.message}
            disabled={disabled}
            {...register('salesman.firstName')}
          />
          <TextField
            label="Salesman last name"
            placeholder="e.g. Kowalska-Nowak"
            suggestions={[...new Set(suggestions.salesmen.map((s) => s.lastName))]}
            error={errors.salesman?.lastName?.message}
            disabled={disabled}
            {...register('salesman.lastName')}
          />
        </div>
        {recentSalesmen.length > 0 && (
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs text-muted">Recent:</span>
            {recentSalesmen.map((salesman) => (
              <Button
                key={fullName(salesman)}
                size="sm"
                disabled={disabled}
                onClick={() => pickSalesman(salesman)}
              >
                {fullName(salesman)}
              </Button>
            ))}
          </div>
        )}
      </fieldset>

      <TextAreaField
        label="Notes"
        optional
        className="min-h-20"
        error={errors.notes?.message}
        disabled={disabled}
        {...register('notes')}
      />
    </>
  )
}
