# Changelog

Todas as alterações notáveis deste projeto serão documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e as
versões seguem [Semântic Versioning](https://semver.org/lang/pt-BR/).



## [1.1.0] — 2026-08-05

### Alterado
- implementada a funcionalidade de Edição de ID3 em massa
- alteradas as portas do backend (8111) e frontend (8112)
- alterado tamanho campos ID3

### Corrigido
- fix: alinhar tauri e tauri-build para v2

## [1.0.0] — 2026-08-04

### Alterado
- readmes e changelog
- baixar capa do album
- colunas da playlist, edição da letra
- diagramas
- readmes
- virtual playlists
- nao abrir janela backend
- empacotamento v0.9.0
- info + statusbar
- icones altrerados
- readmes
- logs
- botoes aumentar e diminuir fonte tela de letras
- icones
- conf icones
- letras + readmes
- configs + controles de midia
- primeiro

### Corrigido
- skill build, pequenos ajustes
- ajuste scrapper
- ajuste salvamento letras
- bug fix carregar id3 das musicas ao selecionar a pasta

### Refatorado
- refatoração, criação de skills, changelog, javadoc, readmes
- refatoracao


## [1.1.0] — 2026-08-05

### Alterado
- implementada a funcionalidade de Edição de ID3 em massa
- alteradas as portas do backend (8111) e frontend (8112)
- alterado tamanho campos ID3

### Corrigido
- fix: alinhar tauri e tauri-build para v2

## [0.9.0] — 2026-07-31

### Adicionado
- Empacotamento desktop: compilação do JAR, geração do JRE com `jlink` e distribuição via Tauri.
- O app inicia e encerra o backend automaticamente.