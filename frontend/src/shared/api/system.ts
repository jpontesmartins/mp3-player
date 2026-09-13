import { API, fetchJSON } from './client';

export interface SystemInfo {
  logFile: string;
  cacheFile: string;
  backendPort: string;
  frontendPort: string;
}

export const getInfo = () => fetchJSON<SystemInfo>(`${API}/info`);
