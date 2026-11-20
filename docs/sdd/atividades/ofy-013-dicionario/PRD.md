# PRD — ofy-013 — Dicionário (significado de palavra da letra)

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Ao selecionar uma palavra na letra e clicar com o botão direito, o usuário abre o submenu **Procurar no dicionário...** e vê o significado em um modal. Hoje só existe o idioma Português (fonte Priberam).

## Objetivos

- Consultar rapidamente o significado de palavras sem sair do player.

## Histórias de Usuário

- Como ouvinte, quero ver o significado de uma palavra da letra com o botão direito.

## Funcionalidades Principais

1. **RF-013.1 Gatilho:** botão direito sobre texto selecionado contendo **uma única palavra** (sem espaços) no painel de letras.
2. **RF-013.2 Menu:** `Procurar no dicionário...` → idiomas vindos de `GET /dictionary/languages` (atualmente `Português`).
3. **RF-013.3 Consulta:** `POST /dictionary/lookup {word, language}`; modal `Significado de <palavra>` com spinner durante a busca.
4. **RF-013.4 Resultado:** significados (linhas separadas por `\n`) e fonte; sem resultado, `Nenhuma definição encontrada.`.
5. **RF-013.5 Fechar:** `Fechar` ou `Esc`.
6. **Critérios de aceite:** (a) palavra válida mostra definições; (b) palavra inexistente mostra a mensagem de vazio; (c) palavra em branco → `400`.

## Experiência do Usuário

- Modal centralizado com título, corpo rolável e botão `Fechar`.

## Restrições Técnicas de Alto Nível

- Scraping de `https://dicionario.priberam.org/<palavra>`; timeout 10 s; User-Agent `Mozilla/5.0 (compatible; MP3Player/1.0)`.

## Fora de Escopo

- Outros idiomas/fontes, histórico, consulta por texto digitado, palavras compostas.

## Questões em Aberto

- Lista de idiomas depende do backend (`["pt"]`); o rótulo `Português` no menu é mapeado no frontend `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Falha de rede e ausência de definição podem ser exibidas igualmente como vazio.
