---
name: frontend-style
description: Estilo de frontend monocromático. (React 18 + Vite + TypeScript strict + CSS puro + ícones MUI). Use em interfaces desktop/Tauri ou web quando o usuário quiser o mesmo visual monocromático, minimalista e em português. Suporta temas claro e escuro.
---

# Skill: Frontend Monocromático & Minimalista (React + Vite + TS + CSS)

Guia portátil para reproduzir o estilo visual e a arquitetura de componentes usados no ovelhafy em outros projetos.

## Objetivo
Interfaces limpas, coerentes e elegantes com paleta estritamente monocromática,
arquitetura de componentes modular, TypeScript strict e zero dependência de framework de UI
(o CSS é puro; o MUI é usado **apenas** para ícones).
Suporta **temas claro e escuro**, com o escuro como padrão.

## Tech stack

| Camada | Tecnologia |
|---|---|
| Framework UI | React 18 |
| Bundler | Vite 6 |
| Linguagem | TypeScript (strict, noUnusedLocals, noUnusedParameters) |
| Ícones | `@mui/icons-material` (apenas ícones; sem componentes de tema) |
| Estilização | CSS puro em arquivos `.css` globais + CSS custom properties (variables) |
| Runtime desktop | Tauri v2 (Rust) — opcional |

## 1. Diretriz visual: paleta monocromática estrita

**Proibido:** cores primárias coloridas (azul, verde chamativo, vermelho de destaque)
em elementos estruturais, botões principais ou fundos.

### Tema escuro (padrão)

| Token | Hex | Uso |
|---|---|---|
| Fundo da página | `#000` | `body` |
| Superfície (surface) | `#0d0d0d` | Cards, painéis, container principal |
| Superfície 2 | `#111` | Inputs, áreas agrupadas internas |
| Superfície 3 | `#1a1a1a` | Botões base, toolbar, header de tabela |
| Superfície 4 | `#2a2a2a` | Controles ativos, hover de botões, item selecionado |
| Borda padrão | `#222` | Bordas de controles e painéis |
| Borda secundária | `#333` | Bordas de botões, scrollbars |
| Foco (border-focus) | `#555` | Input em foco, borda de destaque |
| Texto primário | `#eee` | Texto geral |
| Texto forte | `#fff` | Títulos, label ativo |
| Texto secundário | `#888` | Status, dicas, subtítulos |
| Texto弱化 (dim) | `#555` | Texto desabilitado, placeholders |
| Texto弱化 2 (faint) | `#666` | Linhas de destaque, footer |
| Texto suave (soft) | `#aaa` | Texto auxiliar |
| Perigo | `#e07b7b` | Erros, ações destrutivas |
| Fundo do perigo | `#2a1a1a` | Background de mensagens de erro |
| Sucesso | `#7cd67c` | Feedbacks positivos |
| Fundo do sucesso | `#1a2a1a` | Background de mensagens de sucesso |
| Overlay | `rgba(0,0,0,0.7)` | Fundo de modais |

### Tema claro

| Token | Hex | Uso |
|---|---|---|
| Fundo da página | `#ffffff` | `body` |
| Superfície (surface) | `#f7f7f7` | Cards, painéis, container principal |
| Superfície 2 | `#ececec` | Inputs, áreas agrupadas internas |
| Superfície 3 | `#e2e2e2` | Botões base, toolbar, header de tabela |
| Superfície 4 | `#cfcfcf` | Controles ativos, hover de botões, item selecionado |
| Borda padrão | `#d4d4d4` | Bordas de controles e painéis |
| Borda secundária | `#b8b8b8` | Bordas de botões, scrollbars |
| Foco (border-focus) | `#8a8a8a` | Input em foco, borda de destaque |
| Texto primário | `#1a1a1a` | Texto geral |
| Texto forte | `#000` | Títulos, label ativo |
| Texto secundário | `#555` | Status, dicas, subtítulos |
| Texto弱化 (dim) | `#767676` | Texto desabilitado, placeholders |
| Texto弱化 2 (faint) | `#8a8a8a` | Linhas de destaque, footer |
| Texto suave (soft) | `#404040` | Texto auxiliar |
| Perigo | `#b3261e` | Erros, ações destrutivas |
| Fundo do perigo | `#fdecea` | Background de mensagens de erro |
| Sucesso | `#1e7d32` | Feedbacks positivos |
| Fundo do sucesso | `#e8f5e9` | Background de mensagens de sucesso |
| Overlay | `rgba(0,0,0,0.35)` | Fundo de modais |

**Exceção funcional:** cores semânticas (perigo/sucesso) com extremo comedimento, apenas em feedbacks
de formulários e mensagens.

