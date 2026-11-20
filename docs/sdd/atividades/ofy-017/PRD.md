# PRD — ofy-017 — Bugfix: falha no download de letras (acentos, artista divergente e "não encontrada" persistida)

> Atividade de correção sobre `ofy-012` (Letras). Código-base: v1.6.0.

## Visão Geral

O botão `Buscar letra` falha para várias músicas da coleção do usuário, exibindo `Letra não encontrada para "<título>" de <artista>`. Casos reportados: **Gilberto Gil – Parque Industrial**, **Gilberto Gil – Bat Macumba** e **Nara Leão – Insensatez** (falham) versus **Iggy Pop – The Passenger** (funciona). A análise de código, dos logs (`backend.log`) e de testes reais contra o Vagalume (ver `referencias.md`) mostrou três causas combinadas, e uma quarta que agrava o problema:

1. **Slug com acentos:** o slug do artista/título preserva caracteres acentuados (`nara-leão`, `miserere-nóbis`), mas os sites usam slug ASCII (`nara-leao`). Resultado: URL direta e página do artista nunca são encontradas para qualquer nome com acento (Nara Leão, Tom Zé, Caetano…, "Nóbis", "Geléia").
2. **Artista da tag ≠ artista do site:** em álbuns coletivos (Tropicália) a tag ID3 diz `Gilberto Gil`, mas o Vagalume cataloga *Parque Industrial* e *Bat Macumba* em **Os Mutantes** (`/os-mutantes/parque-industrial.html` e `/os-mutantes/bat-macumba.html` retornam 200). O nome do arquivo (`Gilberto Gil e Os Mutantes - Bat Macumba`) traz o artista correto, porém é ignorado quando a tag está preenchida.
3. **Buscas internas inoperantes:** `vagalume.com.br/search.php?q=` responde `404` e `letras.mus.br/?q=` não devolve resultados; não há plano B quando a URL direta falha.
4. **Mensagem de erro gravada como letra:** o texto `Letra não encontrada…` é persistido em `<Artista> - <Título>.txt` e passa a ser servido como cache, impedindo nova tentativa (inclusive após uma correção do código) até o usuário usar `Remover letra`.

## Objetivos

- `Buscar letra` encontra a letra das três músicas reportadas (Parque Industrial, Bat Macumba, Insensatez) e continua funcionando para Iggy Pop – The Passenger.
- Nomes com acento/cedilha/til deixam de impedir a localização da página.
- Uma busca sem sucesso **não** deixa lixo em disco e pode ser repetida imediatamente.
- Letras "não encontradas" já gravadas por versões anteriores deixam de bloquear uma nova busca.

## Histórias de Usuário

- Como ouvinte com coleção de MPB/Tropicália, quero clicar em `Buscar letra` em *Insensatez* (Nara Leão) e receber a letra, para não precisar colar manualmente.
- Como ouvinte, quero que músicas de álbuns coletivos (artista da tag diferente do artista "dono" da letra no site) também sejam encontradas.
- Como ouvinte, quero poder tentar de novo uma busca que falhou sem ter de apagar arquivos nem usar `Remover letra`.
- Como ouvinte com `.txt` antigos contendo "Letra não encontrada…", quero que um novo clique em `Buscar letra` refaça a consulta.

## Funcionalidades Principais

