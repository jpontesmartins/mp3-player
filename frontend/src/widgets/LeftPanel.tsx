import { useApp } from '../app/providers/AppContext';
import LyricsPanel from '../features/lyrics/ui/LyricsPanel';
import SettingsPanel from '../features/settings/ui/SettingsPanel';
import CollectionManager from '../features/collection/ui/CollectionManager';

export default function LeftPanel() {
  const app = useApp();
  switch (app.view) {
    case 'lyrics': return <LyricsPanel />;
    case 'settings': return <SettingsPanel />;
    case 'collection': return <CollectionManager />;
  }
}
