import type { ApiErrorBody, ErrorCode, FieldErrorDetail } from './types'

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '/api/v1').replace(/\/+$/, '')
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
  fieldErrors(): Record<string, string> {
    return Object.fromEntries(this.details.map((d) => [d.field, d.message]))
  }
}
export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}

type TokenProvider = () => string | null | undefined
let accessTokenProvider: TokenProvider = () => null

export function setAccessTokenProvider(provider: TokenProvider): void {
  accessTokenProvider = provider
}

let refreshTokenHandler: (() => Promise<void>) | null = null
let sessionExpiredHandler: (() => void) | null = null

export function setRefreshTokenHandler(handler: () => Promise<void>): void {
  refreshTokenHandler = handler
}

export function setSessionExpiredHandler(handler: () => void): void {
  sessionExpiredHandler = handler
}

let unauthorizedHandler: (() => void) | null = null
export function setUnauthorizedHandler(handler: () => void): void {
  unauthorizedHandler = handler
}
type Query = Record<string, string | number | boolean | null | undefined>

export interface RequestOptions {
  query?: Query
  body?: unknown
  signal?: AbortSignal
  headers?: Record<string, string>
  auth?: boolean
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

function buildHeaders(
  options: RequestOptions,
  token: string | null | undefined = options.auth === false ? null : accessTokenProvider(),
): Headers {
  const headers = new Headers({ Accept: 'application/json', ...options.headers })
  if (options.body !== undefined && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (options.auth === false) {
    headers.delete('Authorization')
  } else if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  return headers
}
async function toApiError(response: Response): Promise<ApiError> {
  let body: Partial<ApiErrorBody> = {}
  try {
    body = (await response.json()) as Partial<ApiErrorBody>
  } catch {
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

function expireSession(): void {
  sessionExpiredHandler?.()
  unauthorizedHandler?.()
}

async function sendRequest<T>(method: string, path: string, options: RequestOptions, retried = false): Promise<T> {
  const token = options.auth === false ? null : accessTokenProvider()
  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method,
      headers: buildHeaders(options, token),
      credentials: 'include',
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
    if (response.status === 401 && token) {
      if (!retried && refreshTokenHandler) {
        try {
          if (accessTokenProvider() === token) {
            await refreshTokenHandler()
          }
        } catch (error) {
          if (isApiError(error) && (error.status === 401 || error.code === 'ACCOUNT_INACTIVE' || error.code === 'EMAIL_NOT_VERIFIED')) {
            expireSession()
          }
          throw error
        }
        options.signal?.throwIfAborted()
        return sendRequest<T>(method, path, options, true)
      }
      expireSession()
    } else if (token && (apiError.code === 'ACCOUNT_INACTIVE' || apiError.code === 'EMAIL_NOT_VERIFIED')) {
      expireSession()
    }
    throw apiError
  }
  if (response.status === 204 || response.headers.get('Content-Length') === '0') {
    return undefined as T
  }
  return (await response.json()) as T
}

export function apiRequest<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  return sendRequest<T>(method, path, options)
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
export async function apiGetBlob(path: string, options: Omit<RequestOptions, 'body'> = {}, retried = false): Promise<Blob> {
  const token = options.auth === false ? null : accessTokenProvider()
  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method: 'GET',
      headers: buildHeaders(options, token),
      credentials: 'include',
      signal: options.signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError(0, { code: 'NETWORK_ERROR', message: 'Không kết nối được máy chủ' })
  }
  if (!response.ok) {
    const apiError = await toApiError(response)
    if (response.status === 401 && token) {
      if (!retried && refreshTokenHandler) {
        try {
          if (accessTokenProvider() === token) {
            await refreshTokenHandler()
          }
        } catch (error) {
          if (isApiError(error) && (error.status === 401 || error.code === 'ACCOUNT_INACTIVE' || error.code === 'EMAIL_NOT_VERIFIED')) {
            expireSession()
          }
          throw error
        }
        options.signal?.throwIfAborted()
        return apiGetBlob(path, options, true)
      }
      expireSession()
    } else if (token && (apiError.code === 'ACCOUNT_INACTIVE' || apiError.code === 'EMAIL_NOT_VERIFIED')) {
      expireSession()
    }
    throw apiError
  }
  return response.blob()
}
