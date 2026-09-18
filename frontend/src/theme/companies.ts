export type CompanyId = 'lufthansa' | 'trecom'
export type ThemeId = CompanyId | 'neutral'

export interface Company {
  id: CompanyId
  name: string
  tagline: string
  basePath: string
}

export const COMPANIES: Record<CompanyId, Company> = {
  lufthansa: {
    id: 'lufthansa',
    name: 'Lufthansa',
    tagline: 'AI-categorised tasks and monthly category summaries',
    basePath: '/lufthansa',
  },
  trecom: {
    id: 'trecom',
    name: 'Trecom',
    tagline: 'Customer work log with hours per salesman',
    basePath: '/trecom',
  },
}

export const COMPANY_LIST: Company[] = [COMPANIES.lufthansa, COMPANIES.trecom]

export function themeForPath(pathname: string): ThemeId {
  const match = COMPANY_LIST.find(
    (company) => pathname === company.basePath || pathname.startsWith(`${company.basePath}/`),
  )
  return match?.id ?? 'neutral'
}
