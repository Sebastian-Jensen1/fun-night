import './LawnBackground.css'

/** Sky, picket fence and a striped lawn behind every page. Purely decorative. */
export default function LawnBackground({ children }) {
  return (
    <div className="lawn-bg">
      <div className="lawn-bg__scenery" aria-hidden="true">
        <div className="lawn-bg__sun" />
        <div className="lawn-bg__cloud lawn-bg__cloud--a" />
        <div className="lawn-bg__cloud lawn-bg__cloud--b" />
        <div className="lawn-bg__house" />
        <div className="lawn-bg__fence" />
        <div className="lawn-bg__lawn" />
      </div>
      <main className="lawn-bg__content">{children}</main>
    </div>
  )
}
