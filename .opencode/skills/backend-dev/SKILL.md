# Skill: Backend Mastery (Clean Architecture & SOLID)

## 1. O Contrato de Implementação (Obrigatório)
Sempre que uma tarefa for solicitada, antes de gerar o código, o agente deve confirmar mentalmente (ou reportar):
1. **Camada de Domínio (Pureza):** Existe lógica de banco de dados ou frameworks (Spring, Express, etc.) dentro das entidades? Se sim, **refatore**.
2. **SOLID Checklist:**
   - **SRP:** Esta classe/função tem apenas uma razão para mudar?
   - **OCP:** Se eu precisar adicionar um novo comportamento, terei que modificar código existente ou apenas estender via interface?
   - **LSP/ISP/DIP:** As dependências estão injetadas via interface (DIP)? As interfaces são específicas para o uso (ISP)?
3. **Clean Code:** Nomes de variáveis e métodos revelam intenção? (Evite: `data`, `res`, `tmp`; Use: `userRegistrationDate`, `httpResponse`, `temporaryCache`).

## 2. Padrão de Testes Unitários (TDD)
O código não é aceito se não vier acompanhado de testes.
- **Estrutura:** Obrigatório o uso do padrão **AAA (Arrange, Act, Assert)**.
- **Cobertura:** Teste o "Caminho Feliz" (Happy Path) e, no mínimo, 3 cenários de erro/exceção (ex: campos inválidos, falha de infraestrutura, estado de negócio proibido).
- **Isolamento:** Mocks devem ser usados para I/O. Não use bancos de dados reais.

## 3. Protocolo de Refatoração (O "Code Smell Check")
Antes de entregar, aplique a técnica de **Boy Scout Rule** (deixar o código mais limpo do que encontrou):
- **Identificar:** Existe duplicação de lógica (DRY)? Existe complexidade ciclomática alta (muitos `if` aninhados)?
- **Ação:** Se o código novo for adicionado em uma classe que já possui "cheiro de código" (code smell), a prioridade é refatorar a classe existente antes de implementar a nova funcionalidade.

## 4. Integração com Clean Architecture
- **Injeção de Dependência:** Nunca instancie dependências concretas (`new Service()`). Use injeção via construtor ou contêiner de inversão de controle.
- **Entrada/Saída:** Use DTOs para entrada e saída de dados. Nunca exponha entidades de banco de dados (`@Entity`) diretamente na camada de apresentação (API).