1. **RF-017.1 Slug ASCII:** o slug usado nas URLs de artista/música remove diacríticos (normalização Unicode NFD) antes de filtrar caracteres. Ex.: `Nara Leão` → `nara-leao`; `Miserere Nóbis` → `miserere-nobis`; `Geléia Geral` → `geleia-geral`. Slugs sem acento permanecem idênticos aos atuais (sem regressão para `iggy-pop`, `the-passenger`).
2. **RF-017.2 Artistas candidatos:** a busca tenta, em ordem e sem duplicatas: (a) o artista da tag ID3; (b) os artistas inferidos do nome do arquivo (parte antes do separador ` - `, descartando prefixo de faixa como `1-05 `/`04 - `, e separando por `,`, ` e `, ` & `, ` feat.`/` ft.`/` part.`, ` x `). Para cada candidato aplica-se o fluxo já existente (URL direta → página do artista → busca).
3. **RF-017.3 Não persistir falha:** quando nenhuma fonte encontra a letra, `GET /lyrics` responde `200` com a mensagem amigável (contrato atual preservado para a UI), **sem** gravar `.txt`.
4. **RF-017.4 Cache legado:** um `.txt` cujo conteúdo é exatamente uma mensagem `Letra não encontrada para "…" de …` é tratado como **ausência de letra** (`GET /lyrics/cached` → `404`; `GET /lyrics` refaz a busca e sobrescreve o arquivo se encontrar).
5. **RF-017.5 Resiliência da busca:** falha HTTP em uma estratégia (ex.: `search.php` com `404`) não aborta as demais estratégias/candidatos da mesma fonte; apenas registra `WARN`.
6. **Critérios de aceite:**
   - (a) `Nara Leão – Insensatez` (tag `Nara Leão`) → letra exibida e salva em `Nara Leão - Insensatez.txt` (o arquivo `.txt` legado existente é sobrescrito).
   - (b) `Gilberto Gil – Parque Industrial` e `Gilberto Gil – Bat Macumba` (tag `Gilberto Gil`; arquivo `… Gilberto Gil e Os Mutantes - Bat Macumba.mp3`) → letra encontrada via artista inferido `Os Mutantes`.
   - (c) `Iggy Pop – The Passenger` continua retornando a letra pela URL direta, com o mesmo número de requisições (1 tentativa) que hoje.
   - (d) Quando nenhuma fonte tem a letra: resposta `200` com `Letra não encontrada para "<título>" de <artista>`, **nenhum** `.txt` criado, e novo clique em `Buscar letra` repete a consulta.
   - (e) `.txt` legado com a mensagem de "não encontrada" → `GET /lyrics/cached` devolve `404` e o botão `Buscar letra` aparece.
   - (f) Um `.txt` com letra real (ou editada manualmente) **nunca** é tratado como ausência.
   - (g) Testes unitários cobrem slug com acento, candidatos de artista, não persistência e cache legado (`mvn test` verde).

## Experiência do Usuário

- Sem mudança visual. Fluxo: `Buscar letra` → `Buscando...` → letra exibida; em falha, a mensagem amigável é exibida como hoje, porém o botão `Buscar letra` continua disponível ao reabrir a música (porque nada foi gravado).
- Limitação conhecida de UX (já existente): após uma falha, a mensagem aparece no painel; ao trocar de música e voltar, o painel volta ao estado vazio com `Buscar letra`.

## Restrições Técnicas de Alto Nível

- Mantidos os contratos HTTP de `/lyrics` (`docs/sdd/TechSpec.md`) e o formato de persistência `<pasta do MP3>/<Artista> - <Título>.txt`.
- Scraping de HTML de terceiros sem autenticação; respeitar timeouts configurados; **não** adicionar novas fontes (Rules §6).
- Aumentar o número de candidatos aumenta o nº de requisições apenas em caso de falha; o caminho feliz (primeiro candidato acerta) não pode ficar mais lento.
- Sem regra de negócio em controller; mudanças em `application/` e `infrastructure/` respeitando `web → application → domain ← infrastructure`.

## Fora de Escopo

- Novas fontes de letras ou uso de APIs oficiais (ex.: API do Vagalume, hoje respondendo `503`).
- Reparar `search.php` do Vagalume ou a busca do letras.mus.br (endpoints de terceiros) — apenas tolerar a falha.
- Busca "por título apenas" sem artista, correspondência aproximada (fuzzy) de títulos, remoção de sufixos como `(Ao Vivo)`/`- Remastered` (candidatos a atividade futura).
- Alterar a UI (`LyricsPanel`), o tamanho de fonte ou a edição manual de letras.
- Corrigir o referrer fixo / timeout fixo do `CompositeLyricsScraper` (registrado em `ofy-012` como pendência).
- Corrigir tags ID3 do usuário.

## Questões em Aberto

- **Verificação no letras.mus.br:** o ambiente de análise recebeu `403` (anti-bot) do letras.mus.br em `curl`, então a correção de slug para essa fonte foi inferida do padrão de URL (`/nara-leao/insensatez/`) e deve ser confirmada em execução real pelos logs `[letras.mus.br]`. As três músicas devem ser localizáveis ao menos pelo Vagalume (confirmado).
- **Parque Industrial / Bat Macumba só existem sob outro artista no site:** a heurística de "artista inferido do nome do arquivo" depende de o arquivo seguir o padrão `[faixa] Artista(s) - Título`. Arquivos fora do padrão continuarão dependendo apenas da tag `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- **Remoção do texto legado:** o `.txt` com a mensagem de "não encontrada" será ignorado (não apagado) até que uma nova letra seja encontrada. Confirmar se isso é aceitável ou se deve ser apagado ao ser detectado.
- **Pasta da atividade:** esta atividade usa o nome `ofy-017` (sem slug `kebab-case`), contrariando o padrão `ofy-XXX-<slug>` de `Rules.md`; sugestão de renomeação: `ofy-017-bugfix-download-letras`.
