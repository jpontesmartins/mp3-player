# PRD — ofy-012 — Letras de músicas (busca, cache, edição e remoção)

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Painel **Letra** exibe a letra da música em reprodução: busca automática em sites (Vagalume e letras.mus.br), cache em arquivo `.txt` ao lado do MP3, edição manual, remoção e ajuste de tamanho da fonte.

## Objetivos

- Obter a letra com um clique e reutilizá-la offline depois.
- Permitir corrigir ou remover letras erradas.

## Histórias de Usuário

- Como ouvinte, quero ver a letra da música que está tocando.
- Como ouvinte, quero corrigir a letra quando o site trouxer algo errado.
- Como ouvinte, quero aumentar/diminuir a fonte.
- Como ouvinte, quero remover uma letra salva errada.

## Funcionalidades Principais

1. **RF-012.1 Cache automático:** ao mudar a faixa, `GET /lyrics/cached` mostra a letra salva (se `404`, exibe o botão `Buscar letra`).
2. **RF-012.2 Buscar:** `GET /lyrics?path=` devolve o cache, se existir; senão lê as tags (artista/título; fallback: nome do arquivo `Artista - Título`), consulta as fontes habilitadas em ordem de prioridade crescente (**Vagalume = 1**, **letras.mus.br = 2**) e grava o resultado em `.txt`.
3. **RF-012.3 Edição:** `Alterar letra` abre um `textarea`; `Salvar` → `POST /lyrics {path, text}`; `Cancelar` descarta. A posição de rolagem é preservada entre leitura e edição.
4. **RF-012.4 Remoção:** `Remover letra` (confirmação `Remover letra salva?`) → `DELETE /lyrics?path=`; volta ao estado vazio.
5. **RF-012.5 Fonte:** `A+`/`A-` ajustam de **0.7** a **2.0** rem (passo 0.1; padrão 0.9).
6. **RF-012.6 Dicionário:** selecionar uma palavra e usar o botão direito (ver `ofy-013`).
7. **RF-012.7 Arquivo:** `<pasta do MP3>/<Artista> - <Título>.txt` (tags ID3 preferidas; sem tags, nome do arquivo; caracteres inválidos → `_`).
8. **Critérios de aceite:** (a) letra encontrada é exibida e gravada; (b) reabrir a música mostra o cache sem rede; (c) `Remover` apaga o `.txt`; (d) erro de rede mostra `Erro: ...`.

## Experiência do Usuário

- Estado vazio com botão `Buscar letra` (desabilitado sem música ou durante busca: `Buscando...`); modo conteúdo com cabeçalho de botões fixo e área rolável; texto em `<pre>`.

## Restrições Técnicas de Alto Nível

- Scraping de HTML de terceiros (letras.mus.br e vagalume.com.br) sem autenticação; timeouts em `mp3.lyrics.*` (8 s conexão / 15 s leitura).
- Fontes podem ser desabilitadas por propriedade (`mp3.lyrics.<fonte>.enabled`).

## Fora de Escopo

- Letras sincronizadas (LRC), tradução, outras fontes, histórico de versões, edição de letra embutida na tag (USLT).

## Questões em Aberto

- **Letra "não encontrada" (resolvido em `ofy-017`):** a mensagem não é mais persistida e `.txt` legados com essa mensagem são ignorados; a busca também tenta artistas inferidos do nome do arquivo e usa slug sem acentos.
- `Buscar letra` quando já há letra devolve o **cache** (não refaz a consulta); para buscar de novo é preciso `Remover letra` antes.
- O tamanho da fonte é **lido** de `localStorage['lyrics_font_size']`, mas o código não o **grava** (README afirma persistência) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- `CompositeLyricsScraper` usa referrer `letras.mus.br` e timeout 15000 fixos, ignorando a configuração da fonte; `AbstractLyricsSource#connect` fica sem uso nesse caminho.
