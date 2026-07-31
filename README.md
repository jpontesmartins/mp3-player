# MP3 Player

Player de música MP3 com frontend desktop (Tauri + React) e backend em Java (Spring Boot).

## Estrutura

```
mp3-player/
├── backend/        # API REST em Java 21 + Spring Boot
├── frontend/       # App desktop com Tauri + React + Vite + TypeScript
└── README.md       # Este arquivo
```

## Como executar

### Backend

```bash
cd backend
mvn spring-boot:run
```

A API roda em `http://localhost:8080`.

### Frontend (desenvolvimento)

```bash
cd frontend
npm install
npm run dev
```

O Vite abre em `http://localhost:5173`.

### Frontend (build Tauri)

```bash
cd frontend
npm run tauri build
```

## Funcionalidades principais

- Reprodução de arquivos MP3 (play, pausa, stop, seek)
- Navegação entre faixas (anterior / próxima)
- Três modos de reprodução: Contínua, Aleatória e Repetição
- Leitura e exibição de tags ID3 (artista, título, álbum, duração)
- Capa do álbum extraída de arquivos `cover`/`folder`/`album`/`front`/`art`/`artwork` (jpg/png) na pasta da música
- Busca e cache de letras via letras.mus.br com fallback de busca
- Cache de letras em arquivos `.txt` na pasta do álbum
- Controle de tamanho da fonte da letra (A+/A-)
- Playlist selecionável por pasta com total de músicas e duração total
- Gerenciador de coleção: lista de álbuns e artistas da playlist com edição das tags ID3 (música, álbum, gênero, faixa, etc.) em grade por álbum/artista
- Barra de progresso clicável (seek)
- Interface escura com tema `#000`/`#0d0d0d` e ícones unicode
- Botões de mídia 32×32 com fundo escuro e texto branco
- Toolbar com navegação entre painéis (letra / configurações)
- Todos os endpoints e serviços com logging via SLF4J
