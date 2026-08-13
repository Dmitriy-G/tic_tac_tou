export type Player = 'X' | 'O'

export type SquareValue = Player | null

export type BoardState = SquareValue[]

export interface WinResult {
  winner: Player
  line: number[]
}

export type GameOutcome = 'X_WON' | 'O_WON' | 'DRAW' | 'FAILED' | 'CANCELLED'

export type SessionStatus = 'CREATED' | 'IN_PROGRESS' | GameOutcome

export type StepStatus =
  | 'CORRECT_STEP'
  | 'GAME_FINISHED'
  | 'INVALID_SYMBOL'
  | 'INVALID_POSITION'
  | 'CELL_OCCUPIED'
  | 'OUT_OF_TURN'

export interface MoveRecord {
  player: Player
  position: number
}

export interface GameSession {
  sessionId: string
  gameId: string
  status: SessionStatus
  moveHistory: MoveRecord[]
}

export interface SessionEvent {
  sessionId: string
  board: string[]
  stepStatus: StepStatus
  winner: GameOutcome | null
}