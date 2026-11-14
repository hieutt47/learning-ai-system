import type { Id, IsoDateTime, Role } from '../api/types'

export interface User {
  id: Id
  full_name: string
  email: string
  role: Role
  is_active: boolean
  created_at: IsoDateTime
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  full_name: string
}

export interface AuthResponse {
  access_token: string
}

export interface ProfileRequest {
  full_name: string
}

export interface ChangePasswordRequest {
  current_password: string
  new_password: string
}
