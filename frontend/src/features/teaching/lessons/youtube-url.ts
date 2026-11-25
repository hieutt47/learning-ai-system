const YOUTUBE_HOSTS = new Set(['youtube.com', 'www.youtube.com', 'm.youtube.com'])
const SHORT_HOSTS = new Set(['youtu.be', 'www.youtu.be'])
const NOCOOKIE_HOSTS = new Set(['youtube-nocookie.com', 'www.youtube-nocookie.com'])
const VIDEO_ID_PATTERN = /^[A-Za-z0-9_-]{6,64}$/

function singleSegmentAfter(pathname: string, prefix: string): string | null {
  if (!pathname.startsWith(prefix)) return null
  let rest = pathname.slice(prefix.length)
  if (rest.endsWith('/')) rest = rest.slice(0, -1)
  if (!rest || rest.includes('/')) return null
  return rest
}

export function extractYouTubeVideoId(value: string): string | null {
  const trimmed = value.trim()
  if (!trimmed) return null
  try {
    const url = new URL(trimmed)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      return null
    }
    const host = url.hostname.toLowerCase()
    let candidate: string | null = null

    if (SHORT_HOSTS.has(host)) {
      candidate = singleSegmentAfter(url.pathname, '/')
    } else if (NOCOOKIE_HOSTS.has(host)) {
      candidate = singleSegmentAfter(url.pathname, '/embed/')
    } else if (YOUTUBE_HOSTS.has(host)) {
      if (url.pathname === '/watch') {
        candidate = url.searchParams.get('v')
      } else if (url.pathname.startsWith('/embed/')) {
        candidate = singleSegmentAfter(url.pathname, '/embed/')
      } else if (url.pathname.startsWith('/shorts/')) {
        candidate = singleSegmentAfter(url.pathname, '/shorts/')
      }
    }

    if (!candidate) return null
    const id = candidate.trim()
    return VIDEO_ID_PATTERN.test(id) ? id : null
  } catch {
    return null
  }
}

export function isValidYouTubeUrl(value: string): boolean {
  return extractYouTubeVideoId(value) !== null
}

export function isOptionalValidYouTubeUrl(value?: string): boolean {
  if (!value || !value.trim()) return true
  return isValidYouTubeUrl(value)
}

export function getYouTubeEmbedUrl(value: string): string | undefined {
  const videoId = extractYouTubeVideoId(value)
  return videoId ? `https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}` : undefined
}
