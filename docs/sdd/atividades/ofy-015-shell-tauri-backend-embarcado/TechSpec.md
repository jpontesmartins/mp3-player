# Especificação Técnica — ofy-015 — Shell Tauri e backend embarcado

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

`lib.rs` gerencia `BackendProcess(Mutex<Option<Child>>)` no estado do Tauri; `spawn_backend` resolve caminhos a partir de `resource_dir` (removendo o prefixo `\\?\`) e `kill_backend` é chamado no evento de saída. Não há comandos IPC além do plugin opener.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `frontend/src-tauri/src/{lib.rs,main.rs}`, `build.rs`, `tauri.conf.json`, `capabilities/default.json`, `Cargo.toml`.
- Config: `identifier com.mp3player.app`, `frontendDist ../dist`, `devUrl http://localhost:8112`, `beforeDevCommand npm run dev:vite`, `beforeBuildCommand npm run build:vite`, bundle `all`, recursos `resources/backend.jar` e `resources/jre/**/*`.

## Design de Implementação

### Interfaces Principais

- Sem comandos `invoke`; apenas plugin `tauri-plugin-opener`.

### Modelos de Dados

- `BackendProcess(Mutex<Option<Child>>)`.

### Endpoints de API

- Não se aplica (o backend expõe HTTP em `0.0.0.0:8111`; ver `TechSpec.md` geral).

## Pontos de Integração

- Processo filho Java; arquivo de log em `app_log_dir`.

## Abordagem de Testes

### Testes Unitários

- Não há testes Rust `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

### Testes de Integração

- Verificação manual do instalador (MSI/NSIS).

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Shell com backend embarcado e JRE mínimo (jlink) → ver `ofy-016`.

### Dependências Técnicas

- `ofy-016` (gera `backend.jar` e `jre`).

## Monitoramento e Observabilidade

- `backend.log` em `app_log_dir` (saída do Java).

## Considerações Técnicas

### Decisões Principais

- Processo filho em vez de sidecar/IPC: mantém o backend Spring independente do shell.

### Riscos Conhecidos

- Processo órfão em encerramento forçado; CORS `*` e bind em todas as interfaces expõem a API na rede local.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/frontend-style/SKILL.md` (visual monocromático, Feature-Sliced Design, TS strict).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)

### Arquivos relevantes

- `frontend/src-tauri/**` (exceto `target/`)
