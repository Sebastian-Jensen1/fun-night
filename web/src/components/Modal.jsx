import { useEffect, useRef } from 'react'

/** A native modal dialog: traps focus, closes on Escape or a click outside. */
export default function Modal({ title, onClose, children, wide = false }) {
  const ref = useRef(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog.open) dialog.showModal()
    return () => dialog.close()
  }, [])

  return (
    <dialog
      ref={ref}
      className={`modal ${wide ? 'modal--wide' : ''}`}
      aria-label={title}
      onCancel={(e) => {
        e.preventDefault()
        onClose()
      }}
      onClick={(e) => e.target === ref.current && onClose()}
    >
      <div className="modal__box panel">
        <header className="modal__head">
          <h2 className="modal__title">{title}</h2>
          <button type="button" className="btn btn--ghost btn--icon" onClick={onClose} aria-label="Close">✕</button>
        </header>
        <div className="modal__body">{children}</div>
      </div>
    </dialog>
  )
}

/** Asks before doing something that can't be undone. */
export function Confirm({ title, children, confirmLabel, danger = false, onConfirm, onCancel }) {
  return (
    <Modal title={title} onClose={onCancel}>
      <div className="confirm">
        <div className="confirm__text">{children}</div>
        <div className="confirm__actions">
          <button type="button" className="btn btn--ghost" onClick={onCancel}>Not yet</button>
          <button type="button" className={`btn ${danger ? 'btn--danger' : 'btn--primary'}`} onClick={onConfirm} autoFocus>
            {confirmLabel}
          </button>
        </div>
      </div>
    </Modal>
  )
}
