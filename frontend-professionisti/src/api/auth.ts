import { apiRequest } from './client'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  ruolo: string
}

export interface MeResponse {
  id: string
  nome: string
  cognome: string
  email: string
  ruolo: string
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiRequest<LoginResponse>('/auth/login', { method: 'POST', body: request })
}

export function me(): Promise<MeResponse> {
  return apiRequest<MeResponse>('/auth/me')
}

export function richiediResetPassword(email: string): Promise<void> {
  return apiRequest<void>('/auth/password-dimenticata', { method: 'POST', body: { email } })
}

export function resetPassword(token: string, nuovaPassword: string): Promise<void> {
  return apiRequest<void>('/auth/reset-password', { method: 'POST', body: { token, nuovaPassword } })
}
