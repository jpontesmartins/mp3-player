# Especificação Técnica — ofy-013 — Dicionário (significado de palavra da letra)

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Módulo `dictionary`: `DictionaryController → DictionaryLookupService → DictionarySource` (mapa idioma → fonte). `PriberamSource` estende `AbstractDictionarySource` (Template Method).

## Arquitetura do Sistema

### Visão Geral dos Componentes

- Backend: `DictionaryController`, `DictionaryLookupService`, `DictionarySource`, `AbstractDictionarySource`, `PriberamSource`, `DictionaryLookupResult(word, source, language, meanings)`.
- Frontend: `features/dictionary/**` (modal + menu), `LyricsPanel` (gatilho), `shared/ui/ContextMenu.tsx`, `shared/api/dictionary.ts`.
- Extração: seletor `.dp-definicao` (linhas `py-4 dp-definicao-linha`).

## Design de Implementação

### Interfaces Principais

```java
public interface DictionarySource { String getLanguage(); Optional<DictionaryLookupResult> lookup(String word) throws IOException; }
```

### Modelos de Dados

- `DictionaryLookupRequest { word, language }`; resposta `{ word, source, language, meanings }`.

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `POST /dictionary/lookup` | `{ "word": "...", "language": "pt" }` | `200` resultado / `400` palavra vazia / `404` sem resultado |
| `GET /dictionary/languages` | — | `["pt"]` |

## Pontos de Integração

- Priberam via Jsoup.

## Abordagem de Testes

### Testes Unitários

- `DictionaryLookupServiceTest`, `DictionaryLookupResultTest`, `PriberamSourceTest`, `DictionaryControllerTest`. Frontend sem testes.

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Dicionário integrado ao painel de letras (1.5.x/1.6.0).

### Dependências Técnicas

- `ofy-012`.

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs SLF4J do backend com o prefixo indicado nas classes envolvidas; no app instalado vão para `<app_log_dir>/backend.log` (caminho visível em Configurações → "Local do log do backend").

## Considerações Técnicas

### Decisões Principais

- Estratégia por idioma para permitir novas fontes sem alterar o serviço.

### Riscos Conhecidos

- Fragilidade a mudanças de HTML do Priberam; dependência de internet.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)
- `.opencode/skills/frontend-style/SKILL.md` (frontend).

### Arquivos relevantes

- `backend/src/main/java/com/ovelha/fy/dictionary/**`
- `frontend/src/features/dictionary/**`, `frontend/src/features/lyrics/ui/LyricsPanel.tsx`
