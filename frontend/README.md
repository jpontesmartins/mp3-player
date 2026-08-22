# Frontend — ovelhafy

Aplicacao desktop construida com **Tauri v2 + React 18 + Vite + TypeScript (strict)**.

---

## Arquitetura

O frontend e organizado por **modulos de negocio**, onde cada modulo representa um contexto da aplicacao e contem seus proprios componentes e tipos.

### Estrutura

```
src/
├── main.tsx                    # Entrada da aplicacao
├── App.tsx                     # Componente raiz (estado global, polling, auto-play)
├── App.css                     # Estilos globais + CSS custom properties (temas)
├── config.ts                   # URL da API backend
├── searchParser.ts             # Parser de busca avancada
├── vite-env.d.ts               # Tipos Vite
│
├── player/                     # Modulo: Reproducao
│   ├── components/
│   │   ├── Player.tsx          # Capa, controles de midia, barra de progresso
│   │   ├── Playlist.tsx        # Lista de musicas com tooltip ID3
│   │   └── FolderSelector.tsx  # Input de caminho de pasta
│   └── types.ts
│
├── lyrics/                     # Modulo: Letras
│   ├── components/
│   │   └── LyricsPanel.tsx     # Exibicao/edicao de letras, controles A+/A-
│   └── types.ts
│
├── metadata/                   # Modulo: Metadados
│   ├── components/
│   │   ├── CollectionManager.tsx   # Lista de albuns/artistas com edicao em grade
│   │   ├── BulkId3Editor.tsx       # Edicao em lote de tags
│   │   └── InfoModal.tsx           # Dialog "Sobre"
│   └── types.ts
│
└── settings/                   # Modulo: Configuracoes
    ├── components/
    │   ├── SettingsPanel.tsx    # Modo de reproducao, tema, capa, seletor de pasta
    │   ├── PlaylistManager.tsx  # CRUD de playlists virtuais (duas colunas)
    │   └── Toolbar.tsx         # Navegacao: Letra, Colecao, Config, Info
    └── types.ts
```

### Modulos

| Modulo | Responsabilidade | Componentes |
|---|---|---|
| **player** | Reproducao de audio, playlist visual, selecao de pasta | `Player`, `Playlist`, `FolderSelector` |
| **lyrics** | Exibicao e edicao de letras | `LyricsPanel` |
| **metadata** | Gerenciamento de metadados, colecao, edicao em lote | `CollectionManager`, `BulkId3Editor`, `InfoModal` |
| **settings** | Configuracoes, playlists virtuais, navegacao | `SettingsPanel`, `PlaylistManager`, `Toolbar` |

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Framework UI | React 18 |
| Bundler | Vite 6 |
| Linguagem | TypeScript (strict, noUnusedLocals, noUnusedParameters) |
| Desktop | Tauri v2 (Rust) |
| Icones | MUI (Material UI) 9.2 |
| Estilizacao | CSS puro (CSS custom properties) |

---

## Paleta de cores

### Tema escuro (padrao)

| Elemento | Cor | Uso |
|---|---|---|
| Fundo | `#000` | Body |
| Superficies | `#0d0d0d` | Paineis (letra, playlist, configuracoes) |
| Botoes padrao | `#1a1a1a` / borda `#333` | Acoes genericas |
| Botoes de midia | `#2a2a2a` / 32x32 | Play/pause/stop/anterior/proxima |
| Texto primario | `#eee` | Titulos, labels |
| Texto secundario | `#888` / `#777` | Status, dicas |
| Link/ativo | `#ccc` / `#eee` | Item selecionado |
| Desabilitado | Opacity `0.4` | Botao inativo |

### Tema claro

| Elemento | Cor |
|---|---|
| Fundo | `#f4f4f4` |
| Superficies | `#e7e7e7` |
| Botoes | `#cccccc` / borda `#a0a0a0` |
| Texto primario | `#111` |
| Texto secundario | `#555` / `#666` |

Os temas sao implementados via CSS custom properties (`--c-*`) alternados pelo atributo `data-theme` no `<body>`.

---

## Persistencia no localStorage

| Chave | Tipo | Descricao |
|---|---|---|
| `mp3_folder` | `string` | Caminho da ultima pasta carregada |
| `mp3_theme` | `'dark' \| 'light'` | Tema selecionado |

---

## Intercorrer

- **Auto-play**: ao fim da musica, toca a proxima conforme o modo (Continua -> proxima, Aleatoria -> aleatoria, Repeticao -> mesma)
- **Intentional stop**: impede auto-play em Stop manual ou carregamento de nova playlist
- **Seek**: clique na barra de progresso para ir a qualquer ponto
- **Capa**: se a imagem falhar (`onError`), exibe placeholder `🎵`; `key` forcera criacao do `<img>` ao trocar de musica
- **Menu de capa**: botao direito no placeholder abre "Baixar capa do album"
- **Cache de letras**: arquivos `.txt` salvos na pasta do album e reutilizados
- **Polling**: `setInterval` a cada 2s atualiza barra de progresso e status

---

## Configuracao Tauri

| Configuracao | Valor |
|---|---|
| Nome do produto | `ovelhafy` |
| Identificador | `com.mp3player.app` |
| Tamanho da janela | 1400x800 (minimo: 1000x600) |
| URL de dev | `http://localhost:8112` |
| Frontend dist | `../dist` |
| Bundle targets | `all` (MSI, NSIS, etc.) |

---

## Scripts npm

| Script | Comando | Descricao |
|---|---|---|
| `dev` | `tauri dev` | Modo desenvolvimento desktop (Vite + Tauri) |
| `build` | `tauri build` | Build de producao desktop |
| `dev:vite` | `vite` | Servidor Vite apenas (porta 8112) |
| `build:vite` | `vite build` | Build Vite apenas |
| `preview` | `vite preview` | Preview do build de producao |

---

## Como rodar

### Pre-requisitos

- **Node.js 18+** e **npm**
- **Rust** (para compilar o Tauri)
- Backend rodando em `http://localhost:8111`

### Instalar dependencias

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

Inicia o Vite e abre a janela desktop do Tauri. O Rust compila automaticamente na primeira execucao.

---

## Como empacotar

```bash
npm run build
```

Executa `vite build` (via `beforeBuildCommand`) e compila o binario Tauri + instaladores MSI/NSIS.

Os instaladores ficam em `src-tauri/target/release/bundle/{msi,nsis}/`.

### Pre-requisitos para build

- **Rust toolchain** instalado (via [rustup](https://rustup.rs))
- **Tauri CLI** (incluido nas dependencias do projeto)
- **MSVC Build Tools** (Windows) ou equivalentes (Linux/macOS)

---

## Estrutura Tauri

```
src-tauri/
├── src/
│   ├── main.rs          # Ponto de entrada
│   └── lib.rs           # Spawn do backend Java como processo filho
├── tauri.conf.json      # Configuracao do Tauri
├── Cargo.toml           # Dependencias Rust (tauri v2, serde)
├── capabilities/        # Permissoes do Tauri
└── icons/               # Icones da aplicacao
```

O `lib.rs` gerencia o ciclo de vida do backend Java: inicia o processo ao abrir a janela e o encerra ao fechar.