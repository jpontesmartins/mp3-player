import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { AppTheme } from '../../shared/types';

interface AppState {
  view: 'lyrics' | 'settings' | 'collection';
  showInfo: boolean;
  theme: AppTheme;
}

interface AppActions {
  setView: (view: AppState['view']) => void;
  setShowInfo: (show: boolean) => void;
  setTheme: (theme: AppTheme) => void;
}

const AppContext = createContext<AppState & AppActions>(null!);

const THEME_KEY = 'mp3_theme';

function loadTheme(): AppTheme {
  return localStorage.getItem(THEME_KEY) === 'light' ? 'light' : 'dark';
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [view, setView] = useState<AppState['view']>('lyrics');
  const [showInfo, setShowInfo] = useState(false);
  const [theme, setThemeState] = useState<AppTheme>(loadTheme);

  const setTheme = useCallback((newTheme: AppTheme) => {
    setThemeState(newTheme);
  }, []);

  useEffect(() => {
    document.body.dataset.theme = theme;
    localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  return (
    <AppContext.Provider value={{ view, showInfo, theme, setView, setShowInfo, setTheme }}>
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  return useContext(AppContext);
}
