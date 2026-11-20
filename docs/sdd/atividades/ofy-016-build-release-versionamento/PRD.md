# PRD — ofy-016 — Build de release e versionamento

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Script PowerShell `scripts/build-release.ps1` que automatiza versionamento semântico, changelog, build do backend, JRE mínimo, frontend e instaladores Windows.

## Objetivos

- Gerar um release reproduzível com um único comando.

## Histórias de Usuário

- Como mantenedor, quero gerar o instalador e a nova versão automaticamente a partir dos commits.

## Funcionalidades Principais

1. **RF-016.1 Parâmetros:** `-Version`, `-Minor`, `-Major`, `-DryRun`.
2. **RF-016.2 Classificação de commits** desde a última tag em added (`feat|add|criar|novo|nova|incluir|implementar|suporte|adicionar`), fixed (`fix|bug|corrigir|corrigido|resolver|ajuste`), refactored (`refator…`) e removed.
3. **RF-016.3 Versão:** `-Version` > `-Major` > (`-Minor` ou added>0 ⇒ minor) > patch.
4. **RF-016.4 Arquivos atualizados:** `package.json`, `tauri.conf.json`, `Cargo.toml`, `App.tsx` e `CHANGELOG.md` (entrada inserida antes do primeiro `## [`).
5. **RF-016.5 Build:** `mvn -q package -DskipTests` → `jlink` (25 módulos; `--strip-debug --no-header-files --no-man-pages --compress=2`) → `resources/jre` e `resources/backend.jar` → `npm run build` (tauri build).
6. **RF-016.6 Git:** `git add -A`, commit `release: vX.Y.Z` e tag.
7. **Saídas:** `frontend/src-tauri/target/release/bundle/{msi,nsis}`.
8. **Critérios de aceite:** `-DryRun` não altera nada; versão resultante consistente nos 4 arquivos.

## Experiência do Usuário

- Saída de console por etapa.

## Restrições Técnicas de Alto Nível

- Requer Windows, PowerShell, JDK 21 (`jlink`), Maven, Node e Rust. `resources/`, `*.jar`, `target`, `dist` e `node_modules` são ignorados pelo git.

## Fora de Escopo

- CI, assinatura de código, publicação automática.

## Questões em Aberto

- `git add -A` inclui tudo no commit de release.
- O skill `.opencode/skills/build-release/SKILL.md` cita v0.9.0 como última tag (desatualizado; atual v1.6.0) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
