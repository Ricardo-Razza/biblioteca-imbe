# CLAUDE.md

Sistema da Biblioteca Municipal de Imbé. Decisões, modelo de dados, API e roadmap ficam no `README.md`. Leia antes de mudar algo.

## Comandos

```bash
cp .env.example .env                 # uma vez, preencher as senhas
docker compose up -d                 # MySQL 8.4
cd backend && ./mvnw spring-boot:run # API em http://localhost:8080
cd backend && ./mvnw verify          # testes (Testcontainers: precisa do Docker rodando)
cd frontend && ng serve              # http://localhost:4200, /api vai para o 8080
cd frontend && ng build
cd frontend && ng test --watch=false
```

## Git

- Trabalho na `develop`, com um commit ao fim de cada etapa.
- A `main` só recebe versões prontas, e só quando o usuário pedir.

## Backend (`br.gov.imbe.biblioteca`)

- Um pacote por módulo (`isbn`, `acervo`, `leitor`, ...), cada um com `controller → service → repository`.
- DTOs são `record`. Entidades JPA nunca saem na API.
- Erros: lançar `RequisicaoInvalidaException` (400), `NaoAutenticadoException` (401), `RecursoNaoEncontradoException` (404), `ConflitoException` (409), `RegraNegocioException` (422), `LimiteExcedidoException` (429) ou `IndisponivelException` (503), todas em `comum.erro`. O `TratadorGlobalDeErros` transforma em `ProblemDetail` com mensagem em português.
- Flyway em `src/main/resources/db/migration`, `V{n}__descricao.sql`. **Migration já aplicada não se edita**: crie uma nova.
- Hibernate roda com `ddl-auto: validate`. Toda mudança de esquema passa por migration.
- Enums no banco são `VARCHAR` com `CHECK`, mapeados com `@Enumerated(EnumType.STRING)` **e** `@JdbcTypeCode(SqlTypes.VARCHAR)`: sem isso o Hibernate espera `ENUM` nativo do MySQL e o `validate` falha.
- Colunas `CHAR`, `TEXT` e `SMALLINT`: use `columnDefinition = "CHAR(13)"`, `columnDefinition = "TEXT"` e `Short` na entidade, senão o `validate` reclama do tipo.
- `LIKE` em JPQL: escape com `!` (`like :padrao escape '!'`). Barra invertida é escape nos literais do MySQL e quebra o SQL.
- Listas paginadas saem como `comum.Pagina` (não o `Page` do Spring Data).
- Testes de integração: anotar com `@TesteIntegracao` (+ `@Transactional`). Ali as fontes de ISBN apontam para uma porta fechada (teste nunca acessa a internet) e as capas vão para `target/capas-teste`. Todas compartilham o mesmo contexto e o mesmo container MySQL; não crie variações de configuração sem necessidade, senão sobe outro container.
- Configuração por variável de ambiente (`DB_URL`, `JWT_SECRET`, ...). Nenhum segredo no repositório.
- Só o backend fala com APIs externas. Testes de API externa usam WireMock.
- ISBN: sempre via `Isbn.de(...)` (valida e converte para ISBN-13). Cada fonte é um `IsbnProvider`: devolve vazio quando não conhece o ISBN e lança `FonteIsbnIndisponivelException` quando falha. "Não encontrado" pode ir para o cache; falha, não.
- O Surefire define `jdk.net.unixdomain.tmpdir=target/`. Nos processos iniciados pelo app desktop do Claude Code, o Selector do NIO falha em qualquer pasta dentro de `AppData\Local`, inclusive o `%TEMP%` ("Unable to establish loopback connection"), e o WireMock não sobe. No terminal do usuário funciona. Não remover. Se precisar subir a API a partir do Claude, passe a mesma propriedade apontando para fora de `AppData\Local`.
- Os contadores de exemplares da `Obra` são `@Formula`: só se atualizam ao recarregar a entidade. Em teste `@Transactional`, faça `em.flush()` e `em.clear()` antes de conferir.
- Segurança: regras por rota no `SecurityConfig` (ADMIN inclui ATENDENTE). Quem está logado: `UsuarioAtual.id()`. Ação sensível nova registra em `Auditoria.registrar(...)`.
- Testes de integração rodam autenticados com `@WithMockUser(roles = "ADMIN")` na classe; para testar login, token e perfil de verdade, use `Funcionarios.local(...)` e `Funcionarios.token(...)`. `@WebMvcTest` com o `SecurityConfig` precisa de `@MockitoBean JwtDecoder` e `@MockitoBean ConversorJwt`.
- Nada com histórico é apagado (obra, exemplar, leitor, reserva): desativar ou dar baixa.
- Reserva: travar sempre o exemplar antes da reserva (`ReservaRepository` explica). O balcão fala com a reserva só pela interface `ReservasAtivas`.
- Testes do site público: use `Publico.ipNovo()` em cada teste (o limite por IP vive no contexto compartilhado) e CPFs novos de `Dados.cpf()`.
- Tarefas agendadas (expiração de reservas, feriados) ficam no `AgendamentoConfig`; nos testes a BrasilAPI de feriados aponta para a porta fechada.

## Frontend

- Angular 22, componentes standalone e signals, Angular Material com tema próprio, SCSS.
- Pastas `publico`, `gestao`, `core`, `shared` (ver README).
- Chamadas sempre para `/api/v1/...` (caminho relativo).
- Diretrizes visuais no README: sem emojis, Material Symbols, sem `alert()`/`confirm()`.

## Documentação

- Toda decisão nova ou mudança de escopo atualiza o `README.md` (seção correspondente + linha em "Registro de decisões" com a data).
