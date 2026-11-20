# PRD — ofy-018 — Nova fonte de letras (LRCLIB) para regravações e músicas fora do catálogo dos sites

> Evolução de `ofy-012` (Letras) e continuação de `ofy-017`. Código-base: v1.6.0 + alterações da ofy-017.

## Visão Geral

Hoje as letras vêm de scraping do Vagalume e do letras.mus.br, que catalogam a música sob o **autor/intérprete original**. Regravações e covers (ex.: *Nara Leão – Ela Desatinou* e *Nara Leão – O Que Será (À Flor Da Pele)*, composições de Chico Buarque) não existem em `/nara-leao/…` nesses sites. As buscas internas dos sites são inviáveis (Vagalume: widget Google só em JavaScript e API com `503`; letras.mus.br: `403` anti-bot), então não há como achá-las pelo artista da tag. Resultado: `Letra não encontrada…` mesmo existindo a letra na web.

Esta atividade adiciona uma **terceira fonte baseada em API JSON pública (LRCLIB, `https://lrclib.net`)**, que tem busca real por faixa/artista/álbum/duração e devolve a letra em texto puro, e uma **normalização do título** para aumentar a taxa de acerto. Beneficia o ouvinte da coleção local cujas faixas são regravações ou têm sufixos no título.

## Objetivos

- *Nara Leão – Ela Desatinou* e *Nara Leão – O Que Será (À Flor Da Pele)* passam a retornar letra pelo botão `Buscar letra`.
- A letra buscada é, sempre que possível, a da **gravação correta** (mesmo artista, álbum/duração compatíveis), e não a de outro intérprete.
- Músicas que já funcionam (Vagalume/letras.mus.br) **não** mudam de comportamento nem ficam mais lentas: a nova fonte só é consultada depois das atuais.
- A fonte pode ser desligada por configuração, como as demais.

## Histórias de Usuário

- Como ouvinte de MPB com muitos covers, quero que `Buscar letra` ache a letra de uma regravação mesmo quando os sites de letras só a catalogam pelo compositor original.
- Como ouvinte, quero que títulos com `?`, `(Ao Vivo)`, `- Remastered 2011` e similares não impeçam a busca.
- Como ouvinte, quero que, se houver mais de uma versão, a letra escolhida seja a da gravação do artista da minha faixa (e, se possível, do meu álbum), para não receber a letra de outra versão.
- Como usuário que prefere não consultar serviços adicionais, quero poder desabilitar a nova fonte em `application.properties`.

## Funcionalidades Principais

1. **RF-018.1 Nova fonte `lrclib.net`:** consultada **depois** de Vagalume e letras.mus.br (prioridade 3), apenas quando as demais não encontraram letra. Usa **somente letra em texto puro** (`plainLyrics`); letra sincronizada (LRC) é ignorada.
2. **RF-018.2 Consulta exata:** com artista (tag/candidatos da `ofy-017`), título normalizado, álbum e duração da faixa (quando disponíveis nas tags), pede a gravação correspondente (`GET /api/get`). Resposta `200` com `plainLyrics` não vazio e `instrumental=false` → letra encontrada.
3. **RF-018.3 Busca por faixa (fallback):** se a consulta exata não retornar letra, busca por título + artista (`GET /api/search`) e, se ainda nada, **por título apenas**. Entre os resultados escolhe, nesta ordem de preferência: (a) mesmo artista (comparação sem acento/caixa) **e** duração dentro de ±5 s; (b) mesmo artista; (c) duração dentro de ±5 s com título normalizado idêntico; (d) primeiro resultado com título normalizado idêntico. Resultados sem `plainLyrics` ou `instrumental=true` são descartados.
4. **RF-018.4 Normalização de título:** variações tentadas em ordem, sem duplicatas: título original; sem `?`/`!`; sem trecho entre parênteses/colchetes finais (`(Ao Vivo)`, `(À Flor Da Pele)`, `[Remastered]`); sem sufixo ` - <ano> Remaster(ed)`/` - Ao Vivo`/` - Live`. Aplicada apenas à nova fonte (as fontes por slug continuam com o título original, ver Fora de Escopo).
5. **RF-018.5 Mensagem e persistência:** a letra encontrada é gravada como hoje (`<Artista> - <Título>.txt`); se nenhuma fonte (inclusive a nova) encontrar, vale o comportamento da `ofy-017` (mensagem amigável, nada gravado).
6. **RF-018.6 Configuração:** `mp3.lyrics.lrclib.{enabled, priority, base-url, user-agent, timeout-connect, timeout-fetch}`; padrão `enabled=true`, `priority=3`, `base-url=https://lrclib.net`.
7. **Critérios de aceite:**
   - (a) `Nara Leão – Ela Desatinou` (álbum *Com Açúcar Com Afeto*, ~169 s) → letra da gravação de Nara Leão, exibida e gravada em `Nara Leão - Ela Desatinou.txt`.
   - (b) `Nara Leão – O Que Será? (À Flor Da Pele)` → letra encontrada (via título normalizado sem `?` e sem o parêntese final) e gravada.
   - (c) `Iggy Pop – The Passenger` e demais músicas já encontradas pelo Vagalume/letras.mus.br: **nenhuma requisição ao LRCLIB** é feita.
   - (d) Música inexistente em todas as fontes → mensagem amigável, nenhum `.txt` criado, nenhuma exceção ao usuário.
   - (e) Com `mp3.lyrics.lrclib.enabled=false` a fonte não é consultada nem listada em `[Scraper] Fontes habilitadas`.
   - (f) Resultado com `instrumental=true` ou `plainLyrics` vazio nunca é gravado como letra.
   - (g) Erro de rede/`5xx`/timeout/JSON inválido do LRCLIB não afeta a busca (retorna "não encontrada") e é registrado em log `WARN`.
   - (h) Testes unitários cobrem normalização de título, escolha entre resultados, tratamento de erro e ordem de fontes (`mvn test` verde).

