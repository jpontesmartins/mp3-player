# PRD — ofy-011 — Playlists virtuais (criação, edição e exclusão)

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Permite criar listas de músicas nomeadas ("playlists virtuais"), persistidas como arquivos `.txt`, editá-las em um editor de duas colunas com arrastar e soltar, carregá-las no player e excluí-las. Há três pontos de criação/alteração: gerenciador de playlists, salvar resultado de busca (`ofy-005`) e "Adicionar à playlist" no menu de contexto (`ofy-004`).

## Objetivos

- Montar coleções personalizadas sem mover arquivos.
- Persistir playlists de forma simples e legível (um caminho por linha).

## Histórias de Usuário

- Como usuário, quero criar uma playlist arrastando músicas da biblioteca.
- Como usuário, quero editar e salvar uma playlist existente.
- Como usuário, quero carregar uma playlist no player.
- Como usuário, quero excluir uma playlist com confirmação.

## Funcionalidades Principais

1. **RF-011.1 Listar:** `GET /playlists` → nomes (arquivos `.txt`, sem extensão, ordenados).
2. **RF-011.2 Nova playlist:** campo de nome + `Nova playlist` abre o editor com o nome preenchido.
3. **RF-011.3 Editor:** painel esquerdo `Todas as músicas (n)` (biblioteca completa, com busca avançada) e direito `Na playlist (n)`. Adicionar por botão `Adicionar` ou **arrastar** da esquerda para a direita (implementado com eventos de mouse e "fantasma" do item); itens já presentes ficam desabilitados (`Na lista`); `Remover` tira da direita.
4. **RF-011.4 Salvar:** exige nome e ao menos 1 música (`Informe um nome para a playlist` / `Adicione pelo menos uma música`); `POST /playlist` sobrescreve o arquivo; mensagem `Playlist "x" salva (n músicas)`; lista de playlists é atualizada.
5. **RF-011.5 Carregar:** `Carregar` (na lista ou no editor) → `GET /playlist/{name}` → `playlistFiles` passa a ser a playlist; tags dos arquivos ainda sem cache são lidas em lote.
6. **RF-011.6 Editar/Excluir:** `Editar` abre o editor com as músicas atuais; `Excluir` pede confirmação (`Excluir playlist <nome>?`) e chama `DELETE /playlist/{name}`; mensagem `Playlist "x" excluída`.
7. **RF-011.7 Nomes:** o backend sanitiza o nome do arquivo (`\ / : * ? " < > |` → `_`; vazio → `playlist`).
8. **Critérios de aceite:** (a) salvar 3 músicas cria `<nome>.txt` com 3 linhas; (b) arrastar para a direita adiciona sem duplicar; (c) carregar substitui a lista exibida; (d) excluir remove o arquivo.

## Experiência do Usuário

- Duas subtelas (lista/editor) dentro de Coleção; arrastar mostra um rótulo seguindo o cursor e destaca a área de soltura (`Solte aqui para adicionar`).

## Restrições Técnicas de Alto Nível

- Armazenamento: `~/.mp3-player/playlists/<nome>.txt` (diretório do usuário), UTF-8, um caminho absoluto por linha.
- O painel esquerdo do editor só oferece arquivos da biblioteca carregada.

## Fora de Escopo

- Reordenar músicas dentro da playlist, playlists com metadados, importação/exportação M3U, rename na interface.

## Questões em Aberto

- `POST /playlist/rename` existe e tem teste, mas **não é usado** pelo frontend; alterar o nome no editor e salvar **cria outra playlist** e mantém a antiga `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- `GET /playlist/{name}` retorna `200 []` para playlist inexistente (o ramo `404` só ocorre por exceção); `DELETE` de inexistente retorna `200 "Deleted"`.
- Nomes diferentes que colidem após sanitização (ex.: `a/b` e `a_b`) apontam para o mesmo arquivo.
