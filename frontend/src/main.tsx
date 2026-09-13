import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './app/App';
import { AppProvider } from './app/providers/AppContext';
import { LibraryProvider } from './app/providers/LibraryContext';
import { PlayerProvider } from './app/providers/PlayerContext';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <AppProvider>
      <LibraryProvider>
        <PlayerProvider>
          <App />
        </PlayerProvider>
      </LibraryProvider>
    </AppProvider>
  </React.StrictMode>
);
