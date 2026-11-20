---
name: fix-review
description: Lê os apontamentos do code-review de uma atividade SDD e aplica apenas as correções aprovadas.
argument-hint: <id-da-atividade, ex: ofy-123> [itens ou instruções de aprovação]
---

Você deve aplicar as correções apontadas no code review da atividade **$ARGUMENTS** seguindo estritamente as especificações e regras do projeto.

## 1. Coleta de Contexto
1. Localize a pasta da atividade correspondente ao ID em `docs/sdd/atividades/` (`docs/sdd/atividades/<id>` ou `docs/sdd/atividades/<id>-*`).
2. Leia obrigatoriamente:
   - `@docs/sdd/Rules.md`
   - `<pasta-da-atividade>/PRD.md`
   - `<pasta-da-atividade>/TechSpec.md`
   - `<pasta-da-atividade>/referencias.md` (se existir)
3. Localize os apontamentos do code review na conversa atual ou em artefatos/notas dentro de `<pasta-da-atividade>/` (ex.: `code-review.md`, `review.md` ou `referencias.md`). Se não houver nenhum apontamento de code review disponível, pare e informe o usuário.

## 2. Filtragem Estrita de Itens Aprovados
Antes de alterar qualquer arquivo, classifique e filtre cada apontamento:
- **Aplique APENAS o que estiver aprovado:**
  - Itens explicitamente aprovados na mensagem/argumentos do usuário (ex.: por número, título, categoria como "críticos" ou "apenas bloqueantes").
  - Se o usuário passar apenas o ID e disser para aplicar os itens aprovados sem listar exceções, trate como aprovados os achados **Críticos (Bloqueantes)** e os itens marcados como aprovados no review, mantendo itens **Não-bloqueantes / opcionais** ou explicitamente **não aprovados / adiados** de fora, salvo se o usuário pedir para incluir não-bloqueantes.
- **Não aplique:**
  - Itens marcados como rejeitados, adiados, fora de escopo ou pendentes de decisão.
  - Refatorações extras ou alterações não solicitadas no review que fujam do `PRD.md`, `TechSpec.md` ou `Rules.md`.

## 3. Execução das Correções
- Implemente cada correção aprovada respeitando as fronteiras de camadas, convenções e arquivos protegidos de `@docs/sdd/Rules.md`.
- Crie ou atualize testes unitários e/ou de integração necessários para cobrir as correções aplicadas.
- Se alguma correção aprovada exigir ajuste em contrato ou decisão técnica documentada, atualize `<pasta-da-atividade>/TechSpec.md` e registre o racional em `<pasta-da-atividade>/referencias.md`.

## 4. Verificação e Relatório Final
1. Execute os comandos de build, lint e testes definidos em `@docs/sdd/Rules.md`.
2. Apresente um resumo objetivo contendo:
   - **Aplicados:** quais achados do review foram corrigidos e em quais arquivos.
   - **Não aplicados / Ignorados:** quais itens ficaram de fora e o motivo (não aprovados, fora de escopo ou adiados).
   - **Validação:** resultado dos testes, lint e build após as alterações.