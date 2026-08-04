---
name: build-release
description: Gerenciar empacotamento, changelog e versionamento semântico do MP3 Player. Use quando o usuário quiser gerar um build, criar release, empacotar o projeto, ou bump de versão.
---

# Skill: Build, Release & Changelog (MP3 Player)

Fluxo automatizado de empacotamento que gera changelog a partir do git log,
aplica versionamento semântico (SemVer) e atualiza todos os arquivos de versão
do projeto.

## Quando usar

O usuário pede para:
- "Gerar build" / "Empacotar" / "Criar release"
- "Bump de versão" / "Atualizar versão"
- "Gerar changelog"
- "Soltar versão"

## 1. Fluxo completo (passo a passo)

### Passo 1 — Coletar commits desde o último tag

```powershell
git tag --sort=-version:refname          # listar tags (última = v0.9.0)
git log v0.9.0..HEAD --oneline           # commits desde a última tag
```

Se não houver tag, usar todos os commits.

### Passo 2 — Classificar commits

Ler cada linha do `git log` e classificar conforme o prefixo/keyword:

| Categoria      | Palavras-chave no commit                                    |
|----------------|-------------------------------------------------------------|
| **Adicionado** | add, feat, criar, novo, nova, incluir, implementar, suporte |
| **Alterado**   | alterar, mudar, ajuste, update, modify, port, traduzir      |
| **Corrigido**  | fix, bug, corrigir, corrigido, resolver                      |
| **Refatorado** | refator, refatorar, reescrever, limpar, extrair              |
| **Removido**   | remove, deletar, excluir, drop, remover                      |

### Passo 3 — Determinar bump da versão (SemVer)

Regras:
- **patch** (x.x.1): apenas fixes OU commits misturados sem feat novo
- **minor** (x.1.0): ao menos 1 feat/adicionado
- **major** (1.0.0): **APENAS** quando o usuário especificar `--major` ou "major"
  explicitamente. Nunca inferir major do commit.

Exemplos:
- "fix no scrapper" → patch
- "feat: download de capa" → minor
- "usuário pediu major" → major

### Passo 4 — Atualizar arquivos de versão

A versão deve ser atualizada em **4 arquivos** (sem `v` prefixo):

| Arquivo | Campo / Linha | Exemplo |
|---------|---------------|---------|
| `frontend/package.json` | `"version": "X.Y.Z"` | linha 3 |
| `frontend/src-tauri/tauri.conf.json` | `"version": "X.Y.Z"` | linha 4 |
| `frontend/src-tauri/Cargo.toml` | `version = "X.Y.Z"` | linha 3 |
| `frontend/src/App.tsx` | `<footer id="statusbar">vX.Y.Z</footer>` | usar `v` prefixo |

**Não alterar** `package-lock.json` nem `Cargo.lock` — são auto-gerados.

### Passo 5 — Gerar CHANGELOG.md

Inserir nova seção **no topo** do arquivo (após o cabeçalho), antes da seção
anterior, seguindo o formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/):

```markdown
## [X.Y.Z] — YYYY-MM-DD

### Adicionado
- Item 1
- Item 2

### Alterado
- Item 1

### Corrigido
- Item 1

### Refatorado
- Item 1

### Removido
- Item 1
```

Remover a seção `[Não publicado]` se existir, incorporando seus itens à nova
versão (ou mantê-la vazia se não houver commits desde a tag).

### Passo 6 — Commit da release

```powershell
git add -A
git commit -m "release: vX.Y.Z"
git tag vX.Y.Z
```

### Passo 7 — Empacotar

Executar o script de empacotamento:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -Version X.Y.Z
```

Ou, se o script não existir ainda, usar o script existente:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\package-windows.ps1
```

## 2. Script de automação: `scripts/build-release.ps1`

O script deve:
1. Aceitar parâmetro `-Version` (obrigatório) e `-Major` (flag)
2. Ler o último tag do git
3. Coletar e classificar commits
4. Calcular próximo número de versão (ou usar o fornecido via `-Version`)
5. Atualizar os 4 arquivos de versão
6. Gerar entrada no CHANGELOG.md
7. Compilar o backend (`mvn package`)
8. Gerar JRE via `jlink`
9. Compilar o frontend/Tauri (`npm run build`)
10. Criar tag git

## 3. Instruções para o agente ao usar esta skill

Quando o usuário pedir um build/release:

1. **Perguntar** se é major (só se o usuário não tiver especificado)
2. **Ler** `git log --oneline` desde a última tag
3. **Classificar** os commits
4. **Calcular** a versão
5. **Mostrar** ao usuário o resumo (versão, changelog proposto) e pedir confirmação
6. **Aplicar** as alterações nos 4 arquivos + CHANGELOG.md
7. **Executar** `mvn package` no backend
8. **Executar** `npm run build` no frontend
9. **Criar** commit e tag
10. **Informar** onde estão os instaladores gerados

### Exemplo de interação

```
Agente: Última versão: v0.9.0. Commits desde então:
  - 8a85968 readmes e changelog
  - 3ff34bf baixar capa do album
  - 8b413a0 ajuste scrapper
  - 49e85e8 colunas da playlist, edição da letra
  - ee13a24 refatoração, criação de skills...

Classificação:
  Adicionado: baixar capa do album, colunas da playlist, edição da letra
  Alterado: ajuste scrapper, readmes e changelog
  Refatorado: refatoração, criação de skills

Versão calculada: v0.10.0 (minor — há features novas)
Changelog proposto:
  ## [0.10.0] — 2026-08-04
  ### Adicionado
  - Baixar capa do álbum
  - Colunas da playlist redimensionáveis
  - Edição de letras no painel

  ### Alterado
  - Ajuste no scraper de letras
  - Readmes e changelog atualizados

  ### Refatorado
  - Criação de skills e refatoração geral

Confirmar geração do build v0.10.0? (S/n)
```

## 4. Referência

- Código de referência: `scripts/package-windows.ps1` (empacotamento atual)
- Changelog: `CHANGELOG.md`
- Versões: `package.json`, `tauri.conf.json`, `Cargo.toml`, `App.tsx` (statusbar)
