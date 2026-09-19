import { ASSIGNABLE_TASK_CATEGORIES, type TaskCategory } from '@/api/types'

/** Same Polish contract wording the backend uses (TaskCategory enum). */
export const CATEGORY_DESCRIPTIONS: Record<TaskCategory, string> = {
  SOFTWARE_DEVELOPMENT:
    'Projektowanie i testowanie oprogramowania w odgórnie ustalonym czasie zgodnie wymaganiami biznesowymi.',
  CONSULTING_AND_TRAINING:
    'Udzielenie konsultacji oraz przeprowadzanie szkoleń w zakresie wytwarzania oprogramowania.',
  DOCUMENTATION:
    'Przygotowywanie dokumentacji technicznej i projektowej związanej z wytwarzanym oprogramowaniem w formie i na zasadach obowiązujących u Zlecającego.',
  CODE_ANALYSIS_AND_REFINEMENT:
    'Analiza wymagań, tworzenie i doskonalenie kodu źródłowego i weryfikacja powstających funkcjonalności.',
  BUG_FIXING_AND_MAINTENANCE: 'Rozwiązywanie błędów i wprowadzanie zmian kodzie źródłowym.',
  TECHNOLOGY_SELECTION:
    'Dobór technologii do rozwiązań na podstawie znajomości trendów rozwoju oprogramowania i różnych technologii.',
  ARCHITECTURE_DESIGN:
    'Analiza i projektowanie architektury oprogramowania w zleconych projektach.',
  UNKNOWN: 'Nie można przypisać zadania do żadnej z dostępnych kategorii.',
}

export function categoryLabel(category: string): string {
  return category.replaceAll('_', ' ')
}

/** The 7 categories a task can be stored with — UNKNOWN is never offered. */
export const CATEGORY_OPTIONS = ASSIGNABLE_TASK_CATEGORIES.map((category) => ({
  value: category,
  label: categoryLabel(category),
}))
