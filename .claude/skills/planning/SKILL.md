---
name: planning
description: Gera PRD.md, TechSpec.md e referencias.md para uma atividade SDD a partir da pasta de input da atividade.
argument-hint: <id-da-atividade, ex: ofy-123>
---

Você deve planejar e especificar a atividade **$ARGUMENTS** seguindo o fluxo de Spec-Driven Development (SDD) deste repositório.

## 1. Localização e Validação de Entrada
1. Localize a pasta da atividade correspondente a `$ARGUMENTS` dentro de `docs/sdd/atividades/` (aceite tanto `docs/sdd/atividades/$ARGUMENTS` quanto `docs/sdd/atividades/$ARGUMENTS-*`).
2. Se nenhuma pasta existir para `$ARGUMENTS`, interrompa e informe o usuário.
3. Leia todo o conteúdo dentro de `<pasta-da-atividade>/input/` (arquivos `.md`, `.txt`, imagens, payloads, logs ou anotações). Se `input/` estiver vazio ou não existir, pare e peça a descrição da demanda.

## 2. Leitura de Contexto Obrigatório
Antes de redigir qualquer especificação, leia:
- `@docs/sdd/Rules.md` (regras arquiteturais, limites entre camadas e convenções obrigatórias)
- `@docs/sdd/PRD.md` (visão macro do produto)
- `@docs/sdd/TechSpec.md` (arquitetura macro do sistema)
- `@docs/sdd/templates/prd-template.md`
- `@docs/sdd/templates/techspec-template.md`
- O código-fonte real das áreas impactadas pela demanda para mapear arquivos, contratos e dependências existentes.

## 3. Entregáveis da Atividade
Dentro de `<pasta-da-atividade>/`, crie ou atualize os seguintes arquivos:

1. **`<pasta-da-atividade>/PRD.md`**
   - Baseado estritamente na estrutura de `@docs/sdd/templates/prd-template.md`.
   - Deve definir problema/objetivo, escopo fechado, fora de escopo, regras de negócio, fluxos de uso e critérios de aceite objetivos e testáveis.

2. **`<pasta-da-atividade>/TechSpec.md`**
   - Baseado estritamente na estrutura de `@docs/sdd/templates/techspec-template.md`.
   - Deve detalhar a solução técnica respeitando as fronteiras de `@docs/sdd/Rules.md`: arquivos a criar/alterar, contratos de API/IPC/eventos, modelos de dados, tratamento de erros, riscos e plano de testes.

3. **`<pasta-da-atividade>/referencias.md`**
   - Consolide rastreabilidade técnica: trechos/arquivos do código analisados, contratos externos, payloads de exemplo, logs de erro (se for bugfix) e decisões técnicas tomadas durante o planejamento.

## 4. Regras Rígidas
- **Não altere** nenhum arquivo em `docs/sdd/templates/` nem dentro de `<pasta-da-atividade>/input/`.
- **Não implemente código de produção** nesta etapa — apenas gere os documentos de especificação.
- Remova instruções internas dos templates e não deixe placeholders `[...]` sem preenchimento.
- Ao concluir, apresente um resumo objetivo da spec gerada e destaque eventuais dúvidas ou premissas que precisem de validação antes do `/dev`.