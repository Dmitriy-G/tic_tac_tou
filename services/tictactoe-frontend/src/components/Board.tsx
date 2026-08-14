import type { BoardState } from '../utils/types'
import Square from './Square'

interface BoardProps {
  squares: BoardState
  winningLine: number[] | null
}

function Board({ squares, winningLine }: BoardProps) {
  const rows = [0, 1, 2]
  return (
    <div className="board" role="grid">
      {rows.map((row) => (
        // display: contents keeps these row groups out of the CSS grid's box model — the 9
        // Squares stay direct grid items for .board's `grid-template-columns`/`-rows` layout,
        // while the DOM still exposes the row/cell structure assistive tech expects.
        <div role="row" key={row} style={{ display: 'contents' }}>
          {squares.slice(row * 3, row * 3 + 3).map((value, col) => {
            // A fixed-length positional array (always 9 cells, board positions never reorder) —
            // the textbook case where an index key is correct.
            const index = row * 3 + col
            return <Square key={index} value={value} isWinning={winningLine?.includes(index) ?? false} />
          })}
        </div>
      ))}
    </div>
  )
}

export default Board
