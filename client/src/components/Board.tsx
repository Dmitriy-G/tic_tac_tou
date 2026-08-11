import type { BoardState } from '../game/types'
import Square from './Square'

interface BoardProps {
  squares: BoardState
  winningLine: number[] | null
}

function Board({ squares, winningLine }: BoardProps) {
  return (
    <div className="board" role="grid">
      {squares.map((value, index) => (
        <Square key={index} value={value} isWinning={winningLine?.includes(index) ?? false} />
      ))}
    </div>
  )
}

export default Board