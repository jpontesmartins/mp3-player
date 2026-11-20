# Referências — ofy-012

## Propriedades (`application.properties`)
```
mp3.lyrics.letras.enabled=true      mp3.lyrics.letras.priority=2     base-url=https://www.letras.mus.br   search-path=/
mp3.lyrics.vagalume.enabled=true    mp3.lyrics.vagalume.priority=1   base-url=https://www.vagalume.com.br search-path=/search.php
timeout-connect=8000  timeout-fetch=15000  (ambas as fontes)
```

## Seletores de extração
- letras.mus.br: `div.lyric-original` → `p`. vagalume: `div#lyrics`.

## Nome do arquivo de letra
`sanitize(<artist id3> + " - " + <title id3>) + ".txt"`; sem tags: nome-base do MP3 (se contém ` - `) ou `<artist> - <title>`.
