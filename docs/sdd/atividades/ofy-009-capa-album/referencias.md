# Referências — ofy-009

## Propriedades (`application.properties`)
```
mp3.cover.user-agent=Mozilla/5.0 (compatible; MP3Player/1.0)
mp3.cover.itunes-url=https://itunes.apple.com/search?entity=album&limit=1&term=
mp3.cover.deezer-url=https://api.deezer.com/search/album?limit=1&q=
mp3.cover.timeout-connect=15000
mp3.cover.timeout-download=25000
```

## Campos extraídos
- iTunes: `"artworkUrl100"` (troca `100x100bb` → `600x600bb`).
- Deezer: `"cover_xl"`, senão `"cover_medium"`.
