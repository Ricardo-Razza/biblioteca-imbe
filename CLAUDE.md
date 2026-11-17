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
- Erros: lançar `RequisicaoInvalidaException` (400), `RecursoNaoEncontradoException` (404), `ConflitoException` (409) ou `RegraNegocioException` (422), todas em `comum.erro`. O `TratadorGlobalDeErros` transforma em `ProblemDetail` com mensagem em português.
- Flyway em `src/main/resources/db/migration`, `V{n}__descricao.sql`. **Migration já aplicada não se edita**: crie uma nova.
- Hibernate roda com `ddl-auto: validate`. Toda mudança de esquema passa por migration.
- Enums no banco são `VARCHAR` com `CHECK`, mapeados com `@Enumerated(EnumType.STRING)`.
- Configuração por variável de ambiente (`DB_URL`, `JWT_SECRET`, ...). Nenhum segredo no repositório.
- Só o backend fala com APIs externas. Testes de API externa usam WireMock.
- ISBN: sempre via `Isbn.de(...)` (valida e converte para ISBN-13). Cada fonte é um `IsbnProvider`: devolve vazio quando não conhece o ISBN e lança `FonteIsbnIndisponivelException` quando falha. "Não encontrado" pode ir para o cache; falha, não.
- O Surefire define `jdk.net.unixdomain.tmpdir=target/`: em algumas máquinas Windows o Selector do NIO falha no `%TEMP%` ("Unable to establish loopback connection"). Não remover.
- Nada com histórico é apagado (obra, exemplar, leitor): desativar ou dar baixa.

## Frontend

- Angular 22, componentes standalone e signals, Angular Material com tema próprio, SCSS.
- Pastas `publico`, `gestao`, `core`, `shared` (ver README).
- Chamadas sempre para `/api/v1/...` (caminho relativo).
- Diretrizes visuais no README: sem emojis, Material Symbols, sem `alert()`/`confirm()`.

## Documentação

- Toda decisão nova ou mudança de escopo atualiza o `README.md` (seção correspondente + linha em "Registro de decisões" com a data).
