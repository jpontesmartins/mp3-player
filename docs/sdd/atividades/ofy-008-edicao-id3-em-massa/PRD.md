# PRD — ofy-008 — Edição de ID3 em massa por padrão de nome de arquivo

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Ferramenta para preencher tags de **todos os MP3 de uma pasta** a partir de um **padrão do nome do arquivo** (ex.: `<track> - <song>`) e/ou de **valores fixos** aplicados a todos, com pré-visualização antes de gravar.

## Objetivos

- Padronizar tags de álbuns inteiros sem digitar arquivo a arquivo.
- Mostrar o resultado esperado antes de gravar e quais arquivos não casam com o padrão.

## Histórias de Usuário

- Como colecionador, quero extrair artista e título do nome `Artista - Música.mp3`.
- Como colecionador, quero definir o mesmo álbum/ano/gênero para todas as faixas.
- Como colecionador, quero abrir a ferramenta direto de um álbum da coleção.

## Funcionalidades Principais

1. **RF-008.1 Entrada:** botão `Edição de ID3 em massa` na Coleção, ou menu de contexto do álbum `Editar ID3 em massa` (pré-preenche a pasta).
2. **RF-008.2 Pasta:** campo de caminho + carregamento via `GET /playlist?path=` (recursivo), mantendo só `.mp3`; sem pasta inicial usa a pasta do primeiro arquivo da biblioteca.
3. **RF-008.3 Padrão:** placeholders `<title>`/`<song>`, `<artist>`, `<album>`, `<genre>`, `<track>`, `<disc>`/`<cd>`/`<disk>`, `<year>`; o restante é texto literal. O padrão é convertido em regex ancorada aplicada ao nome **sem extensão**; o último placeholder é guloso (`(.+)`), os anteriores preguiçosos (`(.+?)`). Placeholder desconhecido invalida o padrão.
4. **RF-008.4 Campos fixos:** para cada uma das 7 tags, valor não vazio sobrepõe o valor extraído do nome.
5. **RF-008.5 Pré-visualização:** tabela com o valor final por arquivo; arquivos que não casam recebem `· não corresponde`; contador `N arquivo(s) .mp3 · M casam com o padrão`.
6. **RF-008.6 Aplicar:** `Aplicar ID3 em massa` envia, para cada arquivo com algum valor final não vazio, **apenas os campos não vazios** a `POST /id3/update` (sequencial); mensagem `N atualizado(s)[, M com erro]`; `Nada para aplicar` se não houver alvos.
7. **Critérios de aceite:** (a) `<artist> - <song>` em `Band - Song.mp3` produz artista `Band` e título `Song`; (b) campo fixo `Álbum` prevalece sobre o padrão; (c) arquivos sem correspondência e sem campos fixos não são alterados.

## Experiência do Usuário

- Tela com campos de pasta, padrão, grupo de campos fixos (largos e estreitos para faixa/disco/ano), tabela de pré-visualização (linhas vazias de preenchimento) e botão Aplicar; botão `← Voltar` retorna à Coleção.

## Restrições Técnicas de Alto Nível

- Mesmas restrições de gravação de `ofy-007` (operação destrutiva, arquivos não podem estar bloqueados).
- A varredura é recursiva: subpastas da pasta informada também são editadas.

## Fora de Escopo

- Renomear arquivos a partir das tags, desfazer, expressões regulares livres, edição de capa.

## Questões em Aberto

- **Risco de apagar tags existentes:** como o backend recria a tag ID3v2.4 apenas com os campos enviados, aplicar só `<artist> - <song>` regrava o arquivo **sem** álbum/ano/gênero/faixa/disco que já existiam `[PENDENTE / AMBÍGUO NO CÓDIGO]` (conclusão por leitura do código; não validada em execução).
- A dica de uso na UI lista as tags `<title>` (ou `<song>`), `<artist>`, `<album>`, `<genre>`, `<track>`, `<disc>` (ou `<cd>`) e `<year>`; o alias `<disk>` é aceito pelo código mas não aparece na dica.
