export type Player = 'X' | 'O'

export type SquareValue = Player | null

export type BoardState = SquareValue[]

export interface WinResult {
  winner: Player
  line: number[]
}

export type SessionStatus = 'CREATED' | 'IN_PROGRESS' | 'WIN' | 'DRAW'

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
  board: SquareValue[]
  status: SessionStatus
  winner: Player | null
}