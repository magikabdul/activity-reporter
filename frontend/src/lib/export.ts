export type Cell = string | number

export interface Sheet {
  name: string
  headers: string[]
  rows: Cell[][]
}

const BOM = String.fromCharCode(0xfeff)

function escapeCsv(value: Cell): string {
  const text = String(value)
  return /[";\n\r]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text
}

/** Semicolon-separated + BOM, so Polish Excel opens it correctly with diacritics. */
export function toCsv({ headers, rows }: Pick<Sheet, 'headers' | 'rows'>): string {
  const lines = [headers, ...rows].map((row) => row.map(escapeCsv).join(';'))
  return `${BOM}${lines.join('\r\n')}\r\n`
}

export function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function downloadCsv(sheet: Sheet, fileName: string): void {
  downloadBlob(new Blob([toCsv(sheet)], { type: 'text/csv;charset=utf-8' }), fileName)
}

export async function downloadXlsx(sheet: Sheet, fileName: string): Promise<void> {
  // Lazy-loaded: keeps the XLSX writer out of the main bundle.
  const { default: writeXlsxFile } = await import('write-excel-file/browser')
  const data = [
    sheet.headers.map((value) => ({ value, fontWeight: 'bold' as const })),
    ...sheet.rows.map((row) =>
      row.map((value) =>
        typeof value === 'number'
          ? { value, type: Number }
          : { value, type: String, wrap: true as const },
      ),
    ),
  ]
  const columns = sheet.headers.map((header, index) => {
    const longest = Math.max(header.length, ...sheet.rows.map((row) => String(row[index]).length))
    return { width: Math.min(Math.max(longest + 2, 10), 80) }
  })
  await writeXlsxFile(data, { sheet: sheet.name, columns }).toFile(fileName)
}
