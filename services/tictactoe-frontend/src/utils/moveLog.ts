import type { Player, StepStatus } from './types'

function pad(value: number): string {
  return value.toString().padStart(2, '0')
}

export function formatTimestamp(date: Date): string {
  const datePart = `${pad(date.getMonth() + 1)}/${pad(date.getDate())}/${date.getFullYear()}`
  const timePart = `${pad(date.getHours())}.${pad(date.getMinutes())}`
  return `${datePart} ${timePart}`
}

export function formatMoveLogEntry(
  date: Date,
  player: Player,
  cell: number,
  stepStatus: StepStatus,
): string {
  return `${formatTimestamp(date)}: ${player} was set to ${cell} cell. ${stepStatus}`
}