# Changelog

Todas as alterações notáveis deste projeto serão documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e as
versões seguem [Semântic Versioning](https://semver.org/lang/pt-BR/).



## [1.2.3] — 2026-08-13

### Alterado
- externalizadas propriedades de obtencao de capa e letras
- melhroada a cobertura de testes
- alterado o nome; adicionados mais testes;

### Corrigido
- corrigido para manter o tamanho da fonte da tela de letras
- ajustes visuais na tela de configurações; adicionada opção de rolagem para a música corrente

## [1.2.2] — 2026-08-11

### Alterado
- endereço para a rede

### Corrigido
- fix: tamanho dos campos faixa, disco e ano
- ajuste changelog

## [1.2.1] — 2026-08-06

### Adicionado
- Coluna de quantidade de CDs no ID3
- Cache para metadados

## [1.2.0] — 2026-08-06

### Alterado
- alterado para usar virtual threads no carregamento dos id3
- adicionada funcionalidade de temas. claro e escuro

## [1.1.1] — 2026-08-05

### Alterado
- adicionadas informações de log e portas nas configurações

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

## [0.9.0] — 2026-07-31

### Adicionado
- Empacotamento desktop: compilação do JAR, geração do JRE com `jlink` e distribuição via Tauri.
- O app inicia e encerra o backend automaticamente.
