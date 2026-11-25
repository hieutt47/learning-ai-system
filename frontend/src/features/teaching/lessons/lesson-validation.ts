export const MAX_TRANSCRIPT_UTF8_BYTES = 65_535
export const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024
export const ALLOWED_DOCUMENT_EXTENSIONS = ['.pdf', '.docx', '.txt'] as const

const textEncoder = new TextEncoder()

export function utf8ByteLength(value: string): number {
  return textEncoder.encode(value).length
}

export function validateDocumentFile(file: { name: string; size: number }): string | null {
  if (file.size <= 0) {
    return `Tệp “${file.name}” đang rỗng.`
  }
  if (file.size > MAX_DOCUMENT_BYTES) {
    return `Tệp “${file.name}” vượt quá giới hạn 10 MiB.`
  }
  const lowerName = file.name.toLowerCase()
  const supported = ALLOWED_DOCUMENT_EXTENSIONS.some((ext) => lowerName.endsWith(ext))
  if (!supported) {
    return `Tệp “${file.name}” không đúng định dạng hỗ trợ (chỉ nhận PDF, DOCX hoặc TXT).`
  }
  return null
}
