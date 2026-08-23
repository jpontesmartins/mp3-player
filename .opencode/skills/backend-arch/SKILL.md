---
name: backend-arch
description: Usar a arquitetura de backend (Java 21 + Spring Boot + Clean Architecture + Clean Code + DDD). Use em APIs REST Java quando o usuário quiser organização por módulos de negócio (Bounded Contexts) com camadas internas (domain/application/infrastructure/web), ports & adapters, testes unitários com JUnit/Mockito e logging com SLF4J.
---

# Skill: Backend Clean Architecture + Clean Code + DDD (Java 21 + Spring Boot)

Guia portátil de arquitetura de backend, hierarquia de pastas, padrões de testes e de logging para replicar em qualquer projeto.

## Objetivo
APIs REST Java bem estruturadas, testáveis e independentes de tecnologia. O código é organizado por **módulos de negócio** (Bounded Contexts), onde cada módulo encapsula suas próprias camadas arquiteturais (`domain`, `application`, `infrastructure`, `web`). O **domínio** é o núcleo isolado (sem dependências externas), o **application** orquestra casos de uso, a **infrastructure** implementa os ports e o **web** expõe HTTP. Toda decisão tecnológica fica na borda — nunca no núcleo.

## Tech stack

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 (virtual threads) |
| Framework | Spring Boot 3.3.x |
| Build | Maven |
| Testes | JUnit 5, Mockito, spring-boot-starter-test |
| Logging | SLF4J API + Logback (bind padrão do Spring Boot) |
| Web | spring-boot-starter-web (REST) |

## 1. Regra de dependência (a regra de ouro)

A dependência sempre aponta **para dentro** — tanto dentro de cada módulo quanto entre módulos:

```
Dentro de cada módulo:
  web → application → domain ← infrastructure
                      ↑

Entre módulos:
  moduloB → moduloA (apenas via ports de domain)
```

**Dentro do módulo:**
- `domain/` **não importa** nada de Spring, bibliotecas ou HTTP.
- `application/` importa apenas `domain` e Java puro.
- `infrastructure/` implementa as interfaces de `domain` (ports e repositories).
- `web/` traduz HTTP → `application`; não contém regra de negócio.

**Entre módulos:**
- Um módulo pode depender de outro, mas **apenas via interfaces de `domain`** (ports).
- Nunca importar classes de `infrastructure` ou `web` de outro módulo.

Trocar biblioteca, framework ou armazenamento (arquivo → banco) **não toca** o núcleo: basta criar uma nova implementação de port/repository dentro do mesmo módulo.

## 2. Hierarquia de pastas

O código é organizado por **módulos de negócio** (Bounded Contexts). Cada módulo encapsula suas próprias camadas arquiteturais.

```
src/main/java/com.<projeto>
├── Application.java               # @SpringBootApplication
├── shared/                        # COMPARTILHADO entre módulos
│   ├── domain/model/              #   modelos usados por vários módulos
│   ├── domain/util/               #   helpers estáticos
│   └── config/                    #   beans de infra compartilhados (CorsConfig, etc.)
├── config/                        # WIRING de beans entre módulos
│   └── <Modulo>Config.java        #   @Configuration que injeta adapters nos services
│
├── <modulo1>/                     # BOUNDED CONTEXT 1
│   ├── domain/                    #   NÚCLEO — regras de negócio, zero dependências externas
│   │   ├── model/                 #     entidades e value objects (invariantes próprias)
│   │   ├── port/                  #     contratos tecnológicos (interfaces)
│   │   └── repository/            #     portas de persistência
│   ├── application/               #   CASOS DE USO — orquestra ports + modelos
│   │   └── <Modulo>Service.java   #     um @Service por módulo
│   ├── infrastructure/            #   IMPLEMENTAÇÕES dos ports
│   │   └── <Tecnologia><Port>     #     um adaptador por implementação
│   └── web/                       #   ADAPTADORES HTTP — Controllers
│       └── <Modulo>Controller.java
│
├── <modulo2>/                     # BOUNDED CONTEXT 2
│   └── ...                        #   mesma estrutura interna
│
└── <moduloN>/                     # BOUNDED CONTEXT N
    └── ...

src/test/java/com.<projeto>        # espelha a hierarquia de main/
├── shared/domain/model/…Test      #   testes de modelos compartilhados
├── <modulo1>/                     #   espelha a estrutura do módulo
│   ├── domain/model/…Test         #     regras puras de domínio
│   ├── application/…Test          #     casos de uso com ports mockados (Mockito)
│   ├── infrastructure/…Test       #     adapters com @TempDir / recursos reais
│   └── web/…Test                  #     controllers com MockMvc
├── <modulo2>/…
└── ApplicationTests               #   smoke test: contexto Spring carrega
```

Convenções:
- Cada funcionalidade pertence a **um único módulo**: lógica não distribuída entre pastas.
- Um `@Service` por módulo em `application/`, com sufixo `Service`.
- Uma interface por port em `domain/port/` e `domain/repository/`.
- Implementações em `infrastructure/` com nome descritivo da tecnologia (`File`, `Http`, `Jpa`, `Aws`), seguido do port.
- Controllers **finos**: traduzem HTTP → service, sem lógica de negócio.
- `shared/` contém apenas o que é genuinamente usado por 2+ módulos; evite colocar coisas aqui prematuramente.

## 3. Clean Code — convenções obrigatórias

