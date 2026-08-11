interface MoveLogProps {
  entries: string[]
}

function MoveLog({ entries }: MoveLogProps) {
  return (
    <aside className="log" aria-label="Move history">
      <h2>Move Log</h2>
      <ul className="log__list">
        {entries.length === 0 ? (
          <li className="log__empty">No moves yet.</li>
        ) : (
          entries.map((entry, index) => <li key={index}>{entry}</li>)
        )}
      </ul>
    </aside>
  )
}

export default MoveLog