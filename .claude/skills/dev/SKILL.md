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

## 2. Criação da Branch
Antes de alterar qualquer arquivo, crie uma nova branch para a implementação:
1. Defina o **tipo** a partir do `PRD.md`/`TechSpec.md`:
   - `feature` — nova funcionalidade ou comportamento;
   - `fix` — correção de bug;
   - `refactor` — mudança interna sem alterar comportamento.
2. Monte o nome no formato `<tipo>/<id>-<funcionalidade>`, ex.: `feature/ofy-001-player-basico`.
   - `<id>` é o id da atividade em minúsculas (ex.: `ofy-001`).
   - `<funcionalidade>` é um slug curto em kebab-case (minúsculas, sem acentos), preferencialmente o sufixo do nome da pasta da atividade (`docs/sdd/atividades/ofy-001-player-basico` → `player-basico`).
3. Verifique que a working tree está limpa (`git status`). Se houver alterações pendentes não relacionadas, pare e pergunte ao usuário como proceder.
4. Atualize a branch principal e crie a nova branch a partir dela:
   ```
   git checkout main
   git pull
   git checkout -b <tipo>/<id>-<funcionalidade>
   ```
5. Se a branch já existir, apenas faça `git checkout` nela e continue a implementação.

## 3. Regras de Implementação
- **Aderência total à spec:** Implemente todos os requisitos, contratos e critérios de aceite descritos no `PRD.md` e no `TechSpec.md` da atividade, sem adicionar escopo não especificado.
- **Respeito às fronteiras:** Siga à risca todas as convenções, limites entre camadas, padrões de código e restrições de `@docs/sdd/Rules.md`.
- **Testes:** Escreva ou atualize os testes unitários e/ou de integração previstos no `TechSpec.md` da atividade.
- **Desvios técnicos:** Se durante a implementação você descobrir uma restrição técnica que exija alterar um contrato ou decisão do `TechSpec.md`, documente o ajuste em `<pasta-da-atividade>/TechSpec.md` e `<pasta-da-atividade>/referencias.md` antes de finalizar.

## 4. Verificação Final
1. Execute os comandos de build, lint e testes definidos em `@docs/sdd/Rules.md` para garantir que nada quebrou.
2. Revise item a item os critérios de aceite de `<pasta-da-atividade>/PRD.md`.
3. Ao final, apresente o nome da branch criada e um resumo objetivo dos arquivos criados/alterados, dos testes executados e do status de cada critério de aceite.