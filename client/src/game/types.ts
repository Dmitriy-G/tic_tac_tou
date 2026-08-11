export type Player = 'X' | 'O'

export type SquareValue = Player | null

export type BoardState = SquareValue[]

export interface WinResult {
  winner: Player
  line: number[]
}