# PRD — ofy-006 — Leitura de tags ID3 e cache de metadados

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Lê as tags ID3 (artista, título, álbum, ano, gênero, faixa, disco, duração e bitrate) dos MP3, individualmente ou em lote, e mantém um **cache em disco** para não reler arquivos inalterados. É a base de dados da lista, da coleção, da busca, das letras e das capas.

## Objetivos

- Ler tags rapidamente para bibliotecas grandes (leitura em lote paralela).
- Evitar releitura de arquivos que não mudaram (cache validado por data de modificação).
- Nunca falhar a listagem por causa de um arquivo problemático.

## Histórias de Usuário

- Como usuário, quero ver artista/título/duração das músicas assim que a biblioteca carrega.
- Como usuário, quero que reabrir o app seja rápido graças ao cache.
- Caso extremo: arquivo sem tags → título assume o nome do arquivo; arquivo ilegível → entrada com `error`.

## Funcionalidades Principais

1. **RF-006.1 Leitura:** `Id3MagicCodec.read` usa mp3agic: ID3v2 primeiro, completando campos vazios com ID3v1; gênero por descrição; disco = *part of set*; duração (ms) e bitrate (kbps) do arquivo.
2. **RF-006.2 Fallback:** sem nenhuma tag, `title` = nome do arquivo (com extensão); exceção de leitura → metadados vazios e log `WARN`.
3. **RF-006.3 Mapa de tags:** `MusicFile.toTagMap()` só inclui chaves não vazias: `title, artist, album, year, genre, track, disc, duration_ms, kbps`.
4. **RF-006.4 Leitura em lote:** `POST /id3/bulk?refresh=` lê com *virtual threads*; falha individual → `{ "error": "Could not read ID3 tags" }`.
5. **RF-006.5 Cache:** `CachedId3Codec` (Decorator) guarda as tags com `_lastModified`; leitura serve do cache se a data do arquivo não mudou; `update` regrava o cache. `Id3Service.getBulk` (sem `refresh`) devolve entradas do cache diretamente.
6. **RF-006.6 Local do cache:** `GET /info` informa `cacheFile`, exibido em Configurações.
7. **Critérios de aceite:** (a) segunda leitura de arquivo não modificado não reabre o MP3 (log `[ID3] Servido do cache`); (b) alterar o arquivo invalida a entrada; (c) cache sobrevive a reinício (arquivo JSON).

## Experiência do Usuário

- Sem UI própria: os dados aparecem na lista, tooltip, coleção e letras. Caminho do cache em Configurações → Informações do sistema.

## Restrições Técnicas de Alto Nível

- Apenas MP3 (mp3agic 0.9.1). Cache em JSON único, em memória + arquivo.
- Lotes de 50 arquivos por requisição (ver `ofy-003`).

## Fora de Escopo

- Leitura de capas embutidas (APIC), tags de outros formatos, limpeza/compactação do cache, banco de dados.

## Questões em Aberto

- O mapa devolvido pelo cache em `getBulk` inclui a chave interna `_lastModified` e **não** revalida a data do arquivo (só `CachedId3Codec.read` revalida) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- `refresh=true` evita o atalho do `Id3Service`, mas `CachedId3Codec.read` ainda serve do cache quando não obsoleto — ou seja, não força releitura `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Fallback de título usa o nome do arquivo **com** extensão `.mp3`.
- `GET /id3` existe mas não é usado pelo frontend; `MetadataCacheRepository.putAll` não é usado pelos serviços.
