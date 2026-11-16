/** A number with minus and plus buttons, e.g. a bet in sips. */
export default function Stepper({ value, min, max, onChange, disabled = false, label }) {
  return (
    <div className="stepper" role="group" aria-label={label}>
      <button type="button" className="stepper__btn" onClick={() => onChange(value - 1)} disabled={disabled || value <= min}
        aria-label={`${label}: one less`}>−</button>
      <span className="stepper__value" aria-live="polite">{value}</span>
      <button type="button" className="stepper__btn" onClick={() => onChange(value + 1)} disabled={disabled || value >= max}
        aria-label={`${label}: one more`}>+</button>
    </div>
  )
}
