# Project Rules & Architectural Guidelines

> **Instrução para o Assistente de IA:** Preencha todas as seções abaixo analisando a estrutura real, configurações, dependências e código-fonte deste repositório. Substitua todos os marcadores `[...]` por regras concretas e específicas do projeto. Remova este bloco de instrução ao gerar o `Rules.md` final.

---

## 1. Visão Geral da Stack e Módulos
- **Linguagens e Runtimes:** `[Listar linguagens, versões e ambientes identificados no projeto]`
- **Frameworks e Bibliotecas Principais:** `[Listar frameworks de interface, backend, camada nativa/desktop, build tools, etc.]`
- **Mapa de Diretórios:**
  - `[caminho/pasta-1]`: `[Responsabilidade da camada/módulo]`
  - `[caminho/pasta-2]`: `[Responsabilidade da camada/módulo]`
  - `[caminho/pasta-3]`: `[Responsabilidade da camada/módulo]`

## 2. Fronteiras Arquiteturais e Comunicação entre Camadas
- **Fluxo de Dependência:** `[Descrever qual camada pode chamar qual e em que direção os dados trafegam]`
- **Mecanismos de Integração:** `[Descrever como os módulos se comunicam: APIs, IPC, mensageria, CLI, eventos, etc.]`
- **Isolamento de Responsabilidades:**
  - `[Onde deve residir exclusivamente a lógica de negócio/domínio]`
  - `[Onde deve residir exclusivamente a lógica de apresentação/interface]`
  - `[Onde devem residir acessos a recursos externos, sistema operacional ou persistência]`
- **Proibições Arquiteturais:** `[Listar acoplamentos proibidos, ex.: camada X nunca deve acessar diretamente Y sem passar por Z]`

## 3. Padrões de Código e Convenções
- **Estilo e Nomenclatura:** `[Convenções de nomes de arquivos, classes, funções, variáveis e pacotes identificadas no código]`
- **Padrões Estruturais:** `[Padrões adotados por módulo, ex.: arquitetura em camadas, clean architecture, composição de componentes, imutabilidade, etc.]`
- **Tratamento de Erros e Logs:** `[Como exceções, erros de integração e logs são estruturados e propagados]`
- **Gerenciamento de Estado e Configuração:** `[Onde ficam variáveis de ambiente, arquivos de configuração e estado da aplicação]`

## 4. Testes e Qualidade
- **Estratégia de Testes:** `[Tipos de testes existentes (unitários, integração, E2E) e onde ficam localizados]`
- **Critérios Mínimos:** `[O que precisa ser testado antes de considerar uma alteração concluída]`
- **Build e Verificação:** `[Comandos ou etapas obrigatórias para validar compilação, lint e testes]`

## 5. Governança de Spec-Driven Development (SDD)
- **Fonte da Verdade:** Nenhum código de funcionalidade nova ou alteração estrutural deve ser escrito antes de atualizar `docs/sdd/PRD.md` (requisitos e critérios de aceite) e `docs/sdd/TechSpec.md` (contratos, fluxos e impacto técnico).
- **Fidelidade aos Templates:** Toda criação ou atualização de documentação em `docs/sdd/` deve seguir rigorosamente a estrutura definida em `docs/sdd/templates/`.
- **Proteção dos Modelos:** Nunca modificar ou sobrescrever os arquivos originais dentro de `docs/sdd/templates/` durante a geração ou evolução das specs.
- **Sincronia Código-Spec:** Caso uma decisão técnica mude durante a implementação, atualizar `TechSpec.md` (e `Rules.md`, se for uma nova convenção permanente) antes de finalizar a tarefa.

## 6. Restrições Críticas e Zonas Sensíveis
- **Arquivos e Contratos Protegidos:** `[Listar arquivos de configuração crítica, portas, schemas, manifestos ou contratos públicos que não podem ser alterados sem revisão explícita]`
- **Diretórios Ignorados / Gerados:** `[Listar pastas de build, dependências ou artefatos compilados que não devem ser editados manualmente nem usados como referência de arquitetura]`
- **Segurança e Permissões:** `[Regras sobre credenciais, exposição de endpoints, permissões de sistema ou validação de entrada]`