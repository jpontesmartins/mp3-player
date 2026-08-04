# build-release.ps1
# Gerencia empacotamento, changelog e versionamento semântico do MP3 Player.
#
# Uso:
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -Minor
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -Version "0.10.0"
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -Major
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -DryRun
param(
    [string]$Version,
    [switch]$Minor,
    [switch]$Major,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

# ── 1. Determinar último tag ──────────────────────────────────────────────────
$lastTag = (git tag --sort=-version:refname 2>$null | Select-Object -First 1)
if ($lastTag) {
    $lastVersion = $lastTag -replace '^v', ''
    Write-Host "`nUltimo tag: $lastTag" -ForegroundColor Cyan
} else {
    $lastVersion = '0.0.0'
    Write-Host "`nNenhum tag encontrado. Listando todos os commits." -ForegroundColor Cyan
}

# ── 2. Coletar commits desde o último tag ─────────────────────────────────────
if ($lastTag) {
    $commits = git log "$lastTag..HEAD" --oneline 2>$null
} else {
    $commits = git log --oneline 2>$null
}
if (-not $commits) {
    Write-Host "Nenhum commit desde $lastTag. Nada a fazer." -ForegroundColor Yellow
    exit 0
}

Write-Host "Commits desde $lastTag ($($commits.Count)):" -ForegroundColor Gray
foreach ($c in $commits) { Write-Host "  $c" -ForegroundColor DarkGray }

# ── 3. Classificar commits ────────────────────────────────────────────────────
$added    = @()
$changed  = @()
$fixed    = @()
$refactored = @()
$removed  = @()

foreach ($line in $commits) {
    $msg = ($line -split ' ', 2)[1]
    $lower = $msg.ToLower()

    if ($lower -match '(feat|add|criar|novo|nova|incluir|implementar|suporte|adicionar)') {
        $added += $msg
    } elseif ($lower -match '(fix|bug|corrigir|corrigido|resolver|ajuste)') {
        $fixed += $msg
    } elseif ($lower -match '(refator|refatorar|reescrever|limpar|extrair)') {
        $refactored += $msg
    } elseif ($lower -match '(remove|deletar|excluir|drop|remover)') {
        $removed += $msg
    } else {
        $changed += $msg
    }
}

Write-Host "`nClassificacao:" -ForegroundColor Cyan
if ($added.Count -gt 0)       { Write-Host "  Adicionado: $($added.Count)" -ForegroundColor Green }
if ($changed.Count -gt 0)     { Write-Host "  Alterado:   $($changed.Count)" -ForegroundColor Yellow }
if ($fixed.Count -gt 0)       { Write-Host "  Corrigido:  $($fixed.Count)" -ForegroundColor Red }
if ($refactored.Count -gt 0)  { Write-Host "  Refatorado: $($refactored.Count)" -ForegroundColor Magenta }
if ($removed.Count -gt 0)     { Write-Host "  Removido:   $($removed.Count)" -ForegroundColor DarkRed }

# ── 4. Calcular versão ───────────────────────────────────────────────────────
$parts = $lastVersion -split '\.'
$majorNum = [int]$parts[0]
$minorNum = [int]$parts[1]
$patchNum = [int]$parts[2]

if ($Version) {
    $newVersion = $Version
    Write-Host "`nVersao especificada: $newVersion" -ForegroundColor Cyan
} elseif ($Major) {
    $majorNum++
    $minorNum = 0
    $patchNum = 0
    $newVersion = "$majorNum.$minorNum.$patchNum"
    Write-Host "`nBump MAJOR: $newVersion" -ForegroundColor Red
} elseif ($Minor -or $added.Count -gt 0) {
    $minorNum++
    $patchNum = 0
    $newVersion = "$majorNum.$minorNum.$patchNum"
    Write-Host "`nBump MINOR: $newVersion" -ForegroundColor Yellow
} else {
    $patchNum++
    $newVersion = "$majorNum.$minorNum.$patchNum"
    Write-Host "`nBump PATCH: $newVersion" -ForegroundColor Green
}

$date = Get-Date -Format 'yyyy-MM-dd'

# ── 5. Gerar entrada do changelog ─────────────────────────────────────────────
$changelogEntry = "`n## [$newVersion] — $date`n"
if ($added.Count -gt 0) {
    $changelogEntry += "`n### Adicionado`n"
    foreach ($item in $added) { $changelogEntry += "- $item`n" }
}
if ($changed.Count -gt 0) {
    $changelogEntry += "`n### Alterado`n"
    foreach ($item in $changed) { $changelogEntry += "- $item`n" }
}
if ($fixed.Count -gt 0) {
    $changelogEntry += "`n### Corrigido`n"
    foreach ($item in $fixed) { $changelogEntry += "- $item`n" }
}
if ($refactored.Count -gt 0) {
    $changelogEntry += "`n### Refatorado`n"
    foreach ($item in $refactored) { $changelogEntry += "- $item`n" }
}
if ($removed.Count -gt 0) {
    $changelogEntry += "`n### Removido`n"
    foreach ($item in $removed) { $changelogEntry += "- $item`n" }
}

Write-Host "`nChangelog gerado:" -ForegroundColor Cyan
Write-Host $changelogEntry -ForegroundColor Gray

if ($DryRun) {
    Write-Host "`n[DRY RUN] Nenhuma alteracao realizada." -ForegroundColor Yellow
    exit 0
}

# ── 6. Atualizar arquivos de versão ──────────────────────────────────────────
$packageJson = Join-Path $root 'frontend\package.json'
$tauriConf   = Join-Path $root 'frontend\src-tauri\tauri.conf.json'
$cargoToml   = Join-Path $root 'frontend\src-tauri\Cargo.toml'
$appTsx      = Join-Path $root 'frontend\src\App.tsx'

# package.json
(Get-Content $packageJson -Raw) -replace '"version"\s*:\s*"[^"]*"', "`"version`": `"$newVersion`"" |
    Set-Content $packageJson -NoNewline
Write-Host "Atualizado: package.json -> $newVersion" -ForegroundColor Green

# tauri.conf.json
(Get-Content $tauriConf -Raw) -replace '"version"\s*:\s*"[^"]*"', "`"version`": `"$newVersion`"" |
    Set-Content $tauriConf -NoNewline
Write-Host "Atualizado: tauri.conf.json -> $newVersion" -ForegroundColor Green

# Cargo.toml
(Get-Content $cargoToml -Raw) -replace 'version\s*=\s*"[^"]*"', "version = `"$newVersion`"" |
    Set-Content $cargoToml -NoNewline
Write-Host "Atualizado: Cargo.toml -> $newVersion" -ForegroundColor Green

# App.tsx (statusbar)
(Get-Content $appTsx -Raw) -replace 'v[0-9]+\.[0-9]+\.[0-9]+', "v$newVersion" |
    Set-Content $appTsx -NoNewline
Write-Host "Atualizado: App.tsx statusbar -> v$newVersion" -ForegroundColor Green

# ── 7. Atualizar CHANGELOG.md ────────────────────────────────────────────────
$changelogPath = Join-Path $root 'CHANGELOG.md'
$changelog = Get-Content $changelogPath -Raw

# Remover seção [Não publicado] se existir
$changelog = $changelog -replace '(?s)\n## \[Não publicado\].*?(?=\n## \[)', ''

# Inserir nova seção antes da primeira versão existente (## [X.Y.Z])
if ($changelog -match '(?m)^## \[') {
    $changelog = $changelog -replace '(?m)(## \[)', "$changelogEntry`n`$1"
} else {
    # Sem versão anterior, adicionar ao final
    $changelog = $changelog.TrimEnd() + "`n" + $changelogEntry
}

