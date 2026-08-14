import type { MoveHistoryEntry } from './types'

/** Formats from the structured move record rather than a client-observed timestamp, so a move
 * replayed from `session.moves` after a reconnect renders identically to one seen live over SSE. */
export function formatMoveLogEntry(entry: MoveHistoryEntry): string {
  return `Move ${entry.moveNumber}: ${entry.symbol} was set to ${entry.position + 1} cell. ${entry.stepStatus}`
}
