export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '0 B'
  if (bytes < 1024) return `${Math.round(bytes)} B`
  const kb = bytes / 1024
  if (kb < 1024) return `${kb.toFixed(1).replace('.', ',')} KB`
  const mb = kb / 1024
  return `${mb.toFixed(2).replace('.', ',')} MB`
}

export function documentTypeBadge(mediaType: string, originalName: string): { label: string; color: string } {
  const lowerType = mediaType.toLowerCase()
  const lowerName = originalName.toLowerCase()
  if (lowerType === 'application/pdf' || lowerName.endsWith('.pdf')) {
    return { label: 'PDF', color: 'red' }
  }
  if (lowerType.includes('wordprocessingml') || lowerName.endsWith('.docx')) {
    return { label: 'DOCX', color: 'blue' }
  }
  if (lowerType === 'text/plain' || lowerName.endsWith('.txt')) {
    return { label: 'TXT', color: 'green' }
  }
  return { label: 'FILE', color: 'default' }
}
