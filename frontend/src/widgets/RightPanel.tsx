import Player from '../features/playback/ui/Player';
import Playlist from '../features/playlist-view/ui/Playlist';

export default function RightPanel() {
  return (
    <div id="right-panel">
      <Player />
      <Playlist />
    </div>
  );
}
