# ovelhafy

## Objetivo

Gerenciador e organizador de coleção de músicas local com tocador de mp3. Organize e altere as ID3 de suas músicas, baixe as letras, as capas de álbuns, crie playlists etc.

---

## Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Reprodução MP3 | Play, pausa, stop, resume e seek em arquivos MP3 locais |
| Navegação entre faixas | Anterior / próxima com três modos: Contínua, Aleatória e Repetição |
| Auto-play | Reproduz automaticamente a próxima faixa ao término da atual |
| Tags ID3 | Leitura e edição de artista, título, álbum, ano, gênero, faixa, disco, bitrate e duração |
| Edição em lote (bulk) | Edição de tags ID3 de múltiplos arquivos a partir de padrões de nome |
| Capa do álbum | Exibição automática de arquivos de capa (jpg/png/webp/gif) |
| Download de capa | Busca automática via APIs do iTunes (fallback: Deezer) |
| Letras (lyrics) | Busca via web scraping em letras.mus.br com cache local e edição |
| Playlists físicas | Escaneamento de pastas para arquivos MP3 |
| Playlists virtuais | Criação, edição (duas colunas), renomeação, exclusão e carregamento |
| Gerenciador de coleção | Lista de álbuns e artistas com edição de tags ID3 em grade |
| Busca avançada | Filtros com operadores lógicos (`&&`, `||`) e filtros por tag |
| Temas | Suporte a tema escuro e claro com CSS custom properties |
| Cache de metadados | Cache em disco (`~/.mp3-player/metadata-cache.json`) usando Decorator Pattern |

---

## Tecnologias

| Camada | Tecnologia | Versão |
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
| Ícones | MUI (Material UI) | 9.2 |
| Testes backend | JUnit 5 + Mockito | — |

---

## Como rodar (desenvolvimento)

### Pré-requisitos

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

### Testes de integração

```bash
cd backend
mvn test -Dtest=Mp3PlayerApplicationTests
```

Valida que o contexto Spring sobe e injeta todos os beans.

---

## Como empacotar

### Build rápido (apenas frontend)

```bash
cd frontend
npm run build
```

Gera o binário Tauri + instaladores MSI/NSIS em `frontend/src-tauri/target/release/bundle/`.

### Release completa (automatizada)

O script `scripts/build-release.ps1` faz tudo automaticamente: versionamento semântico, changelog, compilação do JAR, geração do JRE mínimo via `jlink`, e empacotamento Tauri:

```powershell
# Do diretório raiz do projeto:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1

# Opções:
#   -Minor              # Bump de versão minor
#   -Major              # Bump de versão major
#   -Version "X.Y.Z"   # Versão explícita
#   -DryRun             # Simula sem alterar nada
```

O que o script faz:

1. Lê a última tag git
2. Coleta e classifica commits desde a última tag
3. Calcula a nova versão semântica
4. Atualiza versão em `package.json`, `tauri.conf.json`, `Cargo.toml` e `App.tsx`
5. Gera e insere entrada no `CHANGELOG.md`
6. Empacota: `mvn package -DskipTests` -> `jlink` (JRE mínimo) -> copia JAR para `resources/` -> `npm run build` (Tauri)
7. Cria commit e tag git

O app gerado é auto-contido: embute o backend Java e um JRE mínimo (`jlink`), sem exigir Java instalado na máquina de destino.

---

## Estrutura do projeto

```
mp3-player/
├── backend/        # API REST em Java 21 + Spring Boot (Clean Architecture / DDD)
├── frontend/       # App desktop com Tauri v2 + React 18 + Vite + TypeScript
├── scripts/        # Scripts de build e release
├── docs/           # Diagramas de interação das camadas (Mermaid)
└── CHANGELOG.md    # Histórico de versões
```