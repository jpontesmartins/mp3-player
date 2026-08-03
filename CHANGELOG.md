# Changelog

Todas as alterações notáveis deste projeto serão documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e as
versões seguem [Semântic Versioning](https://semver.org/lang/pt-BR/).

## [Não publicado] — v0.9.x (alterações desde o empacotamento v0.9.0)

### Adicionado
- Playlists virtuais: criar, editar, listar, renomear e excluir playlists salvas,
  com gerenciador na tela de Coleção.
- Testes unitários do backend com JUnit 5 e Mockito (domínio, application e infraestrutura).
- Documentação: README da raiz e do backend, e diagramas de arquitetura (`docs/architecture.md`).
- Skills do opencode para replicação de arquitetura de backend e estilo de frontend.
- **Download de capa do álbum**: menu de contexto no placeholder `🎵` do Player com a
  opção "Baixar capa do álbum"; o backend busca a arte pelas APIs do iTunes (fallback:
  Deezer) e salva `cover.<ext>` na pasta do álbum, com recarga automática no Player.
- Bitrate (`kbps`) nas tags ID3 e no tooltip da playlist.
- Tooltip na playlist com os metadados ID3 da faixa (1s de hover, posição ajustada à janela).
- Colunas do cabeçalho da playlist redimensionáveis (Artista | Música | Tempo).
- Edição de letras no painel ("Alterar letra") com persistência via `POST /lyrics`.
- Fallbacks no scraper de letras: variantes "The" (com/sem) e slugs invertido/normal
  na página do artista, ignorando páginas inexistentes (404).

### Refatorado
- Backend reescrito seguindo **Clean Architecture / Clean Code / DDD**: hierarquia
  `domain → application → infrastructure → web`, com ports & adapters (Hexagonal).
  O core ficou isolado de bibliotecas (JLayer, mp3agic, Jsoup) e pronto para migrar
  de armazenamento TXT para banco sem tocar o núcleo.

### Alterado
- Porta da API alterada de `8080` para `8111` (backend, frontend e início automático no Tauri).
- Ajustes no salvamento/cache de letras.
- Javadoc do backend traduzido/adicionado em português.
- Readmes (raiz, backend e frontend) atualizados com as novas funcionalidades.
- `GET /cover` passou a servir também `webp` e `gif`.

### Corrigido
- Nenhum.

### Removido
- Nenhum.

## [0.9.0] — 2026-07-31

### Adicionado
- Empacotamento desktop: compilação do JAR, geração do JRE com `jlink` e distribuição via Tauri.
- O app inicia e encerra o backend automaticamente.