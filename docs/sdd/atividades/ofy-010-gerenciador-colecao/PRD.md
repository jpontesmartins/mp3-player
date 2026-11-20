# PRD — ofy-010 — Gerenciador de coleção (álbuns e artistas)

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Painel **Coleção** (botão da barra de ferramentas) que organiza a biblioteca carregada em três listas — **Álbuns**, **Artistas** e **Playlists** — e serve de ponto de entrada para edição de tags (`ofy-007`, `ofy-008`) e gerenciamento de playlists (`ofy-011`).

## Objetivos

- Dar uma visão agrupada da biblioteca por álbum/artista.
- Oferecer atalhos: carregar toda a biblioteca como playlist, abrir pasta no Explorer, copiar caminho, editar tags em massa.

## Histórias de Usuário

- Como colecionador, quero ver quantos álbuns e artistas tenho e quantas faixas cada um possui.
- Como colecionador, quero clicar com o botão direito em um álbum para abrir sua pasta ou editar as tags em massa.
- Como ouvinte, quero voltar a tocar "todos os arquivos" da biblioteca depois de ver uma playlist.

## Funcionalidades Principais

1. **RF-010.1 Álbuns:** agrupados **por pasta**; o nome exibido é a tag `album` mais frequente na pasta (ou o nome da pasta se não houver); ordenados por nome (`localeCompare`); contagem de faixas.
2. **RF-010.2 Artistas:** valores distintos da tag `artist` (não vazios), com contagem, ordenados por nome.
3. **RF-010.3 Playlists:** primeiro item `Todos os arquivos` (define `playlistFiles = libraryFiles`), seguido das playlists virtuais; clicar numa playlist abre o gerenciador de playlists; botão `Gerenciar playlists`. O título mostra `Playlists (N+1)`.
4. **RF-010.4 Menu de contexto do álbum:** `Abrir pasta no explorer`, `Copiar caminho` e `Editar ID3 em massa` (abre `BulkId3Editor` na pasta do álbum).
5. **RF-010.5 Navegação interna:** botão `Edição de ID3 em massa` abre o editor; subtelas de playlists e edição em massa têm `← Voltar`.
6. **RF-010.6 Seleção:** clicar em álbum/artista abre a grade editável (`ofy-007`).
7. **Critérios de aceite:** (a) com biblioteca carregada, `Álbuns (n)` e `Artistas (m)` refletem os dados; (b) `Todos os arquivos` mostra a contagem total; (c) botão direito num álbum abre o menu com 3 itens.

## Experiência do Usuário

- Três colunas de lista com rolagem, item ativo destacado, contagens à direita; mensagens `Nenhum álbum`/`Nenhum artista`.
- O painel esquerdo é alternado pela barra de ferramentas (Letra / Coleção / Configurações).

## Restrições Técnicas de Alto Nível

- Baseia-se em `libraryFiles` e `id3Cache` (ver `ofy-003`/`ofy-006`); só faz sentido depois do carregamento das tags.

## Fora de Escopo

- Agrupamento por tag de álbum (em vez de pasta), capa nas listas, menu de contexto em artistas, ordenação/filtro nas listas.

## Questões em Aberto

- Dois álbuns na mesma pasta aparecem como um só; o mesmo álbum em pastas diferentes aparece duas vezes (efeito do agrupamento por pasta).
- Ao trocar de painel (ex.: ir para Letra), o `CollectionManager` é desmontado e a seleção/edições pendentes são perdidas `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Clicar numa playlist da lista abre o gerenciador (não a carrega no player).
