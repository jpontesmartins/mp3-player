# Project Rules & Architectural Guidelines

> Projeto **ovelhafy** (repositório `mp3-player`) — gerenciador e tocador de coleção local de MP3. Este documento descreve as regras reais observadas no código na versão **1.6.0**. Pontos que o código não resolve de forma clara estão marcados como `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

---

## 1. Visão Geral da Stack e Módulos
- **Linguagens e Runtimes:**
  - Java 21 (uso de virtual threads, `record`, pattern matching) — backend.
  - TypeScript (`strict`, `noUnusedLocals`, `noUnusedParameters`, alvo ES2020) — frontend.
  - Rust (edition 2021) — shell nativo Tauri.
  - PowerShell — script de release (`scripts/build-release.ps1`). Plataforma-alvo efetiva: **Windows** (o shell Tauri procura `jre/bin/java.exe`).
- **Frameworks e Bibliotecas Principais:**
  - Backend: Spring Boot 3.3.5 (`spring-boot-starter-web`), JLayer 1.0.1 (decodificação/reprodução MP3), mp3agic 0.9.1 (tags ID3), Jsoup 1.18.1 (HTTP + scraping), Lombok (`@Data`, `@EqualsAndHashCode`), Maven. Testes: JUnit 5, Mockito, `spring-boot-starter-test`.
  - Frontend: React 18.3, Vite 6, `@vitejs/plugin-react`, MUI (`@mui/icons-material` — usado **apenas para ícones**), CSS puro com custom properties. Testes: Vitest 5 + Testing Library + jsdom.
  - Shell nativo: Tauri v2, `tauri-plugin-opener`, `serde`/`serde_json`.
- **Mapa de Diretórios:**
  - `backend/`: API REST (Spring Boot) em `com.ovelha.fy`, organizada por módulos de negócio.
  - `backend/src/main/java/com/ovelha/fy/player/`: módulo núcleo, com subdomínios `controls` (reprodução), `music` (ID3, capas, cache) e `playlist` (pastas e playlists virtuais); `player/domain` guarda `MusicFile` e a porta `PlayerEngine`.
  - `backend/src/main/java/com/ovelha/fy/lyrics/`: bounded context de letras (scraping + persistência `.txt`).
  - `backend/src/main/java/com/ovelha/fy/dictionary/`: bounded context de dicionário (Priberam).
  - `backend/src/main/java/com/ovelha/fy/shared/`: configurações Spring (`config/`), `Settings` e utilitário `MusicFileNaming` (`domain/`).
  - `backend/src/main/resources/application.properties`: porta e propriedades externalizadas de scraping/capas.
  - `backend/src/test/java/`: testes unitários espelhando a estrutura de pacotes.
  - `frontend/src/app/`: composição da aplicação (`App.tsx`, `App.css`) e providers de estado global.
  - `frontend/src/widgets/`: composição de telas (`Toolbar`, `LeftPanel`, `RightPanel`).
  - `frontend/src/features/<feature>/{ui,lib,__tests__}`: fatias de funcionalidade (Feature-Sliced Design): `app-info`, `collection`, `load-folder`, `lyrics`, `playback`, `playlist-view`, `settings`.
  - `frontend/src/shared/{api,lib,types,ui}`: clientes HTTP, utilitários puros, tipos da API e componentes reutilizáveis.
  - `frontend/src-tauri/`: shell nativo Rust (`src/lib.rs`), `tauri.conf.json`, `capabilities/default.json`, ícones e `resources/` (JRE + `backend.jar`, gerados no build).
  - `scripts/`: automação de release.
  - `docs/`: `architecture.md` (diagramas Mermaid do backend) e `docs/sdd/` (especificações SDD).
  - `.opencode/skills/`: skills de apoio a agentes (`backend-arch`, `backend-dev`, `frontend-style`, `build-release`) com convenções complementares.

## 2. Fronteiras Arquiteturais e Comunicação entre Camadas
- **Fluxo de Dependência:**
  - Fluxo geral: `Frontend (React, WebView do Tauri) → HTTP REST (localhost:8111) → Backend (Spring Boot) → sistema de arquivos / sites externos`. O shell Rust **não** intermedia chamadas de negócio: apenas inicia/encerra o processo Java e expõe o plugin `opener`.
  - Dentro de cada módulo do backend: `web → application → domain ← infrastructure`. `domain` não depende de nada; `application` conhece apenas `domain`; `infrastructure` implementa as portas (`port/`) e repositórios (`repository/`) de `domain`.
  - Entre módulos do backend: `lyrics` depende de `player` apenas via portas/modelos (`Id3Codec`, `MusicFile`) e do util `shared.domain.util.MusicFileNaming`.
  - Frontend: `app → widgets → features → shared`. Features consomem estado via providers (`useApp`, `useLibrary`, `usePlayer`) e a rede via `shared/api`.
- **Mecanismos de Integração:**
  - Frontend ↔ Backend: HTTP/JSON (e `text/plain` em `/play`, `/pause`, `/resume`, `/stop`), base fixa `http://localhost:8111` definida em `frontend/src/shared/api/client.ts`. Não há WebSocket/SSE: o estado de reprodução é obtido por **polling** de `GET /playing` a cada 1 s.
  - Frontend ↔ Shell nativo: apenas o plugin `@tauri-apps/plugin-opener` (`revealItemInDir`). Não existem `#[tauri::command]` próprios nem eventos customizados.
  - Shell nativo ↔ Backend: o Rust inicia `resources/jre/bin/java.exe -jar resources/backend.jar` passando `--server.address=0.0.0.0 --server.port=8111 --mp3.frontend-port=8112 --mp3.log-file=<app_log_dir>/backend.log`, redireciona stdout/stderr para esse arquivo e mata o processo no `RunEvent::Exit`.
  - Backend ↔ Externos: Jsoup para letras.mus.br, vagalume.com.br, dicionario.priberam.org, iTunes Search API e Deezer API.
