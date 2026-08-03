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

### Refatorado
- Backend reescrito seguindo **Clean Architecture / Clean Code / DDD**: hierarquia
  `domain → application → infrastructure → web`, com ports & adapters (Hexagonal).
  O core ficou isolado de bibliotecas (JLayer, mp3agic, Jsoup) e pronto para migrar
  de armazenamento TXT para banco sem tocar o núcleo.

### Alterado
- Porta da API alterada de `8080` para `8111` (backend, frontend e início automático no Tauri).
- Ajustes no salvamento/cache de letras.
- Javadoc do backend traduzido/adicionado em português.

### Corrigido
- Nenhum.

### Removido
- Nenhum.

## [0.9.0] — 2026-07-31

### Adicionado
- Empacotamento desktop: compilação do JAR, geração do JRE com `jlink` e distribuição via Tauri.
- O app inicia e encerra o backend automaticamente.