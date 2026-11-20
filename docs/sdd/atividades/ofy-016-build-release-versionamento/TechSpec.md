# Especificação Técnica — ofy-016 — Build de release e versionamento

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Fluxo sequencial em PowerShell: tag → commits → classificação → bump → atualização de arquivos → build do backend → jlink → copia → build do frontend/Tauri → commit e tag.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `scripts/build-release.ps1`, `CHANGELOG.md`, `.gitignore`, `.opencode/skills/build-release/SKILL.md`.

## Design de Implementação

### Interfaces Principais

- CLI: `./scripts/build-release.ps1 [-Version x.y.z] [-Minor] [-Major] [-DryRun]`.

### Modelos de Dados

- Seções do changelog (Adicionado/Corrigido/Refatorado/Removido).

### Endpoints de API

- Não se aplica.

## Pontos de Integração

- Git, Maven, `jlink`, npm/Tauri CLI.

## Abordagem de Testes

### Testes Unitários

- Não há.

### Testes de Integração

- `-DryRun` como verificação.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Versão 1.6.0 é a última release (`release: v1.6.0`).

### Dependências Técnicas

- `ofy-015`.

## Monitoramento e Observabilidade

- Saída do console.

## Considerações Técnicas

### Decisões Principais

- JRE reduzido via `jlink` para reduzir o tamanho do instalador.

### Riscos Conhecidos

- Lista fixa de 25 módulos pode faltar algum ao adicionar dependências; falha no meio deixa arquivos de versão alterados.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)
- `.opencode/skills/frontend-style/SKILL.md` (frontend).

### Arquivos relevantes

- `scripts/build-release.ps1`, `CHANGELOG.md`, `.gitignore`, `.opencode/skills/build-release/SKILL.md`