- **Isolamento de Responsabilidades:**
  - Lógica de negócio/domínio: `domain/` e `application/` de cada módulo do backend. Regras de navegação entre faixas (modos contínuo/aleatório/repetição, histórico) e de busca avançada vivem **no frontend** (`features/playback/lib/navigation.ts`, `shared/lib/search.ts`) — decisão existente, manter nesses locais.
  - Apresentação/interface: exclusivamente em `frontend/src` (componentes em `ui/`, orquestração em hooks em `lib/`, composição em `widgets/`). Controllers do backend só traduzem HTTP ↔ serviço.
  - Acesso a recursos externos, SO e persistência: `infrastructure/` do backend (arquivos, JLayer, mp3agic, Jsoup) e `src-tauri/src/lib.rs` (processo filho, diretório de logs, plugin opener).
- **Proibições Arquiteturais:**
  - `domain/` não pode importar Spring, Jsoup, mp3agic, JLayer nem tipos HTTP. **Exceção existente:** `lyrics.domain.port.LyricsSource` importa `org.jsoup.nodes.Document` `[PENDENTE / AMBÍGUO NO CÓDIGO]` — não replicar em novos ports.
  - `application/` nunca instancia implementações concretas (`new`) nem importa `infrastructure/`; depende de portas via construtor.
  - Controllers não contêm regra de negócio nem acessam `infrastructure/` diretamente. **Exceção existente:** `MetadataController.getCover` (`GET /cover`) faz a busca do arquivo de capa na pasta diretamente no controller `[PENDENTE / AMBÍGUO NO CÓDIGO]` — não replicar o padrão.
  - Componentes React (`ui/`) não chamam `fetch` diretamente; usam `shared/api/*` via hooks. Exceção existente: `shared/api/cover.ts` e `shared/api/id3.ts` fazem `fetch` próprio dentro de `shared/api` (permitido, pois continuam na camada de API).
  - O frontend não deve acessar o sistema de arquivos; qualquer acesso a arquivos passa pelo backend (ou pelo plugin `opener` para revelar no Explorer).
  - Não adicionar `#[tauri::command]` para lógica de domínio sem atualizar `docs/sdd/TechSpec.md` e esta seção.

