# PRD — ofy-014 — Configurações, tema e Sobre

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Painel **Configurações** (tema, modo de reprodução, capa, pasta da biblioteca e informações do sistema), modal **Sobre**, barra de ferramentas e rodapé com versão.

## Objetivos

- Permitir personalizar aparência e recarregar a biblioteca; expor caminhos de log/cache para diagnóstico.

## Histórias de Usuário

- Como usuário, quero alternar tema escuro/claro e que ele seja lembrado.
- Como usuário, quero recarregar a biblioteca forçando a releitura das tags.
- Como usuário, quero saber onde ficam log e cache.

## Funcionalidades Principais

1. **RF-014.1 Tema:** `dark`/`light` via `body[data-theme]`; persistido em `localStorage['mp3_theme']`.
2. **RF-014.2 Modo de reprodução:** `continuous`/`shuffle`/`repeat` (ver `ofy-002`); **não persistido**.
3. **RF-014.3 Capa:** checkbox `Habilitar mostrar capa do álbum` (ver `ofy-009`); **não persistido**.
4. **RF-014.4 Pasta:** campo + `Carregar` (carrega com `forceRefresh=true`; ver `ofy-003`).
5. **RF-014.5 Informações do sistema:** `GET /info` → arquivo de log, arquivo de cache, porta do backend (fallback `8111`) e do frontend (fallback `8112`).
6. **RF-014.6 Sobre:** modal com ícone, `Gerenciador de biblioteca de músicas.`, `Desenvolvido por ovelha-eletrica.`; `Fechar`/`Esc`.
7. **RF-014.7 Barra de ferramentas:** 4 botões (Letra, Coleção, Configurações, Sobre); rodapé `v1.6.0`.
8. **Critérios de aceite:** (a) tema persiste após reiniciar; (b) `Carregar` relê as tags; (c) `Sobre` abre e fecha com `Esc`.

## Experiência do Usuário

- Controles em radio/checkbox; seção de informações em texto monoespaçado; temas por variáveis CSS.

## Restrições Técnicas de Alto Nível

- Versão do rodapé é string fixa no `App.tsx`, atualizada pelo script de release (`ofy-016`).

## Fora de Escopo

- Persistência de modo/capa, seleção de porta, idioma da interface.

## Questões em Aberto

- O modelo `Settings` do backend não é usado `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Modo de reprodução e capa voltam ao padrão a cada abertura.