Set-Content $changelogPath $changelog -NoNewline
Write-Host "Atualizado: CHANGELOG.md" -ForegroundColor Green

# ── 8. Empacotar ─────────────────────────────────────────────────────────────
Write-Host "`n=== Empacotando v$newVersion ===" -ForegroundColor Cyan

$resources = Join-Path $root 'frontend\src-tauri\resources'

# 1. JAR fat do backend (Spring Boot)
Write-Host "`n[1/4] Compilando backend (JAR)..." -ForegroundColor Yellow
Push-Location (Join-Path $root 'backend')
mvn -q package -DskipTests
Pop-Location
$jar = Get-ChildItem (Join-Path $root 'backend\target\*.jar') | Select-Object -First 1
if (-not $jar) { throw 'Backend JAR nao encontrado apos mvn package' }
Write-Host "Backend JAR: $($jar.Name)" -ForegroundColor Green

# 2. JRE minimo via jlink
Write-Host "`n[2/4] Gerando JRE minimo via jlink..." -ForegroundColor Yellow
$mods = 'java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.xml,jdk.charsets,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.management,jdk.net,jdk.security.auth,jdk.unsupported'
if (Test-Path $resources) { Remove-Item -Recurse -Force $resources }
New-Item -ItemType Directory -Path $resources | Out-Null
jlink --add-modules $mods --strip-debug --no-header-files --no-man-pages --compress=2 --output (Join-Path $resources 'jre')
Write-Host "JRE gerado em $resources\jre" -ForegroundColor Green

# 3. Copiar JAR para resources
Copy-Item $jar.FullName (Join-Path $resources 'backend.jar')
Write-Host "JAR copiado para $resources\backend.jar" -ForegroundColor Green

# 4. Instaladores do Tauri (NSIS + MSI)
Write-Host "`n[3/4] Compilando frontend + Tauri..." -ForegroundColor Yellow
Push-Location (Join-Path $root 'frontend')
npm run build
Pop-Location
Write-Host "Frontend compilado." -ForegroundColor Green

Write-Host "`n[4/4] Instaladores gerados!" -ForegroundColor Green

# ── 9. Commit e tag ──────────────────────────────────────────────────────────
Write-Host "`nCriando commit e tag..." -ForegroundColor Yellow
git add -A
git commit -m "release: v$newVersion"
git tag "v$newVersion"

Write-Host "`n=== Build v$newVersion concluido! ===" -ForegroundColor Green
Write-Host "Tag criada: v$newVersion" -ForegroundColor Cyan
Write-Host "Instaladores em:" -ForegroundColor Cyan
Write-Host "  frontend\src-tauri\target\release\bundle\msi\" -ForegroundColor Gray
Write-Host "  frontend\src-tauri\target\release\bundle\nsis\" -ForegroundColor Gray
