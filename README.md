# ovelhafy

## Objetivo

Gerenciador e organizador de coleção de músicas local com tocador de mp3. Organize e altere as ID3 de suas músicas, baixe as letras, as capas de álbuns, crie playlists etc.

---

## Funcionalidades

| Funcionalidade | Descricao |
|---|---|
| Reproducao MP3 | Play, pausa, stop, resume e seek em arquivos MP3 locais |
| Navegacao entre faixas | Anterior / proxima com tres modos: Continua, Aleatoria e Repeticao |
| Auto-play | Reproduz automaticamente a proxima faixa ao termino da atual |
| Tags ID3 | Leitura e edicao de artista, titulo, album, ano, genero, faixa, disco, bitrate e duracao |
| Edicao em lote (bulk) | Edicao de tags ID3 de multiplos arquivos a partir de padroes de nome |
| Capa do album | Exibicao automatica de arquivos de capa (jpg/png/webp/gif) |
| Download de capa | Busca automatica via APIs do iTunes (fallback: Deezer) |
| Letras (lyrics) | Busca via web scraping em letras.mus.br com cache local e edicao |
| Playlists fisicas | Escaneamento de pastas para arquivos MP3 |
| Playlists virtuais | Criacao, edicao (duas colunas), renomeacao, exclusao e carregamento |
| Gerenciador de colecao | Lista de albuns e artistas com edicao de tags ID3 em grade |
| Busca avancada | Filtros com operadores logicos (`&&`, `||`) e filtros por tag |
| Temas | Suporte a tema escuro e claro com CSS custom properties |
| Cache de metadados | Cache em disco (`~/.mp3-player/metadata-cache.json`) usando Decorator Pattern |

---

## Tecnologias

| Camada | Tecnologia | Versao |
|---|---|---|
| Runtime backend | Java | 21 |
| Framework backend | Spring Boot | 3.3.5 |
| Build backend | Maven | 3.8+ |
| Decodificador MP3 | JLayer | 1.0.1 |
| Tags ID3 | mp3agic | 0.9.1 |
| Web scraping | Jsoup | 1.18.1 |
| Framework UI | React | 18 |
| Bundler frontend | Vite | 6 |
| Linguagem frontend | TypeScript | strict |
| Desktop | Tauri | v2 |
| Icones | MUI (Material UI) | 9.2 |
| Testes backend | JUnit 5 + Mockito | — |

---

## Como rodar (desenvolvimento)

### Pre-requisitos

- **Java 21**
- **Maven 3.8+**
- **Node.js 18+** e **npm**
- **Rust** (para compilar o Tauri)

### Backend

```bash
cd backend
mvn spring-boot:run
```

A API roda em `http://localhost:8111`.

### Frontend (apenas Vite)

```bash
cd frontend
npm install
npm run dev:vite
```

O Vite roda em `http://localhost:8112`.

### Frontend (Tauri desktop)

```bash
cd frontend
npm install
npm run dev
```

Inicia o Vite e abre a janela desktop do Tauri apontando para `http://localhost:8112`.

---

## Como testar

### Backend

```bash
cd backend
mvn test
```

### Testes de integracao

```bash
cd backend
mvn test -Dtest=Mp3PlayerApplicationTests
```

Valida que o contexto Spring sobe e injeta todos os beans.

---

## Como empacotar

### Build rapido (apenas frontend)

```bash
cd frontend
npm run build
```

Gera o binario Tauri + instaladores MSI/NSIS em `frontend/src-tauri/target/release/bundle/`.

### Release completa (automatizada)

O script `scripts/build-release.ps1` faz tudo automaticamente: versionamento semantico, changelog, compilacao do JAR, geracao do JRE minimo via `jlink`, e empacotamento Tauri:

```powershell
# Do diretorio raiz do projeto:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1

# Opcoes:
#   -Minor              # Bump de versao minor
#   -Major              # Bump de versao major
#   -Version "X.Y.Z"   # Versao explicita
#   -DryRun             # Simula sem alterar nada
```

O que o script faz:

1. Le a ultima tag git
2. Coleta e classifica commits desde a ultima tag
3. Calcula a nova versao semantica
4. Atualiza versao em `package.json`, `tauri.conf.json`, `Cargo.toml` e `App.tsx`
5. Gera e insere entrada no `CHANGELOG.md`
6. Empacota: `mvn package -DskipTests` -> `jlink` (JRE minimo) -> copia JAR para `resources/` -> `npm run build` (Tauri)
7. Cria commit e tag git

O app gerado e auto-contido: embute o backend Java e um JRE minimo (`jlink`), sem exigir Java instalado na maquina de destino.

---

## Estrutura do projeto

```
mp3-player/
├── backend/        # API REST em Java 21 + Spring Boot (Clean Architecture / DDD)
├── frontend/       # App desktop com Tauri v2 + React 18 + Vite + TypeScript
├── scripts/        # Scripts de build e release
├── docs/           # Diagramas de interacao das camadas (Mermaid)
└── CHANGELOG.md    # Historico de versoes
```