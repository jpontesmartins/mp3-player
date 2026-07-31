# Backend — MP3 Player API

API REST em **Java 21** com **Spring Boot 3.3.5** para reprodução de MP3 e serviços auxiliares.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 (virtual threads) |
| Framework | Spring Boot 3.3.5 |
| Build | Maven |
| Decodificador MP3 | JLayer 1.0.1 |
| Tags ID3 | mp3agic 0.9.1 |
| Web scraping | Jsoup 1.18.1 |
| Logging | SLF4J + Logback |

## Endpoints

### Reprodução

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/play` | Inicia reprodução de um arquivo MP3 (body = caminho completo) |
| `POST` | `/pause` | Pausa a música atual |
| `POST` | `/resume` | Retoma a música pausada |
| `POST` | `/stop` | Para a reprodução e limpa o estado |
| `POST` | `/seek` | Salta para uma posição específica (`{ "position": <ms> }`) |
| `GET` | `/playing` | Status atual (`playing`/`paused`/`stopped`), posição, duração e ID3 |

### Playlist

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/playlist?path=<pasta>` | Escaneia uma pasta e retorna lista de arquivos `.mp3` |

### Metadados

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/id3?path=<arquivo>` | Retorna as tags ID3 (artista, título, álbum, etc.) |
| `POST` | `/id3/bulk` | Recebe uma lista de caminhos e retorna as tags ID3 de todos de uma vez (body = `["caminho1", "caminho2", ...]`) |
| `POST` | `/id3/update` | Atualiza as tags ID3 de um arquivo (body = `{ "path": "<arquivo>", "tags": { "title": ..., "artist": ..., "album": ..., "genre": ..., "track": ..., "year": ... } }`) |
| `GET` | `/cover?path=<arquivo>` | Retorna a imagem de capa (`cover`/`folder`/`album`/`front`/`art`/`artwork` — jpg/png) da mesma pasta |
| `GET` | `/lyrics?path=<arquivo>` | Retorna a letra da música (web scraping se não houver cache) |
| `GET` | `/lyrics/cached?path=<arquivo>` | Retorna a letra apenas se já existir arquivo `.txt` em cache |

## Scraper de Letras

O endpoint `/lyrics` implementa um scraper para o site [letras.mus.br](https://www.letras.mus.br):

1. Extrai artista e título das tags ID3 do MP3
2. Constrói slugs e tenta URL direta: `/{artista}/{musica}/`
3. Se falhar, faz busca em `/?q=<artista>+<musica>` e localiza o link `<a class="gs-title">`
4. Fallbacks: `.gs-title a`, link genérico `letras.mus.br`, match por título
5. Remove sufixo `traducao.html` quando presente
6. Extrai o conteúdo de `<div class="lyric-original">` e insere `<br>` após cada `<p>`
7. Salva em `{artista} - {musica}.txt` na mesma pasta do MP3

## Logging

Todos os endpoints e serviços utilizam SLF4J com logs informativos:
- Endpoints registram chamadas com parâmetros
- `Mp3PlayService` loga play/pause/resume/stop/seek e análise do arquivo
- `LyricsService` loga cada etapa do scraper (cache hit/miss, URL direta, busca, extração)

## CORS

Configurado para permitir origens externas (necessário para o frontend em dev no Vite).
