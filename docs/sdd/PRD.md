# PRD — ovelhafy (Visão Geral do Produto)

## Visão Geral

O **ovelhafy** é um aplicativo desktop (Windows) para **organizar e ouvir uma coleção local de arquivos MP3**. Ele resolve o problema de manter uma biblioteca de músicas própria — com tags ID3 corretas, capas de álbum, letras, playlists e busca flexível — sem depender de serviços de streaming nem de banco de dados externo. O público é o próprio dono da coleção (usuário doméstico/entusiasta de música local, com idioma da interface em português do Brasil).

O produto é distribuído como instalador (MSI/NSIS via Tauri) que embute o backend Java e um JRE mínimo, sendo auto-contido: não exige Java instalado nem serviços externos (exceto internet para letras, capas e dicionário). Versão documentada: **1.6.0**.

## Objetivos

- Reproduzir MP3 locais com controles de mídia completos (tocar, pausar, parar, avançar, voltar, buscar posição) e modos de reprodução contínua, aleatória e repetição.
- Manter a biblioteca organizada: ler e editar tags ID3 (individualmente, em grade por álbum/artista e em massa por padrão de nome de arquivo).
- Enriquecer a coleção: baixar capas de álbum e letras de músicas, consultar significado de palavras da letra.
- Permitir montar e gerenciar playlists virtuais, inclusive a partir de resultados de busca avançada.
- Funcionar offline para tudo que é local, persistindo dados em arquivos simples no disco do usuário.
- Sucesso observável (extraído do comportamento atual): biblioteca de uma pasta carregada com progresso de leitura de tags; cache de metadados que evita reler arquivos inalterados; app que inicia e encerra o backend sozinho.
- Métricas de produto/telemetria: `[PENDENTE / AMBÍGUO NO CÓDIGO]` — não existe coleta de métricas no código (o README cita estatísticas de uso apenas como ideia futura).

## Histórias de Usuário

- Como **ouvinte**, quero escolher uma pasta e tocar qualquer MP3 dela para ouvir minha coleção local.
- Como **ouvinte**, quero que a próxima música toque sozinha (ou aleatória, ou repetindo a atual) para não precisar operar o player continuamente.
- Como **colecionador**, quero editar artista, título, álbum, gênero, faixa, disco e ano de várias músicas de uma vez para corrigir tags rapidamente.
- Como **colecionador**, quero preencher tags a partir do nome do arquivo (ex.: `<artist> - <song>`) para padronizar álbuns inteiros.
- Como **ouvinte**, quero ver a capa do álbum e baixá-la quando faltar.
- Como **ouvinte**, quero ler (e corrigir) a letra da música em reprodução e consultar o significado de uma palavra.
- Como **organizador**, quero buscar com operadores (`&&`, `||`, `<genre> == rock`) e salvar o resultado como playlist.
- Como **usuário**, quero alternar entre tema escuro e claro e saber onde ficam log e cache.
- Casos extremos cobertos hoje: pasta inexistente, arquivo sem tags (usa nome do arquivo), letra não encontrada, backend ainda subindo (o frontend tenta restaurar a última pasta até 15 vezes, 1 s entre tentativas).

## Funcionalidades Principais

Mapa consolidado de tudo que existe no código (cada linha tem especificação própria em `docs/sdd/atividades/`):

| ID | Atividade | O que faz (alto nível) | Camadas |
|---|---|---|---|
| ofy-001 | `reproducao-mp3` | Play/pause/resume/stop/seek e barra de progresso de MP3 locais via JLayer | backend `player/controls` + frontend `playback` |
| ofy-002 | `navegacao-modos-autoplay` | Anterior/próxima, histórico, modos contínuo/aleatório/repetição, autoplay ao fim da faixa | frontend `playback` |
| ofy-003 | `carregamento-pasta-biblioteca` | Escaneia pasta recursivamente, restaura última pasta, carrega tags em lotes com progresso | frontend `load-folder` + `LibraryContext` + backend `playlist` |
| ofy-004 | `playlist-view-lista-reproducao` | Grade Artista/Música/Tempo com colunas redimensionáveis, tooltip de tags, menu de contexto | frontend `playlist-view` |
| ofy-005 | `busca-avancada` | Filtro com `&&`, `\|\|`, filtros por tag e comparadores; salvar resultado como playlist | frontend `shared/lib/search.ts` + `playlist-view` |
| ofy-006 | `tags-id3-leitura-cache` | Leitura de ID3 (v2 com fallback v1), leitura em lote com virtual threads, cache em disco | backend `player/music` |
| ofy-007 | `edicao-id3-grade-colecao` | Edição de tags em grade (por álbum/artista) e gravação no MP3 | frontend `collection` + backend `/id3/update` |
| ofy-008 | `edicao-id3-em-massa` | Edição de tags de uma pasta por padrão de nome de arquivo e campos fixos | frontend `collection` |
| ofy-009 | `capa-album` | Exibição de capa da pasta e download (iTunes → Deezer) | backend `/cover*` + frontend `playback/CoverArt` |
| ofy-010 | `gerenciador-colecao` | Painel Coleção: álbuns, artistas, "Todos os arquivos", atalhos | frontend `collection` |
| ofy-011 | `playlists-virtuais` | CRUD de playlists `.txt`, editor de duas colunas com arrastar e soltar, adicionar via menu de contexto | backend `player/playlist` + frontend |
| ofy-012 | `letras` | Buscar (Vagalume → letras.mus.br), cachear em `.txt`, editar, remover, ajustar fonte | backend `lyrics` + frontend `lyrics` |
| ofy-013 | `dicionario` | Significado de palavra selecionada na letra (Priberam, português) | backend `dictionary` + frontend |
| ofy-014 | `configuracoes-tema-sobre` | Tema, modo de reprodução, mostrar capa, carregar pasta, info do sistema, "Sobre", rodapé de versão | frontend `settings`/`app-info` + `/info` |
| ofy-015 | `shell-tauri-backend-embarcado` | Janela Tauri; inicia/encerra o backend Java embarcado; abre pasta no Explorer | `frontend/src-tauri` |
| ofy-016 | `build-release-versionamento` | Versionamento semântico, changelog, JAR + JRE (`jlink`) + instaladores | `scripts/build-release.ps1` |

