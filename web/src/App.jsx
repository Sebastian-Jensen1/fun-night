import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import Backdrop from './components/Backdrop'
import Home from './pages/Home'
import QuickPick from './pages/QuickPick'
import Join from './pages/Join'
import RoomPage from './pages/RoomPage'

/** Remounts the room page when the address changes to another room, so no state leaks across. */
function RoomRoute() {
  const { roomId } = useParams()
  return <RoomPage key={roomId} roomId={roomId} />
}

function JoinRoute() {
  const { code } = useParams()
  return <Join key={code ?? ''} codeFromLink={code ?? ''} />
}

export default function App() {
  return (
    <>
      <Backdrop />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/quick" element={<QuickPick />} />
        <Route path="/join" element={<JoinRoute />} />
        <Route path="/join/:code" element={<JoinRoute />} />
        <Route path="/room/:roomId" element={<RoomRoute />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  )
}
