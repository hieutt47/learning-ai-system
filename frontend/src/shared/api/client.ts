import type { ApiErrorBody, ErrorCode, FieldErrorDetail } from './types'

/**
 * API client dùng chung cho mọi feature. Một chỗ duy nhất biết base URL, header xác thực
 * và cách đọc lỗi; feature chỉ gọi apiGet/apiPost/... với path tương đối, ví dụ "/courses".
 * Frontend dùng chung cho backend Java và Python: chỉ đổi VITE_API_BASE_URL.
 */

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/+$/, '')

/** Lỗi API đã chuẩn hóa. Mọi lỗi (HTTP, mạng) đều ném ra ApiError. */
export class ApiError extends Error {
  readonly status: number
  readonly code: ErrorCode
  readonly details: FieldErrorDetail[]
  readonly requestId?: string

  constructor(status: number, body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = status
    this.code = body.code
    this.details = body.details ?? []
    this.requestId = body.request_id
  }

  /** Lỗi theo field (400 VALIDATION_ERROR) dạng {front_text: "must not be blank"} để gắn vào form. */
  fieldErrors(): Record<string, string> {
    return Object.fromEntries(this.details.map((d) => [d.field, d.message]))
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

// ---- Xác thực: phần auth (JWT) đăng ký vào đây khi xong, client không tự lưu token ----

type TokenProvider = () => string | null | undefined
let accessTokenProvider: TokenProvider = () => null

/** Auth feature gọi hàm này để client gắn "Authorization: Bearer <token>" vào mọi request. */
export function setAccessTokenProvider(provider: TokenProvider): void {
  accessTokenProvider = provider
}

let unauthorizedHandler: (() => void) | null = null

/** Gọi khi API trả 401 (đã đăng ký trong router: chuyển về trang đăng nhập, giữ trang đích). */
export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler
}

// ---- Gọi API ----

type Query = Record<string, string | number | boolean | null | undefined>

export interface RequestOptions {
  query?: Query
  /** Object → JSON; FormData → multipart (upload file). */
  body?: unknown
  signal?: AbortSignal
  headers?: Record<string, string>
}

function buildUrl(path: string, query?: Query): string {
  const url = `${BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
  if (!query) {
    return url
  }
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value))
    }
  }
  const qs = params.toString()
  return qs ? `${url}?${qs}` : url
}

function buildHeaders(options: RequestOptions): Headers {
  const headers = new Headers({ Accept: 'application/json', ...options.headers })
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  const token = accessTokenProvider()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  // TODO(stub): chỉ khi dev và backend còn dùng StubCurrentUserProvider. Bỏ khi auth JWT merge.
  const devUserId = import.meta.env.VITE_DEV_USER_ID
  if (import.meta.env.DEV && devUserId) {
    headers.set('X-User-Id', devUserId)
  }
  return headers
}

async function toApiError(response: Response): Promise<ApiError> {
  let body: Partial<ApiErrorBody> = {}
  try {
    body = (await response.json()) as Partial<ApiErrorBody>
  } catch {
    // Body không phải JSON (proxy lỗi, HTML...): dùng mã mặc định theo HTTP status.
  }
  return new ApiError(response.status, {
    code: body.code ?? defaultCode(response.status),
    message: body.message ?? (response.statusText || 'Lỗi không xác định'),
    details: body.details,
    request_id: body.request_id,
  })
}

function defaultCode(status: number): ErrorCode {
  switch (status) {
    case 400:
      return 'VALIDATION_ERROR'
    case 401:
      return 'UNAUTHENTICATED'
    case 403:
      return 'FORBIDDEN'
    case 404:
      return 'NOT_FOUND'
    case 409:
      return 'INVALID_STATE'
    case 429:
      return 'RATE_LIMITED'
    default:
      return 'UNCATEGORIZED_EXCEPTION'
  }
}

export async function apiRequest<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method,
      headers: buildHeaders(options),
      body:
        options.body === undefined
          ? undefined
          : options.body instanceof FormData
            ? options.body
            : JSON.stringify(options.body),
      signal: options.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error
    }
    throw new ApiError(0, { code: 'NETWORK_ERROR', message: 'Không kết nối được máy chủ' })
  }

  if (!response.ok) {
    const apiError = await toApiError(response)
    if (response.status === 401) {
      unauthorizedHandler?.()
    }
    throw apiError
  }

  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  return (await response.json()) as T
}

export const apiGet = <T>(path: string, options?: Omit<RequestOptions, 'body'>) =>
  apiRequest<T>('GET', path, options)

export const apiPost = <T>(path: string, body?: unknown, options?: RequestOptions) =>
  apiRequest<T>('POST', path, { ...options, body })

export const apiPut = <T>(path: string, body?: unknown, options?: RequestOptions) =>
  apiRequest<T>('PUT', path, { ...options, body })

export const apiPatch = <T>(path: string, body?: unknown, options?: RequestOptions) =>
  apiRequest<T>('PATCH', path, { ...options, body })

export const apiDelete = <T = void>(path: string, options?: RequestOptions) =>
  apiRequest<T>('DELETE', path, options)

export async function apiGetBlob(path: string, options: Omit<RequestOptions, 'body'> = {}): Promise<Blob> {
  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method: 'GET',
      headers: buildHeaders(options),
      signal: options.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError(0, { code: 'NETWORK_ERROR', message: 'Không kết nối được máy chủ' })
  }
  if (!response.ok) {
    const apiError = await toApiError(response)
    if (response.status === 401) unauthorizedHandler?.()
    throw apiError
  }
  return response.blob()
}
