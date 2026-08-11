import { useMemo, useState } from 'react'
import Board from './components/Board'
import { calculateWinner, createEmptyBoard, isBoardFull } from './game/logic'
import type { BoardState, Player } from './game/types'
import './App.css'

function App() {
  const [squares, setSquares] = useState<BoardState>(createEmptyBoard())
  const [xIsNext, setXIsNext] = useState(true)

  const winResult = useMemo(() => calculateWinner(squares), [squares])
  const isDraw = !winResult && isBoardFull(squares)
  const gameOver = winResult !== null || isDraw

  function handleSquareClick(index: number) {
    if (squares[index] || gameOver) return

    const nextSquares = squares.slice()
    nextSquares[index] = (xIsNext ? 'X' : 'O') as Player
    setSquares(nextSquares)
    setXIsNext(!xIsNext)
  }

  function handleReset() {
    setSquares(createEmptyBoard())
    setXIsNext(true)
  }

  const status = winResult
    ? `Winner: ${winResult.winner}`
    : isDraw
      ? "It's a draw!"
      : `Next player: ${xIsNext ? 'X' : 'O'}`

  return (
    <main className="app">
      <h1>Tic Tac Toe</h1>
      <p className="status">{status}</p>
      <Board
        squares={squares}
        winningLine={winResult?.line ?? null}
        gameOver={gameOver}
        onSquareClick={handleSquareClick}
      />
      <button className="reset" onClick={handleReset}>
        Restart
      </button>
    </main>
  )
}

export default App