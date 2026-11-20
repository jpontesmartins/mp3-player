# Referências — ofy-007

## Payload
```json
POST /id3/update
{ "path": "C:\\Music\\Album\\01 - Song.mp3",
  "tags": { "title": "Song", "artist": "Band", "album": "Album", "genre": "Rock", "track": "1", "disc": "1/2", "year": "1999" } }
```

## Dependências
- `mp3agic` (`ID3v24Tag`, `ID3v1Genres`, `Mp3File#save`).
