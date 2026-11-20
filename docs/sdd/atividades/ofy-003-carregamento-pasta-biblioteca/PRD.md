# PRD — ofy-003 — Carregamento de pasta e biblioteca

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Permite informar uma pasta de músicas, varrê-la recursivamente por `.mp3` e carregar a biblioteca na interface, com leitura de tags em lotes e indicador de progresso. A última pasta é lembrada e recarregada na próxima abertura do app.

## Objetivos

- Carregar todos os MP3 de uma pasta (incluindo subpastas).
- Mostrar progresso `n/total` da leitura de tags.
- Restaurar automaticamente a última pasta, tolerando o backend ainda subindo.

## Histórias de Usuário

- Como usuário, quero digitar o caminho da minha pasta de músicas e carregar a biblioteca.
- Como usuário, quero que o app abra já com minha biblioteca anterior.
- Como usuário, quero ver que o app está carregando dados e quantos arquivos faltam.
- Caso extremo: pasta inexistente → o backend responde 400 e nada é carregado.

## Funcionalidades Principais

1. **RF-003.1 Carregar pasta:** em Configurações, campo de texto + botão **Carregar** (ou `Enter`) chama `loadFolderWithRetry(path, true)`, que limpa as listas e executa `LibraryContext.loadFolder`.
2. **RF-003.2 Varredura:** `GET /playlist?path=` devolve caminhos absolutos de todos os `.mp3` (busca recursiva, extensão case-insensitive).
3. **RF-003.3 Estado:** a lista (sem duplicatas) vira `playlistFiles` e `libraryFiles`; a pasta é salva em `localStorage['mp3_folder']`.
4. **RF-003.4 Tags em lote:** arquivos são enviados a `POST /id3/bulk` em lotes de **50**; o resultado alimenta `id3Cache`; `id3Loading/id3Loaded/id3Total` controlam o indicador "Carregando dados... n/total" na lista (ver `ofy-004`). O parâmetro `refresh` recebe `true` quando o carregamento vem do botão Carregar e `false` na restauração automática.
5. **RF-003.5 Restauração:** ao abrir o app, se há `mp3_folder`, tenta carregar; em falha repete a cada 1 s até **15** tentativas. Também carrega a lista de playlists virtuais.
6. **RF-003.6 "Todos os arquivos"/playlist virtual:** ver `ofy-010` e `ofy-011` (alteram `playlistFiles` sem recarregar a pasta; `loadVirtualPlaylist` busca tags dos arquivos ainda sem cache).
7. **Critérios de aceite:** (a) pasta válida com N MP3 → N linhas após o carregamento; (b) progresso chega a N/N; (c) reabrir o app recarrega a mesma pasta; (d) pasta inválida não altera `localStorage` nem lista nova.

## Experiência do Usuário

- Entrada manual do caminho (placeholder `C:\Users\joao_\Music`); não há seletor nativo de pastas.
- Botão muda para "Carregando..." durante a operação. Lista vazia mostra "Nenhum arquivo .mp3 encontrado".

## Restrições Técnicas de Alto Nível

- Apenas `.mp3`. Caminhos absolutos do Windows (o código também trata `/`).
- Lotes de 50 para limitar o tamanho de requisição/tempo por chamada.

## Fora de Escopo

- Seleção de pasta por diálogo nativo, múltiplas pastas, monitoramento de mudanças no disco (file watcher), importação de outros formatos.

## Questões em Aberto

- Falha de carregamento em Configurações não exibe mensagem ao usuário (a API devolve `null` e o hook apenas retorna `false`) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Em falha, `loadFolderWithRetry` já limpou `playlistFiles`/`libraryFiles` antes de tentar `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- `LibraryContext.resetPlaybackState` é um no-op `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
