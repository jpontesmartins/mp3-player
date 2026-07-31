# Empaa coloca o MP3 Player em um instalador Windows auto-contido
# (frontend Tauri + JAR do backend + JRE minimo via jlink).
# Requisitos: Java 21 (com jmods), Maven, Node.js, Rust/Cargo e Tauri CLI.
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$resources = Join-Path $root 'frontend\src-tauri\resources'

# 1. JAR fat do backend (Spring Boot)
Push-Location (Join-Path $root 'backend')
mvn -q package
Pop-Location
$jar = Get-ChildItem (Join-Path $root 'backend\target\*.jar') | Select-Object -First 1
if (-not $jar) { throw 'Backend JAR nao encontrado apos mvn package' }

# 2. JRE minimo via jlink
$mods = 'java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.xml,jdk.charsets,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.management,jdk.net,jdk.security.auth,jdk.unsupported'
if (Test-Path $resources) { Remove-Item -Recurse -Force $resources }
New-Item -ItemType Directory -Path $resources | Out-Null
jlink --add-modules $mods --strip-debug --no-header-files --no-man-pages --compress=2 --output (Join-Path $resources 'jre')
Copy-Item $jar.FullName (Join-Path $resources 'backend.jar')

# 3. Instaladores do Tauri (NSIS + MSI)
Push-Location (Join-Path $root 'frontend')
npm run build
Pop-Location

Write-Host ''
Write-Host 'Instaladores gerados em:'
Write-Host '  frontend\src-tauri\target\release\bundle\msi\'
Write-Host '  frontend\src-tauri\target\release\bundle\nsis\'
