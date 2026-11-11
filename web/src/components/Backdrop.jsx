/**
 * The scenery behind everything. All three sets are in the page; the stylesheet shows the one
 * that belongs to the active theme.
 */
export default function Backdrop() {
  return (
    <div className="backdrop" aria-hidden="true">
      <div className="bd bd--synthwave">
        <div className="bd-stars" />
        <div className="bd-sun" />
        <div className="bd-ridge" />
        <div className="bd-grid" />
        <div className="bd-scan" />
      </div>
      <div className="bd bd--pixel">
        <div className="bd-sky" />
        <div className="bd-moon" />
        <div className="bd-hills bd-hills--far" />
        <div className="bd-hills bd-hills--near" />
        <div className="bd-ground" />
      </div>
      <div className="bd bd--y2k">
        <div className="bd-blob bd-blob--a" />
        <div className="bd-blob bd-blob--b" />
        <div className="bd-blob bd-blob--c" />
        <div className="bd-mesh" />
        <div className="bd-orbit bd-orbit--a" />
        <div className="bd-orbit bd-orbit--b" />
        <div className="bd-spark bd-spark--a" />
        <div className="bd-spark bd-spark--b" />
        <div className="bd-spark bd-spark--c" />
      </div>
    </div>
  )
}
