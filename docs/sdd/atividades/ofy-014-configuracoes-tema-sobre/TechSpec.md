# Especificação Técnica — ofy-014 — Configurações, tema e Sobre

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Frontend: `SettingsPanel`, `InfoModal`, `Toolbar`, `AppContext` (tema, view, modais). Backend: `InfoController` (`GET /info`).

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `features/settings/ui/SettingsPanel.tsx`, `InfoModal`, `widgets/Toolbar.tsx`, `app/providers/AppContext.tsx`, `App.tsx` (rodapé), `shared/api/info.ts`.
- Backend: `InfoController` lê `mp3.log-file`, `server.port`, `mp3.frontend-port`.

## Design de Implementação

### Interfaces Principais

```ts
getInfo(): Promise<{ logFile: string; cacheFile: string; backendPort: string; frontendPort: string } | null>
```

### Modelos de Dados

- `localStorage`: `mp3_theme`, `mp3_folder` (ver `ofy-003`).

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `GET /info` | — | `{ logFile, cacheFile, backendPort, frontendPort }` |

## Pontos de Integração

- Nenhuma externa.

## Abordagem de Testes

### Testes Unitários

- Sem testes específicos `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Tema e configurações (1.0) → informações do sistema e modal Sobre (1.x).

### Dependências Técnicas

- `ofy-003`, `ofy-009`, `ofy-015` (caminhos de log).

## Monitoramento e Observabilidade

- Sem telemetria. Erros de rede são silenciosos no cliente HTTP (`shared/api/client.ts`); diagnóstico via logs do backend e `console` da WebView.

## Considerações Técnicas

### Decisões Principais

- Tema por atributo no `body` e variáveis CSS (sem biblioteca de estilos).

### Riscos Conhecidos

- Valores de `/info` dependem das propriedades passadas pelo shell (`ofy-015`).

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/frontend-style/SKILL.md` (visual monocromático, Feature-Sliced Design, TS strict).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)

### Arquivos relevantes

- `frontend/src/features/settings/**`, `frontend/src/widgets/Toolbar.tsx`, `frontend/src/app/providers/AppContext.tsx`, `frontend/src/App.tsx`
- `backend/src/main/java/com/ovelha/fy/**/InfoController.java`
