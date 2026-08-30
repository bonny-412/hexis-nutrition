export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

let getToken: () => string | null = () => null
let onUnauthorized: () => void = () => {}

export function configureApiClient(options: { getToken: () => string | null; onUnauthorized: () => void }) {
  getToken = options.getToken
  onUnauthorized = options.onUnauthorized
}

interface ApiRequestOptions {
  method?: 'GET' | 'POST'
  body?: unknown
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const token = getToken()
  const response = await fetch(BASE_URL + path, {
    method: options.method ?? 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  if (response.status === 401) {
    onUnauthorized()
    throw new ApiError(401, 'Sessione scaduta')
  }

  if (!response.ok) {
    const testo = await response.text()
    throw new ApiError(response.status, testo || 'Errore imprevisto')
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