## 2. Sistema de temas (CSS variables)

### Implementação no CSS

```css
/* ── Tema escuro (padrão) ── */
:root {
  --bg: #000;
  --surface: #0d0d0d;
  --surface-2: #111;
  --surface-3: #1a1a1a;
  --surface-4: #2a2a2a;
  --border: #222;
  --border-2: #333;
  --border-focus: #555;
  --text: #eee;
  --text-strong: #fff;
  --text-muted: #888;
  --text-dim: #555;
  --text-faint: #666;
  --text-soft: #aaa;
  --danger: #e07b7b;
  --danger-bg: #2a1a1a;
  --success: #7cd67c;
  --success-bg: #1a2a1a;
  --overlay: rgba(0, 0, 0, 0.7);
}

/* ── Tema claro ── */
[data-theme='light'] {
  --bg: #ffffff;
  --surface: #f7f7f7;
  --surface-2: #ececec;
  --surface-3: #e2e2e2;
  --surface-4: #cfcfcf;
  --border: #d4d4d4;
  --border-2: #b8b8b8;
  --border-focus: #8a8a8a;
  --text: #1a1a1a;
  --text-strong: #000;
  --text-muted: #555;
  --text-dim: #767676;
  --text-faint: #8a8a8a;
  --text-soft: #404040;
  --danger: #b3261e;
  --danger-bg: #fdecea;
  --success: #1e7d32;
  --success-bg: #e8f5e9;
  --overlay: rgba(0, 0, 0, 0.35);
}
```

### Implementação no React

Criar um arquivo `theme.ts` para gerenciar o tema:

```typescript
export type Theme = 'dark' | 'light'

const STORAGE_KEY = 'appTheme'

export const DEFAULT_THEME: Theme = 'dark'

export function getTheme(): Theme {
  const saved = localStorage.getItem(STORAGE_KEY)
  return saved === 'light' ? 'light' : 'dark'
}

export function applyTheme(theme: Theme): void {
  document.documentElement.setAttribute('data-theme', theme)
}

export function saveTheme(theme: Theme): void {
  localStorage.setItem(STORAGE_KEY, theme)
}
```

No `App.tsx`:

```typescript
const [theme, setTheme] = useState<Theme>(getTheme)

useEffect(() => {
  applyTheme(theme)
}, [theme])

const handleThemeChange = useCallback((next: Theme) => {
  setTheme(next)
  saveTheme(next)
}, [])
```

## 3. Tipografia e espaçamento

- Fonte: `'Segoe UI', Tahoma, Geneva, Verdana, sans-serif`.
- Texto base pequeno e legível: `0.85rem` a `0.9rem`.
- Números de tempo/duração: `font-variant-numeric: tabular-nums`.
- Bordas arredondadas: `4px` (listas/botões pequenos), `6px` (inputs/botões), `8px` (painéis), `10px` (modais).
- Transições padrão: `background 0.2s, border-color 0.2s, color 0.2s` e hover `0.15s` em listas.
- Todo elemento usa `box-sizing: border-box` (reset global).

## 4. Layout principal

```
#app (100vh, flex column, gap 16px, max-width 1600px, padding 20px 24px 6px)
 ├─ #toolbar              (row de botões de navegação, 32x32)
 ├─ [mensagem erro/aviso]  (condicional)
 ├─ #main-content         (flex: 1, row, gap 16px, min-height: 0)
 │   ├─ painel principal  (flex: 2)  ← conteúdo principal / config / sobre
 │   └─ #playlist-panel   (flex: 1, column) → playlist / lista lateral
 └─ #statusbar            (rodapé, flex-shrink: 0)
```

- **Toolbar** sempre acima da seção principal, com botões de 32x32 para navegação entre painéis.
- Proporção padrão: painel principal 2/3, painel lateral 1/3.
- `min-height: 0` nos contêineres flex para permitir o scroll interno correto.
- Altura travada em `100vh`; os scrolls são internos aos painéis, não da página.

### Toolbar

```css
#toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: var(--surface-3);
  border: 1px solid var(--border-2);
  border-radius: 4px;
  color: var(--text-muted);
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.toolbar-btn:hover {
  background: var(--surface-4);
  color: var(--text);
}

.toolbar-btn.active {
  background: var(--surface-4);
  color: var(--text-strong);
  border-color: var(--border-focus);
}
```

## 5. Padrões de componentes (React)

- **TypeScript estrito:** zero `any`. Interfaces `Props` explícitas por componente.
- Componente default export, nome PascalCase, pasta `src/components/`.
- Props tipadas e colbadas em uma única linha somente quando curtas; senão quebre.
- Estado: local com `useState`/`useRef`; nada de bibliotecas pesadas de estado.
- **IDs (`#...`)** para elementos únicos (painéis e controles principais).
- **Classes (`.classe`)** para elementos repetidos (itens de lista, inputs de tabela).
- Texto da UI em **português (pt-BR)**.

