// Lowkey casino jazz, generated live with the Web Audio API (no audio files, nothing licensed).
// A swung ii–V–I–VI loop in C: soft electric-piano comping, a walking upright-ish bass and
// brushed ride cymbal. Everything is scheduled slightly ahead of time so timing stays tight.

const BPM = 96
const BEAT = 60 / BPM
const SWING = 2 / 3 // the off-beat eighth lands two thirds into the beat
const LOOKAHEAD = 0.15 // seconds of music scheduled ahead of the clock
const TICK_MS = 40

const midiToHz = (m) => 440 * 2 ** ((m - 69) / 12)

// One bar per chord. Rootless voicings keep it mellow; the bass supplies the roots.
const BARS = [
  { chord: [53, 57, 60, 64], bass: [38, 41, 45, 44] }, // Dm9  (D F A Ab→)
  { chord: [53, 59, 64, 69], bass: [43, 47, 50, 49] }, // G13  (G B D Db→)
  { chord: [52, 55, 59, 62], bass: [48, 47, 45, 46] }, // Cmaj9 (C B A Bb→)
  { chord: [55, 61, 64, 67], bass: [45, 49, 52, 51] }, // A7   (A C# E Eb→)
]

// Comping hits within a bar: [beat, swung off-beat?, velocity]
const COMP = [
  [0, false, 0.9],
  [1, true, 0.6],
  [3, false, 0.5],
]

export class JazzLoop {
  constructor() {
    this.ctx = null
    this.timer = null
    this.nextBeatTime = 0
    this.beat = 0
    this.wanted = false
    this.starting = false
  }

  get playing() {
    return this.timer !== null
  }

  /**
   * Starts (or keeps) the music. Safe to call repeatedly, e.g. from every user gesture: each call
   * nudges the browser to unlock audio, but only one loop is ever scheduled.
   */
  async start() {
    this.wanted = true
    if (this.playing) return
    if (this.disposed) return
    if (!this.ctx && !this.createContext()) return
    const ctx = this.ctx
    const resumed = ctx.resume() // must be called inside the gesture to unlock autoplay
    if (this.starting) return
    this.starting = true
    try {
      await resumed
    } finally {
      this.starting = false
    }
    if (!this.wanted || this.playing || this.ctx !== ctx) return
    const now = ctx.currentTime
    this.master.gain.cancelScheduledValues(now)
    this.master.gain.setValueAtTime(this.master.gain.value, now)
    this.master.gain.linearRampToValueAtTime(0.16, now + 1.5) // fade in
    this.nextBeatTime = now + 0.1
    this.beat = 0
    this.timer = setInterval(() => this.schedule(), TICK_MS)
  }

  stop() {
    this.wanted = false
    if (!this.playing) return
    clearInterval(this.timer)
    this.timer = null
    const now = this.ctx.currentTime
    this.master.gain.cancelScheduledValues(now)
    this.master.gain.setValueAtTime(this.master.gain.value, now)
    this.master.gain.linearRampToValueAtTime(0, now + 0.4)
  }

  createContext() {
    const Ctx = window.AudioContext || window.webkitAudioContext
    if (!Ctx) return false
    this.ctx = new Ctx()
    this.master = this.ctx.createGain()
    this.master.gain.value = 0
    const warmth = this.ctx.createBiquadFilter()
    warmth.type = 'lowpass'
    warmth.frequency.value = 3200
    this.master.connect(warmth).connect(this.ctx.destination)
    this.noise = this.makeNoise()
    return true
  }

  dispose() {
    this.stop()
    this.disposed = true
    if (this.ctx) {
      const ctx = this.ctx
      this.ctx = null
      setTimeout(() => ctx.close().catch(() => {}), 500)
    }
  }

  schedule() {
    while (this.nextBeatTime < this.ctx.currentTime + LOOKAHEAD) {
      this.playBeat(this.beat, this.nextBeatTime)
      this.nextBeatTime += BEAT
      this.beat = (this.beat + 1) % (BARS.length * 4)
    }
  }

  playBeat(index, t) {
    const bar = BARS[Math.floor(index / 4)]
    const beatInBar = index % 4

    this.bass(bar.bass[beatInBar], t)

    for (const [beat, offbeat, velocity] of COMP) {
      if (beat === beatInBar) this.chord(bar.chord, t + (offbeat ? BEAT * SWING : 0), velocity)
    }

    // Ride: on every beat, plus the swung "and" of 2 and 4. Brushes on 2 and 4.
    this.ride(t, beatInBar % 2 === 1 ? 0.55 : 0.4)
    if (beatInBar % 2 === 1) {
      this.ride(t + BEAT * SWING, 0.3)
      this.brush(t)
    }
  }

  bass(midi, t) {
    const { ctx } = this
    const osc = ctx.createOscillator()
    osc.type = 'triangle'
    osc.frequency.value = midiToHz(midi)
    const filter = ctx.createBiquadFilter()
    filter.type = 'lowpass'
    filter.frequency.value = 600
    const gain = ctx.createGain()
    gain.gain.setValueAtTime(0, t)
    gain.gain.linearRampToValueAtTime(0.55, t + 0.015)
    gain.gain.exponentialRampToValueAtTime(0.001, t + BEAT * 0.95)
    osc.connect(filter).connect(gain).connect(this.master)
    osc.start(t)
    osc.stop(t + BEAT)
  }

  chord(notes, t, velocity) {
    const { ctx } = this
    for (const midi of notes) {
      // Electric-piano-ish: a sine with a quiet octave partial and a soft bell-like decay.
      const gain = ctx.createGain()
      gain.gain.setValueAtTime(0, t)
      gain.gain.linearRampToValueAtTime(0.07 * velocity, t + 0.01)
      gain.gain.exponentialRampToValueAtTime(0.001, t + 1.6)
      gain.connect(this.master)
      for (const [mult, level] of [[1, 1], [2, 0.18]]) {
        const osc = ctx.createOscillator()
        osc.type = 'sine'
        osc.frequency.value = midiToHz(midi) * mult
        osc.detune.value = (Math.random() - 0.5) * 6
        const partial = ctx.createGain()
        partial.gain.value = level
        osc.connect(partial).connect(gain)
        osc.start(t)
        osc.stop(t + 1.7)
      }
    }
  }

  ride(t, level) {
    const { ctx } = this
    const src = ctx.createBufferSource()
    src.buffer = this.noise
    const hp = ctx.createBiquadFilter()
    hp.type = 'highpass'
    hp.frequency.value = 7000
    const gain = ctx.createGain()
    gain.gain.setValueAtTime(0.05 * level, t)
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.25)
    src.connect(hp).connect(gain).connect(this.master)
    src.start(t)
    src.stop(t + 0.3)
  }

  brush(t) {
    const { ctx } = this
    const src = ctx.createBufferSource()
    src.buffer = this.noise
    const bp = ctx.createBiquadFilter()
    bp.type = 'bandpass'
    bp.frequency.value = 2500
    bp.Q.value = 0.7
    const gain = ctx.createGain()
    gain.gain.setValueAtTime(0, t)
    gain.gain.linearRampToValueAtTime(0.04, t + 0.03)
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.35)
    src.connect(bp).connect(gain).connect(this.master)
    src.start(t)
    src.stop(t + 0.4)
  }

  makeNoise() {
    const length = this.ctx.sampleRate
    const buffer = this.ctx.createBuffer(1, length, this.ctx.sampleRate)
    const data = buffer.getChannelData(0)
    for (let i = 0; i < length; i++) data[i] = Math.random() * 2 - 1
    return buffer
  }
}
