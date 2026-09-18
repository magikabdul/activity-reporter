import { Copy, FileSpreadsheet, FileText, Printer } from 'lucide-react'
import { useState } from 'react'
import { errorMessage } from '@/api/client'
import { Button } from '@/components/ui/Button'
import { copyText } from '@/lib/clipboard'
import { downloadCsv, downloadXlsx, type Sheet } from '@/lib/export'
import { notify } from '@/lib/notify'

interface ReportToolbarProps {
  /** file name without extension, e.g. "trecom-report-2026-09" */
  fileName: string
  sheet: Sheet
  /** plain-text version of the whole report for the clipboard */
  text: string
}

export function ReportToolbar({ fileName, sheet, text }: ReportToolbarProps) {
  const [exporting, setExporting] = useState(false)

  async function copy() {
    if (await copyText(text)) notify.success('Report copied to clipboard')
    else notify.error('Could not access the clipboard')
  }

  async function xlsx() {
    setExporting(true)
    try {
      await downloadXlsx(sheet, `${fileName}.xlsx`)
      notify.success('XLSX exported', `${fileName}.xlsx`)
    } catch (error) {
      notify.error('XLSX export failed', errorMessage(error))
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="no-print flex flex-wrap items-center gap-2">
      <Button size="sm" icon={<Copy className="size-3.5" aria-hidden />} onClick={copy}>
        Copy
      </Button>
      <Button
        size="sm"
        icon={<FileText className="size-3.5" aria-hidden />}
        onClick={() => {
          downloadCsv(sheet, `${fileName}.csv`)
          notify.success('CSV exported', `${fileName}.csv`)
        }}
      >
        CSV
      </Button>
      <Button
        size="sm"
        loading={exporting}
        icon={<FileSpreadsheet className="size-3.5" aria-hidden />}
        onClick={xlsx}
      >
        XLSX
      </Button>
      <Button
        size="sm"
        icon={<Printer className="size-3.5" aria-hidden />}
        onClick={() => window.print()}
      >
        Print / PDF
      </Button>
    </div>
  )
}
