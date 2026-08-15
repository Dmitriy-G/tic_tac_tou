export type Player = 'X' | 'O'

export type SquareValue = Player | null

export type BoardState = SquareValue[]

export interface WinResult {
  winner: Player
  line: number[]
}

export type GameOutcome = 'X_WON' | 'O_WON' | 'DRAW' | 'FAILED'

export type SessionStatus = 'CREATED' | 'IN_PROGRESS' | GameOutcome

export type StepStatus =
  | 'CORRECT_STEP'
  | 'GAME_FINISHED'
  | 'INVALID_SYMBOL'
  | 'INVALID_POSITION'
  | 'CELL_OCCUPIED'
  | 'OUT_OF_TURN'

export interface MoveHistoryEntry {
  moveNumber: number
  symbol: Player
  position: number
  stepStatus: StepStatus
}

/** Mirrors the session service's SessionResponse — the full session view GET /sessions/{id}
 * returns, sufficient on its own to rebuild the UI without the SSE stream. */
export interface SessionResponse {
  sessionId: string
  status: SessionStatus
  board: string[]
  moves: MoveHistoryEntry[]
  winner: Player | null
  errorCode: string | null
  errorMessage: string | null
}

export interface SessionEvent {
  sessionId: string
  type: 'MOVE' | 'COMPLETED' | 'FAILED'
  board: string[] | null
  stepStatus: StepStatus | null
  winner: GameOutcome | null
  errorCode: string | null
  errorMessage: string | null
  traceId: string | null
}

/** Mirrors the shared ErrorResponse both services return for a fault. */
export interface ApiErrorBody {
  timestamp?: string
  status?: number
  code?: string
  message?: string
  path?: string
  traceId?: string
}
