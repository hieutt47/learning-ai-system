const YOUTUBE_HOSTS = new Set([
  'youtube.com',
  'www.youtube.com',
  'm.youtube.com',
])

export function isValidYouTubeUrl(value: string): boolean {
  try {
    const url = new URL(value)

    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      return false
    }

    if (url.hostname === 'youtu.be') {
      return url.pathname.slice(1).trim().length > 0
    }

    if (!YOUTUBE_HOSTS.has(url.hostname)) {
      return false
    }

    if (url.pathname === '/watch') {
      return Boolean(url.searchParams.get('v'))
    }

    return (
      url.pathname.startsWith('/embed/') ||
      url.pathname.startsWith('/shorts/')
    )
  } catch {
    return false
  }
}

export function getYouTubeEmbedUrl(value: string): string | undefined {
  if (!isValidYouTubeUrl(value)) return undefined
  const url = new URL(value)
  let videoId = ''
  if (url.hostname === 'youtu.be') videoId = url.pathname.slice(1)
  else if (url.pathname === '/watch') videoId = url.searchParams.get('v') ?? ''
  else videoId = url.pathname.split('/').filter(Boolean).at(-1) ?? ''
  return videoId ? `https://www.youtube-nocookie.com/embed/${encodeURIComponent(videoId)}` : undefined
}