- **Nomes que falam**: verbos de ação (`createOrder`, `cancelOrder`, `findById`, `scanFolder`). Nunca `doIt`, `handle`, `process`.
- **Métodos pequenos e com uma responsabilidade**; extrair privados com nome explicativo (`fileFor`, `normalizeInput`).
- **Imutabilidade no domínio**: entidades `final`, coleções expostas via `List.copyOf`/`Collections.unmodifiableList`; operações retornam nova instância (`addItem` retorna um novo objeto).
- **Defesa contra null**: `Objects.requireNonNull`, `Objects.requireNonNullElse`, helpers `blank()`/`trim()`.
- **Não repetir**: cada conceito em um só lugar; extrair helpers estáticos puros quando fizerem sentido.
- **Exceções**: `IllegalArgumentException` para input inválido, `IllegalStateException` para falha de persistência; propagar exceções checadas na assinatura (nada de `throws Exception`).
- **Javadoc curto** em classes públicas e contratos explicando o *porquê*; comentários "TODO" só quando realmente pendente.
- Sem dependências cíclicas entre pacotes de `application/`; cada módulo usa apenas os ports de `domain`.

## 4. Logging (SLF4J)

Padrão usado em **todos** os services, controllers e infraestrutura:

```java
private static final Logger log = LoggerFactory.getLogger(UserController.class);
```

Regras:
- **Controllers** logam a entrada/saída do endpoint: `log.info("POST /users: {}", email)`, e erros com `log.error("Create user failed: {}", e.getMessage())`.
- **Infrastructure** loga persistência: `log.info("Order saved: {} ({} items)", id, size)`; falhas de I/O em `log.error("Error listing orders", e)`.
- **Application services** usam `log.info` para marcos e `log.warn` para caminhos ignorados (ex.: ação rejeitada por estado inválido).
- Sempre usar **placeholder `{}`** — nunca concatenação.
- Em erros com stack trace, passar a exceção como último argumento (`log.error("msg {}", arg, e)`).
- Config em `src/main/resources/application.properties`: `server.port` e demais props; nível de log por package se precisar (`logging.level.com.<projeto>=DEBUG`).

## 5. Testes unitários (JUnit 5 + Mockito)

Espelhe a hierarquia de `main/` em `src/test/java`, organizado por módulos. Dois estilos:

### Domínio — puro, sem mocks
Testa identidade, invariantes e regras de valor:
```java
// src/test/java/com.<projeto>/<modulo>/domain/model/OrderTest.java
class OrderTest {
    @Test
    void addItemAvoidsDuplicates() { ... }
}
```

### Application — ports mockados com `@ExtendWith(MockitoExtension.class)`
```java
// src/test/java/com.<projeto>/<modulo>/application/OrderServiceTest.java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository repository;

    @Test
    void cancelDoesNothingWhenNotPending() {
        when(repository.findById(1L)).thenReturn(...);
        OrderService service = new OrderService(repository);
        assertThrows(IllegalStateException.class, () -> service.cancel(1L));
        verify(repository, never()).save(any());
    }
}
```
- Instancie o service **manualmente** (`new OrderService(repository)`) — sem subir o Spring.
- Use `when(...).thenReturn(...)` para definir comportamento e `verify(...)` para garantir interações.
- Cobre: caminho feliz, caso vazio/null, comportamento "ignorado" e bordas (lista vazia, wraparound).

### Infrastructure — recursos reais temporários
```java
// src/test/java/com.<projeto>/<modulo>/infrastructure/FileOrderRepositoryTest.java
@BeforeEach
void setUp() throws IOException {
    dir = Files.createTempDirectory("repo-test");
    repository = new FileOrderRepository(dir);
}
```
- Use `Files.createTempDirectory` ou `@TempDir` para I/O de arquivos; nunca grave em diretórios reais.
- Teste ida-e-volta: criar → carregar → listar → renomear → excluir.

### Web — controllers com MockMvc
```java
// src/test/java/com.<projeto>/<modulo>/web/OrderControllerTest.java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void getReturnsOrder() throws Exception {
        mockMvc.perform(get("/orders/1"))
               .andExpect(status().isOk());
    }
}
```

### Integração — smoke test
```java
@SpringBootTest
class ApplicationTests {
    @Test void contextLoads() { }
}
```

Rodar: `mvn test`. Critério de aceite: **100% de testes verdes** antes de considerar pronto.

## 6. Checklist ao criar um novo projeto com esta arquitetura

1. Criar projeto Spring Boot (Java 21 + Maven), dependências: `starter-web`, `starter-test`, `junit-jupiter`, `mockito-core`.
2. Criar pasta `shared/` para modelos e helpers usados por vários módulos.
3. Criar o primeiro **módulo de negócio** (Bounded Context) com a estrutura `domain/ → application/ → infrastructure/ → web/`.
4. Dentro do módulo, definir os **modelos de domínio** primeiro (imutáveis, com invariantes).
5. Declarar **ports** (`domain/port/`, `domain/repository/`) — são os contratos que o domínio exige.
6. Implementar os **casos de uso** em `application/` usando apenas ports; um `@Service` por módulo.
7. Criar as **implementações** em `infrastructure/` para cada port (biblioteca/arquivo/banco).
8. Criar os **Controllers** finos em `web/`, delegando aos services.
9. Criar módulos adicionais seguindo o mesmo padrão; dependências entre módulos via ports de `domain`.
10. Adicionar `config/` para wiring de beans entre módulos (ex.: injetar adapters de um módulo em services de outro).
11. Adicionar logging SLF4J em services, controllers e infra (seção 4).
12. Escrever testes unitários espelhando a hierarquia por módulo (seção 5) e rodar `mvn test`.

## Referência de contexto

Codebase de referência: `backend/` (especialmente `README.md` com o mapa da arquitetura, `src/main/java` e `src/test/java`) — consultar para copiar padrões estruturais ao replicar.