### Botões
- Genérico: `padding: 10px 20px`, borda `var(--border-2)`, raio `6px`, `font-weight: 600`, `0.9rem`.
- Toolbar/controles de mídia: `32x32` fixos (`width/height: 32px; padding: 0`), ícone centralizado.
- Icon buttons: `display: flex; align-items: center; justify-content: center`.
- Inativo: `:disabled { opacity: 0.4; cursor: not-allowed; }`.

### Inputs / controles
- Input de texto: `background var(--surface-2)`, border `var(--border)`, raio `6px`, `outline: none`, foco `border-color: var(--border-focus)`.
- Radio em grupo de cards `.settings-radio` (oculta o `<input>`, usa `.active` para destaque).
- Checkbox com `accent-color: var(--text-muted)`.

### Listas (playlist / coleção)
- `<ul>` sem marcador, itens com `padding: 7px 10px`, `border-radius: 4px`, hover `var(--surface-3)`.
- Item ativo: `background: var(--surface-4); color: var(--text-strong)`.
- Nome com elipse: `flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap`.

### Scrollbar customizada (WebKit + Firefox)

**Todo elemento scrollável DEVE ter scrollbar estilizada.**

```css
/* ── Scrollbar WebKit (Chrome, Safari, Edge) ── */
.scrollable::-webkit-scrollbar,
.panel::-webkit-scrollbar {
  width: 6px;
}

.scrollable::-webkit-scrollbar-track,
.panel::-webkit-scrollbar-track {
  background: var(--surface);
}

.scrollable::-webkit-scrollbar-thumb,
.panel::-webkit-scrollbar-thumb {
  background: var(--border-2);
  border-radius: 3px;
}

/* ── Scrollbar Firefox ── */
.scrollable,
.panel {
  scrollbar-width: thin;
  scrollbar-color: var(--border-2) var(--surface);
}
```

Aplicar a classe `.scrollable` ou `.panel` em todo elemento com `overflow-y: auto/scroll`.
Para scroll horizontal, adicionar também `height: 6px` no thumb e track.

### Modal
- Overlay fixo: `var(--overlay)`, `z-index: 1000`, fecha no clique de fora e em `Escape`.
- Dialog: `background var(--surface-2)`, border `var(--border-2)`, raio `10px`.

## 6. Regras de ouro

- **Contraste** por valor claro/escuro, nunca por cor.
- Tudo que é interativo tem hover; toda ação primária tem `:disabled`.
- Números sempre `tabular-nums`; tempos com `HH:MM:SS` (único formato, função `formatTime`).
- Sem scroll na página (`height: 100vh`); cada painel rola isolado.
- Sempre ter scrolls estilizados em todos os elementos scrolláveis.
- Não adicionar comentários ao código; o CSS é autoexplicativo com seções `/* ── Nome ── */`.
- Manter o bundle leve: MUI somente para ícones, hooks leves, sem dependências pesadas.
- Tema escuro como padrão; tema claro como opção acessível.

## 7. Checklist ao criar um novo projeto com este estilo

1. Criar palco: React 18 + Vite + TS strict. Confirmar `strict`, `noUnusedLocals`, `noUnusedParameters`.
2. Aplicar o reset global (`* { margin:0; padding:0; box-sizing:border-box }`) e `body` `background: var(--bg)`.
3. Criar `theme.ts` com `getTheme`, `applyTheme`, `saveTheme` (padrão: `dark`).
4. Definir CSS variables para temas escuro (`:root`) e claro (`[data-theme='light']`) na seção 2.
5. Seguir a paleta da seção 1 (nunca inventar cores fora da escala de cinza).
6. Adotar a fonte Segoe UI e os raios/espaçamentos da seção 3.
7. Usar `min-height: 0` em flex scrollable e travar `height: 100vh` na app.
8. Toolbar com botões 32x32 acima de `#main-content` (seção 4).
9. Componentes com Props tipadas, IDs singleton + classes repetíveis, textos em pt-BR.
10. Botões de ferramenta/mídia de 32px; painéis com scrollbar custom de 6px.
11. Estados `:disabled` com `opacity 0.4`; modais fecháveis com overlay + Escape.
12. Aplicar scrollbar customizada (WebKit + Firefox) em TODOS os elementos scrolláveis.

## Referência de contexto

Codebase de referência: `mp3-player/frontend/` e `video-player/frontend/`
— consultar para copiar classes e valores exatos ao replicar.
