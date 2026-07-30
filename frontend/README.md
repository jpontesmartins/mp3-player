# Frontend — MP3 Player

Aplicação desktop construída com **Tauri + React 18 + Vite + TypeScript**.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Framework UI | React 18 |
| Bundler | Vite |
| Linguagem | TypeScript (strict) |
| Desktop | Tauri (Rust) |
| Estilização | CSS puro |

## Paleta de cores

Tema escuro com tons de cinza e detalhes sutis:

| Elemento | Cor | Uso |
|---|---|---|
| Fundo da página | `#000` | Body |
| Superfícies | `#0d0d0d` | Painéis (letra, playlist, configurações) |
| Botões padrão | `#1a1a1a` / borda `#333` | Ações genéricas |
| Botões toolbar | `#1a1a1a` / `32×32` | ⚙️ 📃 |
| Botões de mídia | `#2a2a2a` / `32×32` | ⏮ ▶ ⏸ ⏹ ⏭ |
| Texto primário | `#eee` | Títulos, labels |
| Texto secundário | `#888` / `#777` | Status, dicas |
| Link/ativo | `#ccc` / `#eee` | Item selecionado |
| Desabilitado | Opacity `0.4` | Botão inativo |

## Ícones

Todos os ícones são caracteres unicode — sem dependência de bibliotecas de ícones:

| Ícone | Significado |
|---|---|
| `▶` | Tocar / Retomar |
| `⏸` | Pausar |
| `⏹` | Parar |
| `⏮` | Música anterior |
| `⏭` | Próxima música |
| `⚙️` | Abrir configurações |
| `📃` | Abrir letra |
| `🎵` | Placeholder da capa |

## Componentes

### Toolbar
- Botões de navegação entre painéis (📃 letra / ⚙️ configurações)
- Botão ativo fica desabilitado (opacity reduzida)

### LyricsPanel
- Exibe "Buscar letra" se não houver letra em cache
- Ao trocar de música, verifica automaticamente se existe arquivo `.txt`
- Se existir, carrega e exibe o conteúdo
- Botão "Buscar letra" faz scraping via backend

### Player
- Capa do álbum (cover.jpg/png na mesma pasta do MP3)
- Controles: ⏮ ▶/⏸ ⏹ ⏭
- Barra de progresso clicável (seek)
- Exibe nome da faixa e status
- Placeholder `🎵` quando não há capa

### Playlist
- Lista de músicas da pasta selecionada
- Destaque na faixa atual
- Duração ao lado do nome
- Clique para tocar

### SettingsPanel
- Tipo de reprodução: Contínua / Aleatória / Repetição
- Habilitar capa do álbum (checkbox)
- Campo para selecionar pasta da playlist + botão "Carregar"

## Interações

- **Auto-play**: ao fim da música, toca a próxima conforme o modo selecionado
- **Seek**: clique na barra de progresso para ir a qualquer ponto
- **Capa**: se a imagem falhar (`onError`), exibe o placeholder `🎵`
- **Cache de letras**: arquivos `.txt` são salvos na pasta do álbum e reutilizados
- **Stop intencional**: não dispara auto-play (diferencia de fim natural da música)
