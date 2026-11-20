---
name: dev
description: Implementa o código de uma atividade SDD com base no PRD.md, TechSpec.md e Rules.md.
argument-hint: <id-da-atividade, ex: ofy-123>
---

Você deve implementar a atividade **$ARGUMENTS** seguindo rigorosamente a especificação aprovada e as regras arquiteturais do projeto.

## 1. Leitura Obrigatória Antes de Codar
1. Localize a pasta da atividade `$ARGUMENTS` dentro de `docs/sdd/atividades/` (`docs/sdd/atividades/$ARGUMENTS` ou `docs/sdd/atividades/$ARGUMENTS-*`).
2. Leia obrigatoriamente:
   - `@docs/sdd/Rules.md`
   - `<pasta-da-atividade>/PRD.md`
   - `<pasta-da-atividade>/TechSpec.md`
   - `<pasta-da-atividade>/referencias.md` (se existir)
   - `<pasta-da-atividade>/input/*` (apenas como contexto complementar, prevalecendo sempre o que foi consolidado no `PRD.md` e `TechSpec.md`).
3. Se `PRD.md` ou `TechSpec.md` não existirem na pasta da atividade, pare e instrua o usuário a rodar `/planning $ARGUMENTS` primeiro.

## 2. Regras de Implementação
- **Aderência total à spec:** Implemente todos os requisitos, contratos e critérios de aceite descritos no `PRD.md` e no `TechSpec.md` da atividade, sem adicionar escopo não especificado.
- **Respeito às fronteiras:** Siga à risca todas as convenções, limites entre camadas, padrões de código e restrições de `@docs/sdd/Rules.md`.
- **Testes:** Escreva ou atualize os testes unitários e/ou de integração previstos no `TechSpec.md` da atividade.
- **Desvios técnicos:** Se durante a implementação você descobrir uma restrição técnica que exija alterar um contrato ou decisão do `TechSpec.md`, documente o ajuste em `<pasta-da-atividade>/TechSpec.md` e `<pasta-da-atividade>/referencias.md` antes de finalizar.

## 3. Verificação Final
1. Execute os comandos de build, lint e testes definidos em `@docs/sdd/Rules.md` para garantir que nada quebrou.
2. Revise item a item os critérios de aceite de `<pasta-da-atividade>/PRD.md`.
3. Ao final, apresente um resumo objetivo dos arquivos criados/alterados, dos testes executados e do status de cada critério de aceite.