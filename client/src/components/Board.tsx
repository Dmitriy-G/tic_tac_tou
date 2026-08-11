import type { BoardState } from '../game/types'
import Square from './Square'

interface BoardProps {
  squares: BoardState
  winningLine: number[] | null
  gameOver: boolean
  onSquareClick: (index: number) => void
}

function Board({ squares, winningLine, gameOver, onSquareClick }: BoardProps) {
  return (
    <div className="board" role="grid">
      {squares.map((value, index) => (
        <Square
          key={index}
          value={value}
          isWinning={winningLine?.includes(index) ?? false}
          disabled={gameOver}
          onClick={() => onSquareClick(index)}
        />
      ))}
    </div>
  )
}

export default Board