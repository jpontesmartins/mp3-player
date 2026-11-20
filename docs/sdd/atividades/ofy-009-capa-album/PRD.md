# PRD — ofy-009 — Capa do álbum (exibição e download)

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Mostra no Player a capa da música em reprodução, a partir de um arquivo de imagem na mesma pasta do MP3, e permite **baixar automaticamente** a capa (iTunes, com fallback no Deezer) salvando-a como `cover.<ext>` na pasta.

## Objetivos

- Exibir a capa quando existir um arquivo de imagem conhecido na pasta.
- Facilitar a obtenção de capas ausentes com um clique.

## Histórias de Usuário

- Como ouvinte, quero ver a capa do álbum enquanto a música toca.
- Como ouvinte, quero baixar a capa quando ela não existir.
- Como usuário, quero desabilitar a exibição de capas nas configurações.

## Funcionalidades Principais

1. **RF-009.1 Exibição:** `GET /cover?path=<mp3>` procura na pasta do MP3 um arquivo cujo nome (sem extensão) seja `cover`, `folder`, `album`, `front`, `art` ou `artwork` e extensão `jpg`, `jpeg`, `png`, `webp` ou `gif` (case-insensitive); devolve a imagem ou `404`. Se a imagem falha, o `<img>` é ocultado e aparece um placeholder `🎵`.
2. **RF-009.2 Download:** clique com o botão direito na capa → **Baixar capa do álbum** → `POST /cover/download {path}`. Consulta: `artista + " " + álbum` (apenas artista se o álbum for vazio ou igual ao artista; se não houver tags, artista extraído do nome `Artista - Título`). Tenta iTunes e, se falhar, Deezer; grava `cover.<jpg|png|webp|gif>` pela *content-type*, sobrescrevendo.
3. **RF-009.3 Mensagens:** `Baixando capa...` → `Capa baixada.` ou `Erro: <detalhe>` por 4 s; em sucesso a imagem é recarregada (`?v=<n>`).
4. **RF-009.4 Habilitar/desabilitar:** `showCover` (Configurações) controla a exibição; sem música corrente, nada é exibido.
5. **Critérios de aceite:** (a) pasta com `cover.jpg` → capa visível; (b) download bem-sucedido cria `cover.<ext>` e atualiza a imagem; (c) nenhum resultado → mensagem `Nenhuma imagem encontrada para "<consulta>"`.

## Experiência do Usuário

- Container de capa com `title` "Clique com o botão direito para baixar a capa do álbum"; menu de contexto com ícone de nuvem; mensagem de status sobre a capa.

## Restrições Técnicas de Alto Nível

- Depende de iTunes Search API e Deezer API públicas, sem autenticação; timeouts: conexão 15 s e download 25 s (`mp3.cover.*`).
- Capa é arquivo na pasta, não é embutida na tag.

## Fora de Escopo

- Leitura de capa embutida (APIC), upload/escolha manual de imagem, múltiplas capas por álbum, cache/redimensionamento.

## Questões em Aberto

- Se já existir outra imagem aceita (ex.: `folder.jpg`), o download cria `cover.<ext>` mas `GET /cover` devolve o **primeiro** arquivo encontrado por `Files.list` (ordem não garantida) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Sem artista, álbum ou padrão de nome, a consulta pode ser vazia e a busca externa é feita assim mesmo `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- `GET /cover` contém a lógica de busca de arquivo no próprio controller (fora da camada `application`).
