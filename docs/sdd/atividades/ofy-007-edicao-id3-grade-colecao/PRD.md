# PRD — ofy-007 — Edição de tags ID3 em grade (Coleção)

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Na tela **Coleção**, ao selecionar um álbum ou artista, as músicas aparecem em uma grade editável (Música, Artista, Álbum, Gênero, Faixa, Disco, Ano). O usuário altera células e grava as mudanças diretamente nos arquivos MP3.

## Objetivos

- Corrigir tags de um álbum/artista inteiro em uma única tela.
- Gravar apenas as linhas alteradas e refletir imediatamente o resultado na interface.

## Histórias de Usuário

- Como colecionador, quero editar o nome do álbum de todas as faixas de uma pasta.
- Como colecionador, quero ver quais linhas foram alteradas antes de salvar.
- Como colecionador, quero saber quantas faixas foram salvas e quantas falharam.

## Funcionalidades Principais

1. **RF-007.1 Seleção:** clicar em um **álbum** (agrupa por pasta) ou **artista** (agrupa por tag `artist`) abre a grade com as músicas correspondentes.
2. **RF-007.2 Edição:** cada célula é um `input` de texto; a linha alterada recebe a classe `dirty` (comparação com `id3Cache`).
3. **RF-007.3 Salvar alterações:** botão `Salvar alterações` envia, **uma a uma**, as linhas sujas para `POST /id3/update` com os 7 campos da linha (vazios incluídos). Sucesso atualiza `id3Cache`; mensagem `N salvo(s)` ou `N salvo(s), M com erro`; sem alterações: `Nenhuma alteração`.
4. **RF-007.4 Gravação no arquivo:** o backend recria a tag **ID3v2.4** do MP3 com os campos recebidos, remove a ID3v1 e substitui o arquivo via arquivo temporário `<arquivo>.mp3tmp`.
5. **Critérios de aceite:** (a) alterar o álbum de 3 linhas e salvar → 3 arquivos regravados e `id3Cache` atualizado; (b) linha igual às tags atuais não é enviada; (c) falha em um arquivo não impede os demais.

## Experiência do Usuário

- Tabela rolável com colunas estreitas para Faixa/Disco/Ano e coluna "Arquivo" somente leitura; título da grade `Álbum: <nome> (N músicas)`; mensagem de resultado ao lado do botão.

## Restrições Técnicas de Alto Nível

- Gravação em arquivos do usuário: operação destrutiva (sem desfazer). O MP3 não pode estar bloqueado pelo SO (ver riscos).
- Campos suportados: título, artista, álbum, gênero, faixa, disco, ano.

## Fora de Escopo

- Edição de capa embutida, comentários e outros frames ID3, desfazer/histórico, validação de formato de ano/faixa.

## Questões em Aberto

- Como a tag é **recriada** só com os campos enviados, quaisquer outros frames existentes (imagem APIC, comentário, letra embutida etc.) são perdidos ao salvar `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Strings vazias são enviadas para campos limpos; o tratamento de string vazia pelo mp3agic não está coberto por teste de UI `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- As edições pendentes são descartadas ao trocar de seleção (efeito dependente de `selected.key`).