## 3. Padrões de Código e Convenções
- **Estilo e Nomenclatura:**
  - Backend: pacote raiz `com.ovelha.fy`; módulo → camada (`application`, `domain`, `infrastructure`, `web`); ports em `domain/port`, repositórios em `domain/repository`, modelos em `domain/model`. Implementações com prefixo de tecnologia/estratégia (`File*Repository`, `JLayer*`, `Composite*`, `Abstract*`). Controllers `*Controller`, serviços `*Service`. DTOs de requisição declarados como `record` aninhado no controller (ex.: `Id3UpdateRequest`). Mensagens de log com prefixo de contexto, ex.: `[Player]`, `[ID3]`, `[Capa]`, `[Lyrics]`.
  - Frontend: componentes React em `PascalCase.tsx` (export default), hooks `useXxx.ts(x)`, utilitários em `camelCase.ts`; módulos de API exportam funções (`import * as playbackApi`). Ids/classes CSS em `kebab-case`. Textos de UI em português do Brasil.
  - Rust: `snake_case`, uma função por responsabilidade em `lib.rs`.
- **Padrões Estruturais:**
  - Backend: Clean Architecture/DDD por bounded context; Ports & Adapters; **Decorator** (`CachedId3Codec` envolve `Id3MagicCodec`, montado em `Id3CodecConfig`); **Template Method** (`AbstractCoverSearcher`, `AbstractLyricsSource`, `AbstractDictionarySource`); **Strategy/Composite** (`CompositeCoverSearcher`, `CompositeLyricsScraper`, `DictionaryLookupService` por idioma); **Repository** (arquivos). Injeção por construtor. Entidades de domínio imutáveis (`Settings`, `Playlist`, `Lyric`, `MusicFile`).
  - Frontend: Feature-Sliced Design (`features/<nome>/{ui,lib,__tests__}`), estado global em três Contexts (`AppContext`, `LibraryContext`, `PlayerContext`), refs espelhando estado para uso em loops assíncronos (`PlayerContext`), CSS global único (`App.css`) com tokens `--*` e tema via `body[data-theme='dark'|'light']`.
  - Estilo visual: monocromático estrito, MUI só para ícones, CSS puro (ver `.opencode/skills/frontend-style/SKILL.md`).
- **Tratamento de Erros e Logs:**
  - Backend: controllers capturam exceções e respondem `400 Bad Request` com corpo `"Error: <mensagem>"` (ou mensagens em português em letras/capas) ou `404` quando o recurso não existe; serviços de leitura ID3 em lote convertem falha em `{ "error": "Could not read ID3 tags" }`. Logs via SLF4J (`LoggerFactory`), sem configuração Logback própria (sem `logback-spring.xml`).
  - Frontend: o cliente HTTP **engole** erros de rede e devolve `null`/`false`/`{ ok:false }` (`shared/api/client.ts`); a UI exibe mensagens curtas e temporárias (timers de 4 s) em português. Não há error boundary.
  - Shell: falhas ao subir o backend são silenciosas (`eprintln!`); em dev (sem `resources/`) o backend **não** é iniciado e deve ser executado à parte.
- **Gerenciamento de Estado e Configuração:**
  - Backend: `application.properties` (porta `8111`, `mp3.lyrics.*`, `mp3.cover.*`) via `@ConfigurationProperties` (`LyricsProperties`, `CoverProperties`); propriedades injetadas pelo shell em runtime: `mp3.log-file`, `mp3.frontend-port`, `server.address`, `server.port`.
  - Frontend: estado em Contexts; preferências em `localStorage` (`mp3_folder`, `mp3_theme`, `lyrics_font_size`). Modo de reprodução e "mostrar capa" **não** são persistidos (voltam ao padrão a cada abertura).
  - Persistência em arquivos (sem banco de dados): ver `docs/sdd/TechSpec.md`, seção de persistência.
  - Versão do app: replicada em `package.json`, `tauri.conf.json`, `Cargo.toml` e no texto do rodapé em `App.tsx`; **atualizada apenas pelo script de release** (`pom.xml` permanece em `1.0.0`).

