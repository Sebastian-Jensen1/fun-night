/**
 * The scenery behind everything. All the sets are in the page; the stylesheet shows the one
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
      <div className="bd bd--kawaii">
        <div className="bd-dots" />
        <div className="bd-cloud bd-cloud--a"><span className="bd-face" /></div>
        <div className="bd-cloud bd-cloud--b" />
        <div className="bd-heart bd-heart--a" />
        <div className="bd-heart bd-heart--b" />
        <div className="bd-star bd-star--a" />
        <div className="bd-star bd-star--b" />
        <div className="bd-petal bd-petal--a" />
        <div className="bd-petal bd-petal--b" />
        <div className="bd-petal bd-petal--c" />
        <div className="bd-petal bd-petal--d" />
        <div className="bd-petal bd-petal--e" />
      </div>
      <div className="bd bd--undersea">
        <div className="bd-bloom bd-bloom--a" />
        <div className="bd-bloom bd-bloom--b" />
        <div className="bd-bloom bd-bloom--c" />
        <div className="bd-bloom bd-bloom--d" />
        <div className="bd-weed bd-weed--a" />
        <div className="bd-weed bd-weed--b" />
        <div className="bd-weed bd-weed--c" />
        <div className="bd-weed bd-weed--d" />
        <div className="bd-sand" />
        <div className="bd-bubble bd-bubble--a" />
        <div className="bd-bubble bd-bubble--b" />
        <div className="bd-bubble bd-bubble--c" />
        <div className="bd-bubble bd-bubble--d" />
        <div className="bd-bubble bd-bubble--e" />
      </div>
      <div className="bd bd--runic">
        <div className="bd-aura" />
        <div className="bd-hex" />
        <div className="bd-frame" />
        <div className="bd-mote bd-mote--a" />
        <div className="bd-mote bd-mote--b" />
        <div className="bd-mote bd-mote--c" />
        <div className="bd-mote bd-mote--d" />
        <div className="bd-mote bd-mote--e" />
      </div>
    </div>
  )
}
