import { API, fetchJSON, postJSON } from './client';

export const loadFolder = (path: string) =>
  fetchJSON<string[]>(`${API}/playlist?path=${encodeURIComponent(path)}`);

export const loadVirtual = (name: string) =>
  fetchJSON<string[]>(`${API}/playlist/${encodeURIComponent(name)}`);

export const listPlaylists = () =>
  fetchJSON<string[]>(`${API}/playlists`);

export const savePlaylist = (name: string, paths: string[]) =>
  postJSON(`${API}/playlist`, { name, paths });

import { deleteResource } from './client';

export const deletePlaylist = (name: string) =>
  deleteResource(`${API}/playlist/${encodeURIComponent(name)}`);
