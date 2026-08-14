import { memo } from 'react'
import type { SquareValue } from '../utils/types'

interface SquareProps {
  value: SquareValue
  isWinning: boolean
}

const Square = memo(function Square({ value, isWinning }: SquareProps) {
  return (
    <div
      className={`square${isWinning ? ' square--winning' : ''}`}
      role="gridcell"
      aria-label={value ? `Square: ${value}` : 'Empty square'}
    >
      {value}
    </div>
  )
})

export default Square