Requisitos funcionais transversais:
1. RF-01: toda a persistência é local em arquivos (sem banco de dados).
2. RF-02: o backend só é acessado pelo frontend via HTTP em `localhost:8111`.
3. RF-03: interface em português do Brasil, tema escuro (padrão) ou claro.
4. RF-04: o app é auto-contido ao ser instalado (JAR + JRE embarcados).

## Experiência do Usuário

- **Persona única:** dono de uma coleção local de MP3, usando o app em desktop Windows.
- **Layout:** janela 1400×800 (mínimo 1000×600). Barra de ferramentas com 4 botões (Letra, Coleção, Configurações, Sobre); painel esquerdo troca entre Letra/Coleção/Configurações; painel direito fixo com Player (capa, controles, progresso, status) e Playlist (lista, rodapé com contagem/duração, barra de busca). Rodapé com a versão.
- **Fluxos principais:** (1) Configurações → informar pasta → Carregar → clicar numa música; (2) Coleção → escolher álbum → editar grade → Salvar; (3) Letra → Buscar letra → editar/remover; (4) Busca → salvar resultado como playlist.
- **Interações:** menus de contexto com ícones (música, álbum, capa, palavra da letra), tooltip de tags após 1 s de hover, arrastar e soltar no editor de playlists, atalhos `Esc` (fechar modais/limpar busca) e `Enter` (confirmar campos de pasta/nome).
- **Acessibilidade:** não há requisitos nem atributos ARIA dedicados no código `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

## Restrições Técnicas de Alto Nível

- **Plataforma:** instaladores Windows (MSI/NSIS); o shell procura `java.exe` e usa `CREATE_NO_WINDOW`, portanto não há suporte efetivo a outros SOs no empacotamento atual.
- **Integrações externas obrigatórias para recursos opcionais:** letras.mus.br e vagalume.com.br (scraping HTML), dicionario.priberam.org (scraping HTML), iTunes Search API e Deezer API (JSON). Sem autenticação.
- **Portas fixas:** backend 8111, frontend de desenvolvimento 8112.
- **Privacidade/segurança:** dados não saem da máquina, exceto consultas a sites externos (artista/título/palavra). O backend escuta em `0.0.0.0` quando iniciado pelo Tauri, com CORS aberto e sem autenticação — ver `Rules.md` seção 6.
- **Formato suportado:** apenas `.mp3` (varredura por extensão); decodificação via JLayer e tags via mp3agic.
- **Performance:** leitura de tags em lotes de 50 arquivos, virtual threads no backend, polling de status a cada 1 s. Metas formais de latência/volume: `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- **Sem banco de dados:** decisão explícita do projeto (README); SQLite foi citado apenas como possibilidade futura.

## Fora de Escopo

- Streaming, contas de usuário, sincronização em nuvem, múltiplos usuários.
- Formatos de áudio diferentes de MP3 (FLAC, AAC, OGG etc.).
- Equalizador, volume/mute no app, fila de reprodução separada da playlist carregada (não existem no código).
- Estatísticas de uso (músicas mais tocadas) — citado no README apenas como ideia futura.
- Edição de capa embutida (APIC) nas tags; capas são arquivos `cover.<ext>` na pasta.
- Suporte a outros idiomas de dicionário (existe apenas `pt`) e outras fontes de letras além das duas configuradas.
- Execução em macOS/Linux e instalador multiplataforma.

## Questões em Aberto

Divergências entre documentação existente e código (o código prevalece) e comportamentos ambíguos identificados:

- `README.md` cita o arquivo de cache `~/.mp3-player/metadata-cache.json`; o código usa `music-cache.json` e, no app instalado, o grava **ao lado do log** (`<app_log_dir>/music-cache.json`). `README.md` também afirma letras apenas de letras.mus.br; o código tem **duas** fontes (Vagalume prioridade 1, letras.mus.br prioridade 2). `docs/architecture.md` também omite Vagalume.
- `README.md` lista "Cache de metadados" em `player/domain`; no código está em `player/music/infrastructure`.
- `shared.domain.model.Settings` (modo de reprodução + mostrar capa) existe com testes, mas **não é usado** por nenhum controller/serviço; as preferências vivem só no estado do frontend e não persistem entre execuções (exceto o tema). `[PENDENTE / AMBÍGUO NO CÓDIGO]`
- Endpoints sem consumidor no frontend atual: `GET /id3`, `POST /playlist/rename`, `GET /dictionary/languages`.
- Edição ID3 em massa e individual **recriam** a tag ID3v2.4 do arquivo apenas com os campos enviados (detalhes e riscos em `ofy-007` e `ofy-008`).
- Letra "não encontrada" é tratada como texto e **gravada como letra em cache** (detalhe em `ofy-012`).
- Acessibilidade, métricas, lint/typecheck dedicados e testes das telas de Coleção/Letras/Configurações não existem.
