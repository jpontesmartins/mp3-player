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
- Leitura e exibição de tags ID3 (artista, título, álbum)
- Capa do álbum extraída de arquivos `cover.jpg`/`png` na pasta da música
- Busca e cache de letras no site letras.mus.br
- Playlist selecionável por pasta
- Interface escura com ícones unicode
