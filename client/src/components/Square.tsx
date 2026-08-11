import type { SquareValue } from '../game/types'

interface SquareProps {
  value: SquareValue
  onClick: () => void
  isWinning: boolean
  disabled: boolean
}

function Square({ value, onClick, isWinning, disabled }: SquareProps) {
  return (
    <button
      className={`square${isWinning ? ' square--winning' : ''}`}
      onClick={onClick}
      disabled={disabled || value !== null}
      aria-label={value ? `Square: ${value}` : 'Empty square'}
    >
      {value}
    </button>
  )
}

export default Square