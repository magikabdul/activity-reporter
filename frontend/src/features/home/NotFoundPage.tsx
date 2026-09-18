import { Compass } from 'lucide-react'
import { ButtonLink } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'

export function NotFoundPage() {
  return (
    <EmptyState
      icon={Compass}
      title="Page not found"
      description="This address does not match any page of the application."
      action={
        <ButtonLink to="/" variant="primary">
          Back to Home
        </ButtonLink>
      }
    />
  )
}
