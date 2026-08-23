# Frontend — ovelhafy

Aplicação desktop construída com **Tauri v2 + React 18 + Vite + TypeScript (strict)**.

---

## Arquitetura

O frontend é organizado por **módulos de negócio**, onde cada módulo representa um contexto da aplicação e contém seus próprios componentes e tipos.

### Estrutura

```
src/
├── main.tsx                    # Entrada da aplicação
├── App.tsx                     # Componente raiz (estado global, polling, auto-play)
├── App.css                     # Estilos globais + CSS custom properties (temas)
├── config.ts                   # URL da API backend
├── searchParser.ts             # Parser de busca avançada
├── vite-env.d.ts               # Tipos Vite
│
├── player/                     # Módulo: Reprodução
│   ├── components/
│   │   ├── Player.tsx          # Capa, controles de mídia, barra de progresso
│   │   ├── Playlist.tsx        # Lista de músicas com tooltip ID3
│   │   └── FolderSelector.tsx  # Input de caminho de pasta
│   └── types.ts
│
├── lyrics/                     # Modulo: Letras
│   ├── components/
│   │   └── LyricsPanel.tsx     # Exibição/edição de letras, controles A+/A-
│   └── types.ts
│
├── metadata/                   # Modulo: Metadados
│   ├── components/
│   │   ├── CollectionManager.tsx   # Lista de álbuns/artistas com edição em grade
│   │   ├── BulkId3Editor.tsx       # Edição em lote de tags
│   │   └── InfoModal.tsx           # Dialog "Sobre"
│   └── types.ts
│
└── settings/                   # Módulo: Configurações
    ├── components/
    │   ├── SettingsPanel.tsx    # Modo de reprodução, tema, capa, seletor de pasta
    │   ├── PlaylistManager.tsx  # CRUD de playlists virtuais (duas colunas)
    │   └── Toolbar.tsx         # Navegação: Letra, Coleção, Config, Info
    └── types.ts
```

### Módulos

| Módulo | Responsabilidade | Componentes |
|---|---|---|
| **player** | Reprodução de áudio, playlist visual, seleção de pasta | `Player`, `Playlist`, `FolderSelector` |
| **lyrics** | Exibição e edição de letras | `LyricsPanel` |
| **metadata** | Gerenciamento de metadados, coleção, edição em lote | `CollectionManager`, `BulkId3Editor`, `InfoModal` |
| **settings** | Configurações, playlists virtuais, navegação | `SettingsPanel`, `PlaylistManager`, `Toolbar` |

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Framework UI | React 18 |
| Bundler | Vite 6 |
| Linguagem | TypeScript (strict, noUnusedLocals, noUnusedParameters) |
| Desktop | Tauri v2 (Rust) |
| Ícones | MUI (Material UI) 9.2 |
| Estilização | CSS puro (CSS custom properties) |

---

## Paleta de cores

### Tema escuro (padrão)

| Elemento | Cor | Uso |
|---|---|---|
| Fundo | `#000` | Body |
| Superfícies | `#0d0d0d` | Painéis (letra, playlist, configurações) |
| Botões padrão | `#1a1a1a` / borda `#333` | Ações genéricas |
| Botões de mídia | `#2a2a2a` / 32x32 | Play/pause/stop/anterior/próxima |
| Texto primário | `#eee` | Títulos, labels |
| Texto secundário | `#888` / `#777` | Status, dicas |
| Link/ativo | `#ccc` / `#eee` | Item selecionado |
| Desabilitado | Opacity `0.4` | Botão inativo |

### Tema claro

| Elemento | Cor |
|---|---|
| Fundo | `#f4f4f4` |
| Superfícies | `#e7e7e7` |
| Botões | `#cccccc` / borda `#a0a0a0` |
| Texto primário | `#111` |
| Texto secundário | `#555` / `#666` |

Os temas são implementados via CSS custom properties (`--c-*`) alternados pelo atributo `data-theme` no `<body>`.

---

## Persistência no localStorage

| Chave | Tipo | Descrição |
|---|---|---|
| `mp3_folder` | `string` | Caminho da última pasta carregada |
| `mp3_theme` | `'dark' \| 'light'` | Tema selecionado |

---

## Interações

- **Auto-play**: ao fim da música, toca a próxima conforme o modo (Contínua -> próxima, Aleatória -> aleatória, Repetição -> mesma)
- **Intentional stop**: impede auto-play em Stop manual ou carregamento de nova playlist
- **Seek**: clique na barra de progresso para ir a qualquer ponto
- **Capa**: se a imagem falhar (`onError`), exibe placeholder `🎵`; `key` forçará criação do `<img>` ao trocar de música
- **Menu de capa**: botão direito no placeholder abre "Baixar capa do álbum"
- **Cache de letras**: arquivos `.txt` salvos na pasta do álbum e reutilizados
- **Polling**: `setInterval` a cada 2s atualiza barra de progresso e status

---

## Configuração Tauri

| Configuração | Valor |
|---|---|
| Nome do produto | `ovelhafy` |
| Identificador | `com.mp3player.app` |
| Tamanho da janela | 1400x800 (mínimo: 1000x600) |
| URL de dev | `http://localhost:8112` |
| Frontend dist | `../dist` |
| Bundle targets | `all` (MSI, NSIS, etc.) |

---

## Scripts npm

| Script | Comando | Descrição |
|---|---|---|
| `dev` | `tauri dev` | Modo desenvolvimento desktop (Vite + Tauri) |
| `build` | `tauri build` | Build de produção desktop |
| `dev:vite` | `vite` | Servidor Vite apenas (porta 8112) |
| `build:vite` | `vite build` | Build Vite apenas |
| `preview` | `vite preview` | Preview do build de produção |

---

## Como rodar

### Pré-requisitos

- **Node.js 18+** e **npm**
- **Rust** (para compilar o Tauri)
- Backend rodando em `http://localhost:8111`

### Instalar dependências

```bash
cd frontend
npm install
```

### Apenas Vite (navegador)

```bash
npm run dev:vite
```

Rota em `http://localhost:8112`.

### Tauri desktop (recomendado)

```bash
npm run dev
```

Inicia o Vite e abre a janela desktop do Tauri. O Rust compila automaticamente na primeira execução.

---

## Como empacotar

```bash
npm run build
```

Executa `vite build` (via `beforeBuildCommand`) e compila o binário Tauri + instaladores MSI/NSIS.

Os instaladores ficam em `src-tauri/target/release/bundle/{msi,nsis}/`.

### Pré-requisitos para build

- **Rust toolchain** instalado (via [rustup](https://rustup.rs))
- **Tauri CLI** (incluído nas dependências do projeto)
- **MSVC Build Tools** (Windows) ou equivalentes (Linux/macOS)

---

## Estrutura Tauri

```
src-tauri/
├── src/
│   ├── main.rs          # Ponto de entrada
│   └── lib.rs           # Spawn do backend Java como processo filho
├── tauri.conf.json      # Configuração do Tauri
├── Cargo.toml           # Dependências Rust (tauri v2, serde)
├── capabilities/        # Permissões do Tauri
└── icons/               # Ícones da aplicação
```

O `lib.rs` gerencia o ciclo de vida do backend Java: inicia o processo ao abrir a janela e o encerra ao fechar.