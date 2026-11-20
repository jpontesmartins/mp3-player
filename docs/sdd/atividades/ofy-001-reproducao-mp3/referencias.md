# Referências — ofy-001

## Payloads
```json
// GET /playing (tocando)
{ "status": "playing", "file": "C:\\Music\\a.mp3", "position": 12034, "duration": 215000,
  "id3": { "title": "...", "artist": "...", "duration_ms": "215000", "kbps": "320" } }
// GET /playing (parado)
{ "status": "stopped", "file": "" }
// POST /seek
{ "position": 90000 }
```

## Dependências
- `javazoom:jlayer:1.0.1`; `Id3Codec` (ver `ofy-006`).

## Relacionadas
- `ofy-002` (navegação/autoplay consome `status`), `ofy-004` (lista), `ofy-006` (tags).