## Experiência do Usuário

- Sem mudança visual: `Buscar letra` → `Buscando...` → letra. A busca em caso de falha total pode levar alguns segundos a mais (consulta a uma fonte extra, timeout configurado).
- Letras vindas do LRCLIB não são distinguidas na UI (mesma exibição, mesma edição/remoção).

## Restrições Técnicas de Alto Nível

- Integração com serviço externo **sem autenticação**; API pública do LRCLIB pede um `User-Agent` identificando a aplicação — usar um nome genérico do app, **sem dados pessoais** do usuário nem do dispositivo.
- Nenhum dado do usuário além de artista/título/álbum/duração da faixa é enviado; **caminhos de arquivo nunca são enviados**.
- Respeitar timeouts configurados (padrão 8 s conexão / 15 s leitura); sem retry automático nem requisições paralelas (evitar sobrecarga e bloqueio).
- Respeitar `Rules.md` §6: nova fonte só com spec (esta); termos de uso/robots de serviços externos são fatores de risco declarados.
- Mantidos os contratos HTTP de `/lyrics` e o formato do `.txt`.

## Fora de Escopo

- Letra sincronizada (LRC), exibição de karaokê, upload/contribuição de letras ao LRCLIB.
- Outras fontes (Genius, Musixmatch, Letras.mus.br via navegador headless etc.).
- Aplicar a normalização de título às fontes por slug (Vagalume/letras.mus.br) — candidata a atividade futura.
- Busca fuzzy/similaridade de texto além das regras de escolha de RF-018.3.
- Reparar a busca interna do Vagalume/letras.mus.br.
- Cache próprio das respostas do LRCLIB (o `.txt` já é o cache).
- UI para escolher entre múltiplas versões da letra.
- Alterar o frontend.

## Questões em Aberto

- **Termos de uso/política do LRCLIB:** o serviço é público e gratuito, sem chave, mas não foi feita revisão jurídica dos termos; confirmar antes de distribuir o app.
- **Disponibilidade:** a API é mantida pela comunidade; indisponibilidade resulta apenas em "não encontrada" (aceito).
- **Cobertura:** *O Que Será (À Flor Da Pele)* por Nara Leão **não** retornou resultado na busca exata artista+título nos testes de planejamento (só em busca por título, com versões de outros intérpretes). Com a regra RF-018.3(d) a letra de outro intérprete (mesma música/composição) seria aceita; confirmar se esse comportamento é desejado (alternativa: exigir artista igual e deixar "não encontrada").
- **Conteúdo:** letras de outros intérpretes costumam ser idênticas (mesma composição), mas podem divergir em variações/trechos; sem mecanismo de aviso ao usuário.
- **Duração:** depende de `Metadata.durationMs` estar preenchido; sem duração, a escolha (a)/(c) não se aplica.
