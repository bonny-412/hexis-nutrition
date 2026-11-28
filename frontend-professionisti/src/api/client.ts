export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export class ErroreRete extends Error {
  constructor(causa: unknown) {
    super('Server non raggiungibile', { cause: causa })
  }
}

/** Il server non ha risposto entro TIMEOUT_MS: per chi chiama è un errore di rete (bug S6 della verifica 3). */
export class ErroreTimeout extends ErroreRete {
  constructor() {
    super('timeout')
    this.message = 'Il server non ha risposto in tempo'
  }
}

// Senza un limite, una richiesta senza risposta lasciava "Salvataggio…" acceso per sempre.
export const TIMEOUT_MS = 30_000

export function eErroreConnessione(errore: unknown) {
  return errore instanceof ErroreRete || (errore instanceof ApiError && errore.status >= 500)
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

let getToken: () => string | null = () => null
let onUnauthorized: () => void = () => {}

export function configureApiClient(options: { getToken: () => string | null; onUnauthorized: () => void }) {
  getToken = options.getToken
  onUnauthorized = options.onUnauthorized
}

interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  /**
   * Chiave che identifica una creazione: se la stessa richiesta arriva due volte (risposta persa e nuovo
   * tentativo) il backend non crea un doppione e restituisce la risposta della prima (bug S4 della verifica 3).
   */
  chiaveIdempotenza?: string
}

export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const token = getToken()
  const controllo = new AbortController()
  const timer = setTimeout(() => controllo.abort(), TIMEOUT_MS)
  let response: Response
  try {
    response = await fetch(BASE_URL + path, {
      method: options.method ?? 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.chiaveIdempotenza ? { 'Idempotency-Key': options.chiaveIdempotenza } : {}),
      },
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      signal: controllo.signal,
    })
  } catch (errore: unknown) {
    throw controllo.signal.aborted ? new ErroreTimeout() : new ErroreRete(errore)
  } finally {
    clearTimeout(timer)
  }

  if (response.status === 401) {
    // Solo una richiesta autenticata vuol dire "sessione scaduta": il 401 del login sono credenziali sbagliate.
    if (token) onUnauthorized()
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