## 4. Testes e Qualidade
- **Estratégia de Testes:**
  - Backend: testes unitários JUnit 5 + Mockito em `backend/src/test/java/com/ovelha/fy/**` espelhando os pacotes (domain, application, infrastructure, web); um teste de contexto Spring (`Mp3PlayerApplicationTests`) valida a injeção de todos os beans.
  - Frontend: Vitest + Testing Library (jsdom, `globals: true`, setup `src/test/setup.ts`) em `features/*/__tests__/**/*.test.ts(x)`. Hoje cobrem `playback` (navegação, `usePlayback`, `usePlaybackPolling`, `Player`, `CoverArt`) e `playlist-view` (resize de colunas, tooltip, `ColumnHeader`, `Playlist`, `PlaylistTooltip`, `SearchBar`). Não há testes para `collection`, `lyrics`, `settings`, `load-folder`, `shared/lib/search.ts`, nem testes do shell Rust nem E2E.
  - Padrão exigido pelo skill `backend-dev`: AAA (Arrange/Act/Assert), caminho feliz + cenários de erro, mocks apenas para I/O externo.
- **Critérios Mínimos:**
  - Toda alteração no backend acompanha teste unitário da classe alterada/criada; toda lógica nova no frontend em `lib/` ou `shared/lib/` acompanha teste Vitest.
  - Javadoc deve existir/estar correto em classes públicas do backend alteradas.
  - Comportamentos descritos em `PRD.md`/`TechSpec.md` da atividade devem ter critério de aceite verificável.
- **Build e Verificação:**
  - Backend: `cd backend && mvn test` (e `mvn test -Dtest=Mp3PlayerApplicationTests` para o contexto Spring).
  - Frontend: `cd frontend && npm run test:run`; checagem de tipos pelo `tsc` (strict) — não há script `lint`/`typecheck` dedicado em `package.json` `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
  - Dev: backend `mvn spring-boot:run` (porta 8111) + `npm run dev` (Tauri) ou `npm run dev:vite` (porta 8112, `strictPort`).
  - Release: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 [-Minor|-Major|-Version X.Y.Z|-DryRun]`.

## 5. Governança de Spec-Driven Development (SDD)
- **Fonte da Verdade:** Nenhum código de funcionalidade nova ou alteração estrutural deve ser escrito antes de atualizar `docs/sdd/PRD.md` (requisitos e critérios de aceite) e `docs/sdd/TechSpec.md` (contratos, fluxos e impacto técnico).
- **Fluxo por Atividade (obrigatório):** toda nova funcionalidade ou correção de bug deve ser **especificada antes da implementação** em `docs/sdd/atividades/ofy-XXX-<slug>/`, onde `XXX` é a numeração sequencial de 3 dígitos (`ofy-001`, `ofy-002`, …) e `<slug>` é um nome curto em `kebab-case`. Cada pasta contém:
  - `PRD.md` — escopo, objetivo, regras de negócio e critérios de aceite (base: `docs/sdd/templates/prd-template.md`);
  - `TechSpec.md` — arquivos envolvidos, contratos, fluxo técnico e pontos de atenção (base: `docs/sdd/templates/techspec-template.md`);
  - `referencias.md` — **quando útil**: payloads, schemas, arquivos e dependências para rastreabilidade.
  - O próximo número é sempre o maior existente + 1. As atividades `ofy-001` a `ofy-016` são o levantamento retroativo do que já existe no código na v1.6.0.
