# Referências — ofy-006

## Exemplo de resposta `POST /id3/bulk`
```json
{ "C:\\Music\\a.mp3": { "title": "Song", "artist": "Band", "album": "Album", "year": "1999",
  "genre": "Rock", "track": "3", "duration_ms": "215000", "kbps": "320" },
  "C:\\Music\\b.mp3": { "error": "Could not read ID3 tags" } }
```

## Dependências
- `com.mpatric:mp3agic:0.9.1`.
