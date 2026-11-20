---
name: code-review
description: Realiza code review rigoroso da implementação de uma atividade SDD comparando o código com PRD.md, TechSpec.md e Rules.md.
argument-hint: <id-da-atividade, ex: ofy-123>
---

Você deve realizar um **Code Review técnico e de conformidade SDD** para a atividade **$ARGUMENTS**.

## 1. Coleta de Contexto
1. Localize a pasta da atividade `$ARGUMENTS` dentro de `docs/sdd/atividades/` (`docs/sdd/atividades/$ARGUMENTS` ou `docs/sdd/atividades/$ARGUMENTS-*`).
2. Leia obrigatoriamente:
   - `@docs/sdd/Rules.md`
   - `<pasta-da-atividade>/PRD.md`
   - `<pasta-da-atividade>/TechSpec.md`
   - `<pasta-da-atividade>/referencias.md` (se existir)
3. Inspecione todas as alterações de código relacionadas à atividade usando `git status`, `git diff` (staged, unstaged e commits recentes da branch/atividade) e leitura direta dos arquivos modificados/criados listados no `TechSpec.md`.

## 2. Critérios de Avaliação
Avalie a implementação sob 5 eixos:
1. **Conformidade com o `PRD.md`:** Todos os critérios de aceite e regras de negócio foram atendidos? Há comportamento faltando ou escopo extra não solicitado?
2. **Conformidade com o `TechSpec.md`:** Os contratos, endpoints/comandos, assinaturas, estruturas de dados e fluxos seguem o desenho técnico? Se houve desvio, ele foi documentado na spec?
3. **Conformidade com `@docs/sdd/Rules.md`:** As fronteiras arquiteturais entre camadas foram respeitadas? Há acoplamento indevido, quebra de convenção ou alteração em arquivos protegidos?
4. **Qualidade, Segurança e Manutenibilidade:** Verifique tratamento de erros, edge cases, validação de entradas, legibilidade, duplicação de código e possíveis regressões.
5. **Cobertura e Execução de Testes:** Verifique se os testes cobrem os fluxos principais e casos de borda definidos na spec e rode a suíte de validação se necessário.

## 3. Formato do Relatório de Review
Não altere código de produção automaticamente durante o review — apenas aponte os achados de forma clara e acionável:

- **Veredito Geral:** `APROVADO` | `APROVADO COM RESSALVAS` | `REPROVADO (AJUSTES NECESSÁRIOS)`
- **Checklist de Critérios de Aceite (PRD):** Status (`[x]` / `[ ]`) de cada critério.
- **Achados Críticos (Bloqueantes):** Bugs, falhas de segurança, quebra de contrato ou violação grave de `Rules.md` (com `arquivo:linha`, explicação e sugestão de correção).
- **Achados Importantes (Não-bloqueantes):** Melhorias de design, legibilidade, edge cases ou cobertura de testes.
- **Sincronia de Documentação:** Indique se `PRD.md`, `TechSpec.md` ou docs gerais precisam de algum ajuste final antes do merge.