- **Fidelidade aos Templates:** Toda criação ou atualização de documentação em `docs/sdd/` deve seguir rigorosamente a estrutura definida em `docs/sdd/templates/`.
- **Proteção dos Modelos:** Os arquivos em `docs/sdd/templates/` **nunca** devem ser alterados, movidos ou sobrescritos durante a geração ou evolução das specs.
- **Sincronia Código-Spec:** Caso uma decisão técnica mude durante a implementação, atualizar `TechSpec.md` da atividade (e o `docs/sdd/TechSpec.md` macro e/ou este `Rules.md`, se for contrato ou convenção permanente) antes de finalizar a tarefa.
- **Marcação de Incertezas:** código incompleto, stub ou comportamento ambíguo deve ser descrito com `[PENDENTE / AMBÍGUO NO CÓDIGO]`, nunca "corrigido" silenciosamente na documentação.
- **Estado atual da documentação:** `docs/architecture.md` e `README.md` contêm divergências conhecidas em relação ao código (ver "Questões em Aberto" em `docs/sdd/PRD.md`); em caso de conflito, o código-fonte prevalece e a spec deve registrar o fato.

## 6. Restrições Críticas e Zonas Sensíveis
- **Arquivos e Contratos Protegidos:**
  - Portas fixas: backend `8111`, frontend (Vite dev) `8112` — referenciadas em `application.properties`, `vite.config.js` (`strictPort`), `tauri.conf.json` (`devUrl`), `src-tauri/src/lib.rs` e `frontend/src/shared/api/client.ts`. Alterar uma exige alterar todas.
  - Contratos HTTP listados em `docs/sdd/TechSpec.md` (paths, métodos e formatos, incluindo `text/plain` em `/play`) — consumidos por `frontend/src/shared/api/*`.
  - Chaves de `localStorage` (`mp3_folder`, `mp3_theme`, `lyrics_font_size`) e formatos de persistência em disco (playlists `.txt`, letras `.txt`, cache JSON) — compatibilidade com dados existentes do usuário.
  - `frontend/src-tauri/tauri.conf.json` (`identifier: com.mp3player.app`, `bundle.resources`), `capabilities/default.json` e `Cargo.toml`: mudanças exigem revisão explícita.
  - Pontos de versão atualizados pelo script de release (`package.json`, `tauri.conf.json`, `Cargo.toml`, `App.tsx` via regex `v[0-9]+.[0-9]+.[0-9]+`): não renomear nem reformatar esses trechos sem ajustar o script.
  - `docs/sdd/templates/`: somente leitura.
- **Diretórios Ignorados / Gerados:** `node_modules/`, `frontend/dist/`, `**/target/` (incluindo `backend/target/` e `frontend/src-tauri/target/`), `frontend/src-tauri/resources/` (JRE + `backend.jar` gerados pelo release), `frontend/src-tauri/gen/`, `.idea/`, `.opencode/node_modules/`, `*.jar`. Não editar manualmente nem usar como referência de arquitetura. `frontend/src-tauri/icons_backup/` é cópia auxiliar de ícones.
- **Segurança e Permissões:**
  - O backend, quando iniciado pelo Tauri, escuta em `0.0.0.0:8111` (acessível pela rede local) com CORS `allowedOrigins("*")`, sem autenticação. Endpoints aceitam **caminhos absolutos arbitrários** do sistema de arquivos (`/play`, `/playlist?path=`, `/id3/update`, `/lyrics`, `/cover`, `/cover/download`) e não há validação/restrição de diretório `[PENDENTE / AMBÍGUO NO CÓDIGO]`. Qualquer mudança que amplie a exposição deve ser tratada como alteração de segurança e especificada antes.
  - CSP do Tauri está desabilitado (`security.csp: null`); capabilities concedem `core:default` e `opener:default`.
  - Nomes de playlist são sanitizados (`[\\/:*?"<>|]` → `_`) e nomes de arquivo de letras idem; manter essa sanitização em novos pontos de escrita em disco.
  - Scraping usa `User-Agent` de navegador configurável; respeitar timeouts configurados e não adicionar fontes sem spec (termos de uso/robots dos sites externos são fatores de risco).
  - Não commitar credenciais; o projeto não usa chaves de API (iTunes/Deezer/Priberam são consultados sem autenticação).
