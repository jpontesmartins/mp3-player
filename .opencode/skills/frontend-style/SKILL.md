---
name: frontend-style
description: Estilo de frontend monocromático. (React 18 + Vite + TypeScript strict + CSS puro + ícones MUI). Use em interfaces desktop/Tauri ou web quando o usuário quiser o mesmo visual dark monocromático, minimalista e em português.
---

# Skill: Frontend Monocromático & Minimalista (React + Vite + TS + CSS)

Guia portátil para reproduzir o estilo visual e a arquitetura de componentes usados no MP3 Player em outros projetos.

## Objetivo
Interfaces limpas, coerentes e elegantes com paleta estritamente monocromática (dark),
arquitetura de componentes modular, TypeScript strict e zero dependência de framework de UI
(o CSS é puro; o MUI é usado **apenas** para ícones).

## Tech stack

| Camada | Tecnologia |
|---|---|
| Framework UI | React 18 |
| Bundler | Vite 6 |
| Linguagem | TypeScript (strict, noUnusedLocals, noUnusedParameters) |
| Ícones | `@mui/icons-material` (apenas ícones; sem componentes de tema) |
| Estilização | CSS puro em arquivos `.css` globais |
| Runtime desktop | Tauri v2 (Rust) — opcional |

## 1. Diretriz visual: paleta monocromática estrita

**Proibido:** cores primárias coloridas (azul, verde chamativo, vermelho de destaque)
em elementos estruturais, botões principais ou fundos.

**Permitido exclusivamente** (hex exatos usados no projeto):

| Token | Hex | Uso |
|---|---|---|
| Fundo da página | `#000` | `body` |
| Superfície de painel | `#0d0d0d` | Cards, painéis (letra, playlist, config, coleção) |
| Superfície interna | `#111` | Inputs, áreas agrupadas |
| Botão/toolbar base | `#1a1a1a` | Ações genéricas, header de tabela |
| Botão de mídia/ativo | `#2a2a2a` | Controles do player, item selecionado |
| Hover | `#2a2a2a` / `#3a3a3a` | Hover de botões e listas |
| Borda padrão | `#333` / `#1a1a1a` / `#222` / `#2a2a2a` | Bordas de controles e painéis |
| Texto primário | `#eee` / `#fff` | Títulos, label ativo |
| Texto secundário | `#888` / `#777` / `#999` | Status, dicas, subtítulos |
| Texto inativo/placeholder | `#555` / `#666` | Linha de destaque, placeholder, footer |
| Desabilitado | `opacity: 0.4` | Botão inativo (`cursor: not-allowed`) |

**Exceção funcional:** cores semânticas com extremo comedimento, apenas em feedbacks
de formulários — sucesso aprox. `#7cd67c`, erro aprox. `#e07b7b`.

## 2. Tipografia e espaçamento

- Fonte: `'Segoe UI', Tahoma, Geneva, Verdana, sans-serif`.
- Texto base pequeno e legível: `0.85rem` a `0.9rem`.
- Números de tempo/duração: `font-variant-numeric: tabular-nums` para não "pular".
- Bordas arredondadas: `4px` (listas/botões pequenos), `6px` (inputs/botões), `8px` (painéis), `10px` (modais).
- Transições padrão: `background 0.2s, border-color 0.2s` e hover `0.15s` em listas.
- Todo elemento usa `box-sizing: border-box` (reset global).

## 3. Layout

```
#app (100vh, flex column, gap 16px, max-width 1200px, padding 20px 24px 6px)
 └─ #toolbar             (row de botões de navegação, 32x32)
 └─ #main-content        (flex: 1, row, gap 16px, min-height: 0)
     ├─ painel principal (flex: 2)  ← letra / config / coleção
     └─ #right-panel     (flex: 1, column) → Player + Playlist
 └─ #statusbar           (rodapé, flex-shrink: 0)
```

- Proporção principal de 2/3 e painel lateral 1/3.
- `min-height: 0` nos contêineres flex para permitir o scroll interno correto.
- Altura travada em `100vh`; os scrolls são internos aos painéis, não da página.

## 4. Padrões de componentes (React)

- **TypeScript estrito:** zero `any`. Interfaces `Props` explícitas por componente.
- Componente default export, nome PascalCase, pasta `src/components/`.
- Props tipadas e colbadas em uma única linha somente quando curtas; senão quebre.
- Estado: local com `useState`/`useRef`; nada de bibliotecas pesadas de estado.
- **IDs (`#...`)** para elementos únicos (painéis e controles principais).
- **Classes (`.classe`)** para elementos repetidos (itens de lista, inputs de tabela).
- Texto da UI em **português (pt-BR)**.

### Botões
- Genérico: `padding: 10px 20px`, borda `#333`, raio `6px`, `font-weight: 600`, `0.9rem`.
- Toolbar/controles de mídia: `32x32` fixos (`width/height: 32px; padding: 0`), ícone centralizado.
- Icon buttons: `display: flex; align-items: center; justify-content: center`.
- Inativo: `:disabled { opacity: 0.4; cursor: not-allowed; }`.

### Inputs / controles
- Input de texto: `background #111`, border `#222`, raio `6px`, `outline: none`, foco `border-color: #555`.
- Radio em grupo de cards `.settings-radio` (oculta o `<input>`, usa `.active` para destaque).
- Checkbox com `accent-color: #888`.

### Listas (playlist / coleção)
- `<ul>` sem marcador, itens com `padding: 7px 10px`, `border-radius: 4px`, hover `#1a1a1a`.
- Item ativo: `background: #2a2a2a; color: #fff`.
- Nome com elipse: `flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap`.

### Scrollbar customizada (WebKit)
```css
.panel::-webkit-scrollbar { width: 6px; }
.panel::-webkit-scrollbar-track { background: #0d0d0d; }
.panel::-webkit-scrollbar-thumb { background: #333; border-radius: 3px; }
```

### Modal
- Overlay fixo: `rgba(0,0,0,0.7)`, `z-index: 1000`, fecha no clique de fora e em `Escape`.
- Dialog: `background #111`, border `#333`, raio `10px`.

## 5. Regras de ouro

- **Contraste** por valor claro/escuro, nunca por cor.
- Tudo que é interativo tem hover; toda ação primária tem `:disabled`.
- Numérios sempre `tabular-nums`; tempas com `HH:MM:SS` (unico formato, função `formatTime`).
- Sem scroll na página (`:100vh`); cada painel rola isolado.
- Não adicionar comentários ao código; o CSS é autoexplicativo com seções `/* ── Nome ── */`.
- Manter o bundle leve: MUI somente para ícones, hooks leves, sem dependências pesadas.

## 6. Checklist ao criar um novo projeto com este estilo

1. Criar palco: React 18 + Vite + TS strict. Confirmar `strict`, `noUnusedLocals`, `noUnusedParameters`.
2. Aplicar o reset global (`* { margin:0; padding:0; box-sizing:border-box }`) e `body` `#000`.
3. Seguir a paleta da seção 1 (nunca inventar cores fora da escala de cinza).
4. Adotar a fonte Segoe UI e os raios/espaçamentos da seção 2.
5. Usar `min-height: 0` em flex scrollable e travar `height: 100vh` na app.
6. Componentes com Props tipadas, IDs singleton + classes repetíveis, textos em pt-BR.
7. Botões de ferramenta/mídia de 32px; painéis com scrollbar custom de 6px.
8. Estados `:disabled` com `opacity 0.4`; modais fecháveis com overlay + Escape.

## Referência de contexto

Codebase de referência: `frontend/` (App.css, App.tsx, components/) — consultar para
copiar classes e valores exatos ao replicar.