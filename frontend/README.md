# Frontend — MP3 Player

Aplicação desktop construída com **Tauri + React 18 + Vite + TypeScript (strict)**.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Framework UI | React 18 |
| Bundler | Vite |
| Linguagem | TypeScript (strict, noUnusedLocals, noUnusedParameters) |
| Desktop | Tauri v2 (Rust) |
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

Ícones da janela (Tauri) gerados com `npx tauri icon` a partir de `icone-v1.png`.

## Componentes

### Toolbar
- Botões de navegação entre painéis (📃 letra / ⚙️ configurações)
- Botão ativo fica desabilitado (opacity reduzida)

### LyricsPanel
- Exibe "Buscar letra" se não houver letra em cache
- Ao trocar de música, verifica automaticamente se existe arquivo `.txt`
- Se existir, carrega e exibe o conteúdo
- Botão "Buscar letra" faz scraping via backend (`GET /lyrics`)
- Botão "Alterar letra" abre editor em textarea com Salvar/Cancelar (persistência via `POST /lyrics`)
- Controles A+/A- para aumentar/diminuir tamanho da fonte (0.7rem–2.0rem, passo 0.1)

### Player
- Capa do álbum (cover/folder/album/front/art/artwork.jpg/png/webp/gif na mesma pasta do MP3)
- Só renderiza quando `showCover=true` e há música carregada
- Placeholder `🎵` apenas quando a imagem falha (`onError`)
- **Clique com botão direito no 🎵** abre um menu de contexto com "Baixar capa do álbum"; ao clicar, o backend baixa a capa (iTunes/Deezer) e o Player recarrega a imagem automaticamente
- Controles: ⏮ ▶/⏸ ⏹ ⏭ (todos 32×32)
- Barra de progresso clicável (seek via `POST /seek`)
- Exibe nome da faixa e status

### Playlist
- Lista de músicas da pasta selecionada
- Cabeçalho com colunas redimensionáveis (artista 15–55%, tempo 40–160px) com grade compartilhada
- Tooltip com metad ID3 da faixa (Música, Artista, Álbum, Ano, Gênero, Faixa, Duração, Bitrate) após 1s de hover, com posição ajustada à janela
- Destaque na faixa atual
- Duração ao lado do nome
- Clique para tocar
- Footer com total de músicas e duração total (soma de `duration_ms` das tags ID3)

### SettingsPanel
- Tipo de reprodução: Contínua / Aleatória / Repetição
- Habilitar capa do álbum (checkbox `showCover`)
- Campo para selecionar pasta da playlist + botão "Carregar"

## Interações

- **Auto-play**: ao fim da música, toca a próxima conforme o modo selecionado (Contínua → próxima, Aleatória → aleatória, Repetição → mesma)
- **Intentional stop**: `intentionalStopRef` impede auto-play em Stop manual ou carregamento de nova playlist
- **Seek**: clique na barra de progresso para ir a qualquer ponto
- **Capa**: se a imagem falhar (`onError`), exibe o placeholder `🎵`; `key={currentFile}:{coverVersion}` força recriação do `<img>` ao trocar de música e ao concluir o download da capa (revela a imagem mesmo que a anterior tenha falhado)
- **Menu de capa**: botão direito no 🎵 abre o menu de contexto com "Baixar capa do álbum" (fecha em clique fora/scroll/redimensionar)
- **Cache de letras**: arquivos `.txt` são salvos na pasta do álbum e reutilizados
- **Reset de letra**: `useEffect` em `currentFile` limpa letra ao trocar de música
- **Polling**: `setInterval` a cada 500ms atualiza barra de progresso e status
