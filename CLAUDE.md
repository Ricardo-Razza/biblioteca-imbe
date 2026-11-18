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
docker compose -f docker-compose.prod.yml up -d --build   # produção (precisa de .env com DOMINIO e de deploy/certs)
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
- Painel e relatórios (`relatorio`) leem por SQL direto (`JdbcClient`): em teste `@Transactional`, `em.flush()` antes de consultar. CSV sempre pela classe `Csv` (BOM, `;`, proteção contra fórmula); CPF não sai em relatório.
- Importação em lote (`importacao`): o `ProcessadorDeImportacao` roda numa thread própria depois do commit. Nos testes o commit não acontece: chame `processador.processarTudo(id)` direto e faça `em.flush()`/`em.clear()` antes de conferir.
- Produção: `docker-compose.prod.yml` + `deploy/`, ou a stack do Portainer (`docker-compose.portainer.yml`) com as imagens que o `.github/workflows/ci.yml` publica no ghcr.io. Variável nova do backend ou do web entra nos dois compose e no `deploy/portainer.env.example`. Nginx: modelos em `frontend/nginx/modelos` (`HTTPS=sim|nao`), escolhidos pelo `15-modo-https.sh`. Scripts, Dockerfiles e confs do Nginx ficam com LF (`.gitattributes`).
- Tarefas agendadas (expiração de reservas, feriados) ficam no `AgendamentoConfig`; nos testes a BrasilAPI de feriados aponta para a porta fechada.

## Frontend

- Angular 22, componentes standalone e signals, Angular Material com tema próprio, SCSS.
- Pastas `publico`, `gestao`, `core`, `shared` (ver README).
- Chamadas sempre para `/api/v1/...` (caminho relativo).
- Diretrizes visuais no README: sem emojis, Material Symbols, sem `alert()`/`confirm()`.
- **Sempre responsivo**, de 280 px a telas grandes, sem rolagem lateral da página. Em grid, colunas com `minmax(0, 1fr)` ou `minmax(min(150px, 100%), 1fr)` (só `1fr` não encolhe abaixo do texto mais longo); filho de flex/grid com texto cortado leva `min-width: 0`; linha de lista (capa, texto, selo, botões) é `flex-wrap: wrap` com o texto em `flex: 1 1 200px`, ou container query, nunca só `@media` (na gestão o menu come parte da largura); tabela larga dentro de um contêiner com `overflow-x: auto`. Conferir toda tela nova em 320, 375, 768 e 1280 px.
- Visual segue o Manual de Marca da Prefeitura: cores por `var(--marca-*)` ou pelos tokens do Material (nada de hex solto em componente), marca da Prefeitura sempre pelo `app-assinatura` ou pelos PNG de `public/marca/`, respeitando a largura mínima do manual.

## Documentação

- Toda decisão nova ou mudança de escopo atualiza o `README.md` (seção correspondente + linha em "Registro de decisões" com a data).
