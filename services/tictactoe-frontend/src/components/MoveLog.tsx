import { formatMoveLogEntry } from '../utils/moveLog'
import type { MoveHistoryEntry } from '../utils/types'

interface MoveLogProps {
  entries: MoveHistoryEntry[]
}

function MoveLog({ entries }: MoveLogProps) {
  return (
    <aside className="log" aria-label="Move history">
      <h2>Move Log</h2>
      <ul className="log__list">
        {entries.length === 0 ? (
          <li className="log__empty">No moves yet.</li>
        ) : (
          entries.map((entry) => <li key={entry.moveNumber}>{formatMoveLogEntry(entry)}</li>)
        )}
      </ul>
    </aside>
  )
}

export default MoveLog
