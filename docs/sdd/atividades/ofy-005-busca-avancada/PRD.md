# PRD — ofy-005 — Busca avançada na playlist

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Campo de busca na base da lista que filtra a playlist em tempo real, com texto livre, operadores lógicos (`&&`, `||`) e filtros por tag ID3 (`<genre> == rock`). O resultado filtrado pode ser salvo como playlist virtual.

## Objetivos

- Encontrar músicas por qualquer tag sem sair da tela principal.
- Permitir consultas compostas (AND/OR) e comparações numéricas (ano, faixa).
- Transformar uma consulta em playlist persistente.

## Histórias de Usuário

- Como usuário, quero digitar parte do nome do artista/título e ver só as músicas correspondentes.
- Como usuário, quero combinar condições: `<genre> == rock && <year> > 2000`.
- Como usuário, quero listar vários valores: `<artist> == Queen, Beatles`.
- Como usuário, quero salvar o resultado como playlist.

## Funcionalidades Principais

1. **RF-005.1 Gramática:** consulta = termos separados por `&&` (E); cada termo é um grupo de alternativas separadas por `||` (OU). Vazio = sem filtro.
2. **RF-005.2 Texto livre:** substring, case-insensitive (com escape de regex), em `title`, `artist`, `album`, `genre`, `year`, `track`.
3. **RF-005.3 Filtro por tag:** `<tag> <op> <valor>` com `op ∈ {==, !=, <, <=, >, >=}`; tags: `<artist>`, `<title>`/`<song>`, `<album>`, `<year>`, `<genre>`, `<track>`, `<disc>`. Valores separados por vírgula: `!=` exige diferir de **todos**; demais operadores casam com **algum**.
4. **RF-005.4 Comparação:** se ambos os lados são numéricos, compara números; senão compara strings em minúsculas (`==` é igualdade exata, não "contém"). Campo ausente → não casa.
5. **RF-005.5 Resultado:** arquivos sem tags no cache são excluídos quando há consulta. Contador `filtrados / total` aparece quando o filtro reduz a lista.
6. **RF-005.6 Salvar como playlist:** botão (ícone de disquete) visível quando há filtro ativo; abre campo de nome; `Enter` confirma, `Esc` cancela; exige nome não vazio e ao menos 1 resultado; mensagem `Playlist "x" salva (n músicas)`.
7. **Critérios de aceite:** (a) `rock` casa gênero/artista/título contendo "rock"; (b) `<year> >= 2000 && <genre> == rock` aplica ambas; (c) `Esc` no campo limpa a busca; (d) salvar cria `~/.mp3-player/playlists/<nome>.txt` com os caminhos filtrados.

## Experiência do Usuário

- Barra de busca com ícone; expande ao focar/ter texto; placeholder vazio na barra principal; o editor de playlists (`ofy-011`) usa o mesmo motor com placeholder `<genre> == rock && <year> > 2000`.

## Restrições Técnicas de Alto Nível

- Filtro 100% no cliente sobre `id3Cache`; não consulta o backend.
- Separadores `&&`/`||` não podem ser escapados dentro dos valores.

## Fora de Escopo

- Parênteses/precedência customizada, negação de termo livre, busca por duração/bitrate, busca em letras.

## Questões em Aberto

- Disco no formato `1/2` é comparado como string.
- `shared/lib/search.ts` não possui testes unitários dedicados `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- A função `escapeRegex` está duplicada em `search.ts` e `format.ts`.
