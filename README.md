# Biblioteca Municipal de Imbé

Sistema da Biblioteca Municipal da Secretaria Municipal de Cultura de Imbé, em `biblioteca.imbe.rs.gov.br`, com duas modalidades:

- **Site público (`/`):** o cidadão consulta o acervo, com capas e quantidade disponível, e reserva um livro para retirar no balcão.
- **Gestão (`/login` → `/painel`):** a equipe cuida do acervo, leitores, empréstimos, devoluções e reservas. O livro é cadastrado só pelo ISBN: o sistema busca os dados e a capa em APIs externas.

> Este README registra as decisões do projeto e é atualizado a cada decisão nova.
> Documento completo de arquitetura: `docs/arquitetura.md` · Diagramas: `docs/biblioteca-imbe.excalidraw`

---

## Stack

| Camada | Tecnologia |
| --- | --- |
| Backend | Java 25, Spring Boot 4, Maven |
| Dependências do backend | Spring Web, Data JPA, Security, OAuth2 Resource Server (JWT), Validation, Flyway, Cache + Caffeine, Actuator, springdoc-openapi |
| Frontend | Angular 22 (componentes standalone, signals), Angular Material com tema próprio, SCSS |
| Banco | MySQL 8 |
| Testes | JUnit 5, Testcontainers (MySQL), WireMock (APIs externas) |
| Infra | Docker Compose: Nginx + API + MySQL, num servidor da Prefeitura |

## Estrutura do repositório

```text
biblioteca-imbe/
├── backend/              Spring Boot (br.gov.imbe.biblioteca)
├── frontend/             Angular 22
├── docs/
│   ├── arquitetura.md
│   ├── biblioteca-imbe.excalidraw
│   └── referencia/       contrato da API de login AD, código de referência
├── deploy/             backup.sh, restaurar.sh, certs/ (certificado HTTPS, fora do git)
├── docker-compose.yml    MySQL local (lê o .env)
├── docker-compose.prod.yml  produção: MySQL + API + Nginx com HTTPS
├── .env.example          modelo do .env (o .env não vai para o git)
├── CLAUDE.md
└── README.md
```

### Pacotes do backend

```text
br.gov.imbe.biblioteca
├── isbn          providers (BrasilAPI, Google Books, Open Library), IsbnLookupService, cache
├── acervo        Obra, Exemplar, Autor, Assunto, capas
├── leitor
├── emprestimo    regras, renovação, devolução
├── reserva       reservas, expiração agendada, dias úteis e feriados
├── publico       endpoints do site público (catálogo e reserva)
├── relatorio     painel e exportações CSV
├── importacao    carga do acervo por planilha de ISBNs
├── seguranca     login AD/local, JWT, funcionário, auditoria, limite de tentativas
└── comum         erros (ProblemDetail), configuração, parâmetros
```

Cada módulo: `controller → service → repository`. DTOs são `record`. Entidades nunca saem na API.

### Pastas do frontend

Um único app com duas áreas, cada uma com layout próprio e carregada sob demanda:

- `publico`: `catalogo`, `livro`, `reservar`, `minhas-reservas`
- `gestao`: `inicio`, `acervo`, `leitores`, `emprestimos`, `reservas`, `relatorios`, `configuracoes` (uma pasta por item do menu)
- `core`: interceptor do token, guarda que manda `/painel` para `/login` sem token, guarda de perfil
- `shared`: `campo-isbn`, `capa-livro`, `selo-situacao`, `busca-leitor`

### Rotas do front

| Rota | Quem | Tela |
| --- | --- | --- |
| `/` | Cidadão | Catálogo: busca por título, autor ou assunto, grade de capas, disponíveis, filtro "só disponíveis" |
| `/livro/{id}` | Cidadão | Ficha do livro e botão Reservar |
| `/reservar/{id}` | Cidadão | CPF + data de nascimento → protocolo |
| `/minhas-reservas` | Cidadão | Consulta e cancelamento |
| `/login` | Equipe | Login AD ou local |
| `/painel/...` | Equipe | Gestão. Sem token, vai para `/login` |

Na gestão, o acervo fica em `/painel/acervo` (lista e busca), `/painel/acervo/novo` (cadastro por ISBN), `/painel/acervo/{id}` (ficha com capa e exemplares) e `/painel/acervo/{id}/editar`. Leitores em `/painel/leitores` (busca por nome ou CPF), `/painel/leitores/novo`, `/painel/leitores/{id}` (ficha com situação, livros em uso e histórico) e `/painel/leitores/{id}/editar`. O balcão tem `/painel/emprestar` (escolhe o leitor, `?leitor=` vindo da ficha, e bipa os tombos em sequência) e `/painel/devolver` (bipa o tombo, Enter confere e Enter confirma; `?tombo=` vindo das listas), sempre à mão no topo da gestão. `/painel/emprestimos` lista em abas Ativos, Atrasados e Histórico, com filtro e renovação. `/painel/configuracoes` (só administrador) tem as abas Funcionários, Prazos e limites, Feriados e Auditoria; `/painel/senha` troca a própria senha (obrigatória com senha provisória: até trocar, a gestão só abre essa tela). Ações de administrador (mudar exemplar, desbloquear leitor) não aparecem para o atendente. `/painel/reservas` tem as abas Para separar (por prazo, com tombo e localização; Entregar e Cancelar) e Histórico. `/painel` abre em `/painel/inicio` (indicadores do dia, listas e gráfico da semana) e `/painel/relatorios` baixa os CSVs.

No site público, a busca e os filtros ficam na URL (`/?q=&assunto=&disponiveis=1&pagina=`). CPF e data de nascimento ficam só na memória da tela, nunca na URL nem no armazenamento do navegador.

O Nginx devolve o `index.html` para qualquer rota que não seja arquivo nem `/api`, para os links funcionarem ao recarregar a página.

---

## Arquitetura

Monolito modular. **Só o backend fala com as APIs externas.** O front nunca vê chaves nem tokens.

```mermaid
flowchart LR
    CID[Cidadão<br/>celular ou PC] --> F[Front Angular 22<br/>site público: /<br/>gestão: /painel]
    U[Equipe<br/>navegador + leitor USB] --> F
    F -->|HTTPS| N[Nginx]
    N -->|/api| B
    subgraph B[API Spring Boot 4 · Java 25]
        M1[Busca por ISBN]
        M2[Obras e exemplares]
        M3[Leitores]
        M4[Empréstimos e regras]
        M7[Reservas e expiração]
        M8[Catálogo público]
        M5[Relatórios e painel]
        M6[Login AD/local e JWT]
    end
    M1 --> X1[BrasilAPI]
    M1 --> X2[Google Books]
    M1 --> X3[Open Library]
    M6 --> X4[API de login AD]
    B --> DB[(MySQL 8)]
    B --> C[(Capas<br/>pasta no disco)]
```

---

## Autenticação

O funcionário tem dois meios de login, no mesmo padrão do Contract Management. O tipo de cada um fica no campo `tipo_autenticacao`.

| Tipo | Como valida | Senha no nosso banco |
| --- | --- | --- |
| `AD` | Chamada HTTP à API interna de login da Prefeitura: URL + `Authorization: Bearer` | Não |
| `LOCAL` | BCrypt contra `senha_hash` | Sim |

**Não implementamos LDAP.** O sistema só consome a API de login já existente.

Fluxo:

1. O front envia `POST /api/v1/auth/login` com `{ login, senha }`.
2. O backend busca o funcionário pelo login. Se não existe ou está inativo, recusa.
3. Se for `AD`, chama a API de login (`AD_AUTH_URL`, `AD_AUTH_TOKEN`) com timeout de 4 s. Se for `LOCAL`, compara com BCrypt.
4. Com sucesso, emite o JWT do próprio sistema (validade de 8 h) com o perfil **vindo do nosso banco** (`ADMIN` ou `ATENDENTE`).

Regras:
- O Bearer da API de login fica só no servidor (variável de ambiente) e nunca vai para o front.
- Se a API do AD estiver fora do ar, o login mostra o motivo, e um administrador `LOCAL` continua entrando.
- Usuário `LOCAL` troca a senha no primeiro acesso. Não existe login compartilhado.
- O contrato da API (endpoint, campos, respostas de erro, certificado, validade do Bearer) fica em `docs/referencia/api-auth-ad.md`. **Ainda pendente.**

Como ficou implementado (fase 3):

- **Contrato provisório do AD** (`ValidadorAd`), até a Prefeitura documentar o real: `POST AD_AUTH_URL` com `Authorization: Bearer AD_AUTH_TOKEN` e `{"usuario", "senha"}`. 2xx aceita; 400, 401, 403 e 404 recusam; qualquer outra resposta ou 4 s sem resposta é "fora do ar" (503 com mensagem). Trocar o contrato mexe só nessa classe.
- **JWT** HS256 com `JWT_SECRET` (mínimo 32 caracteres, senão a API não sobe), válido por 8 h. Leva só o id do funcionário (`fid`): perfil, ativo e troca de senha são lidos do banco a cada requisição, então desativar ou mudar o perfil vale na hora.
- **Perfis:** `ADMIN` inclui tudo do `ATENDENTE`. Só admin: `PATCH /exemplares`, desbloqueio de leitor, `PUT /parametros`, incluir e remover feriado, `/funcionarios` e `/auditoria`.
- **Senha local:** BCrypt; 8 a 72 caracteres, com letra e número, diferente do login. Cadastro e "nova senha provisória" geram uma senha de 10 caracteres mostrada uma única vez ao administrador; com ela o funcionário só consegue trocar a senha.
- **Tentativas:** 5 senhas erradas seguidas travam o login por 15 minutos (acertar zera a conta). Login inexistente, desativado ou senha errada têm a mesma mensagem.
- **Primeiro acesso:** com o banco sem funcionários, a API cria o administrador local `admin` com a senha provisória de `ADMIN_INICIAL_SENHA`.
- **Auditoria** (tabela `auditoria`): logins (aceitos, recusados e travados), troca e redefinição de senha, cadastro e alteração de funcionário, parâmetros, feriados, mudança de situação do exemplar e desbloqueio de leitor. O empréstimo guarda quem registrou a saída e a devolução.
- **Front:** token no `sessionStorage` (fechar o navegador do balcão encerra a sessão); 401 em qualquer chamada leva ao login guardando a tela de volta.

---

## Site público e reservas

Decisões de 2026-11-17:

| Pergunta | Decisão |
| --- | --- |
| Como o cidadão se identifica? | CPF + data de nascimento. Só leitor já cadastrado reserva; leitor novo se cadastra no balcão |
| Pode reservar livro sem exemplar? | Não. Só com exemplar disponível; fila de espera fica para depois |
| Prazo para retirar | 2 dias úteis (sem sábados, domingos e feriados) |
| Como acompanha? | Protocolo na tela + consulta e cancelamento em "Minhas reservas". Sem e-mail |

Fluxo:

1. O cidadão encontra o livro e clica em Reservar (o botão só aparece com exemplar disponível).
2. Informa nome completo, CPF, data de nascimento, telefone com DDD (obrigatório) e e-mail (opcional). O contato fica gravado na reserva, para a equipe avisar o cidadão; a tela Reservas mostra esse contato e, sem ele (reservas antigas), o do cadastro do leitor.
3. O backend confere o leitor, os limites e a disponibilidade, e **prende um exemplar** (situação `RESERVADO`) na mesma transação, com `SELECT ... FOR UPDATE SKIP LOCKED`. Assim dois cidadãos não reservam a última cópia.
4. A tela mostra o protocolo, a data limite e o endereço e horário da biblioteca.
5. A equipe vê a reserva em Reservas e no Início, com tombo e localização, e separa o livro.
6. Na retirada, a reserva vira empréstimo com o mesmo exemplar. Se o cidadão não retirar no prazo, um job agendado marca `EXPIRADA` e o exemplar volta a `DISPONIVEL`.

Proteções do site público:
- Só expõe dados da obra e a quantidade disponível. Nunca leitor, empréstimo ou exemplar individual.
- CPF e data de nascimento vão no corpo do POST, nunca na URL, e não entram em log.
- Resposta genérica quando os dados não conferem ("CPF ou data de nascimento não conferem").
- O nome é pedido sempre, tenha ou não cadastro, para a resposta não revelar se um CPF já é leitor.
- Limite de tentativas por IP e por CPF. CAPTCHA a decidir.
- A confirmação mostra só o primeiro nome do leitor.

Como foi implementado (fase 4):

- **Catálogo:** só aparece obra com algum exemplar disponível, reservado ou emprestado (sem exemplar, ou só com extraviados e baixados, fica de fora). A busca procura no título, subtítulo, autores e assuntos, sem diferenciar maiúsculas nem acentos. "Todos emprestados" quando não há exemplar disponível.
- **Reserva sem cadastro (pré-cadastro):** o cidadão não precisa ser leitor para reservar. Se o CPF ainda não existe (e é válido), o leitor é criado com nome, CPF, nascimento, telefone e e-mail informados e `cadastro_pendente = true` (V7). Se o CPF já existe, a data de nascimento precisa conferir. O pré-cadastro reserva normalmente, mas não leva livro enquanto pendente: na tela Reservas aparece "Cadastro pendente" e o botão vira "Conferir cadastro"; a equipe confere o documento com foto, completa endereço e responsável (se menor) e confirma, o que tira a pendência e grava `LEITOR_CADASTRO_CONFERIDO` na auditoria. Risco aceito: alguém pode reservar com o CPF válido de outra pessoa que ainda não é leitora; o livro só sai com documento e a reserva expira no prazo.
- **Reserva:** mesmas regras do balcão (desativado, bloqueado, atraso, limite contando as reservas), com mensagens para o próprio leitor. Cadastro incompleto (pendente ou menor sem responsável) não impede reservar, só retirar. Uma reserva aguardando por leitor e obra. O prazo conta `PRAZO_RETIRADA_RESERVA_DIAS_UTEIS` a partir de hoje (reserva na segunda: retira até quarta). O banco garante uma reserva aguardando por exemplar e por leitor e obra (colunas geradas + índice único).
- **Protocolo:** 8 caracteres sem os que se confundem (0/O, 1/I/L), mostrado como `ABCD-2345`; aceito com ou sem hífen e em minúsculas.
- **Retirada:** Entregar, na tela Reservas, ou bipar o tombo reservado no Emprestar para o próprio leitor: vira empréstimo com o mesmo exemplar. O limite não impede a retirada (a reserva já contava); bloqueio, atraso e desativação impedem. Exemplar reservado para outro leitor não sai no balcão. Com o prazo vencido e a reserva ainda não expirada, ainda dá para entregar.
- **Expiração:** job de hora em hora (e ao subir a API) marca `EXPIRADA` as reservas com o prazo já passado e devolve o exemplar a `DISPONIVEL`.
- **Cancelamento:** pelo leitor em "Minhas reservas" (protocolo de outro leitor responde como inexistente) ou pela equipe (fica na auditoria).
- **Travas:** sempre exemplar antes de reserva, em todos os fluxos, para não haver impasse entre balcão, reserva e job.
- **Limites:** por IP, 300 consultas por minuto e 20 identificações (reservar, consultar, cancelar) a cada 10 minutos; por CPF, 10 identificações a cada 10 minutos, de qualquer IP. Passou: 429 "Muitas tentativas". Contadores em memória (Caffeine), suficientes para uma instância da API; o IP vem do Nginx (`server.forward-headers-strategy: native`).
- **Feriados nacionais:** importados da BrasilAPI (`/api/feriados/v1/{ano}`) para o ano atual e o próximo, ao subir a API e todo dia às 3h10, só quando o ano ainda não tem nenhum. O administrador também pode buscar na aba Feriados. Data já cadastrada como municipal não é sobrescrita.
- **Rodapé e confirmação:** nome, endereço, horário, telefone e e-mail vêm de `BIBLIOTECA_ENDERECO`, `BIBLIOTECA_HORARIO`, `BIBLIOTECA_TELEFONE` e `BIBLIOTECA_EMAIL` (vazios não aparecem; sem nenhum deles, o rodapé diz "em breve"). Prazo de retirada, limite de livros e prazo do empréstimo vêm dos parâmetros.
- **Rodapé do site:** marca da Prefeitura, "Visite a biblioteca" (contato, com telefone e e-mail clicáveis), "Como reservar" em 3 passos com os prazos configurados e "Acesso rápido" (catálogo, minhas reservas, site da Prefeitura e área da equipe).

---

## Painel e relatórios

**Início (`/painel/inicio`)**, a tela que abre ao entrar na gestão:
- Cartões: livros emprestados (em aberto, atrasados inclusive), vencem hoje, atrasados, reservas para separar e leitores bloqueados. Atrasados e reservas levam à lista correspondente; cartão com pendência ganha a cor de alerta ou erro, sempre com o número e o texto.
- Listas de até 10 itens: reservas para separar (tombo e localização, prazo de retirada), vencem hoje e atrasados (com telefone e dias de atraso).
- Gráfico de barras agrupadas com saídas e devoluções dos últimos 7 dias: uma escala só, legenda com o total da semana, detalhe do dia ao passar o mouse ou focar com o teclado e tabela equivalente para leitor de tela. Cores das duas séries conferidas para daltonismo.

**Relatórios (`/painel/relatorios`)**, em CSV:

| Relatório | Período | Colunas |
| --- | --- | --- |
| Empréstimos | saída no período | saída, devolução prevista, devolvido em, situação, renovações, ocorrência, tombo, título, leitor |
| Atrasados | hoje | tombo, título, leitor, telefone, e-mail, saída, devolução prevista, dias de atraso |
| Mais emprestados | saída no período | posição, título, autores, ISBN, empréstimos (100 primeiros) |
| Reservas | feita no período | protocolo, feita em, título, tombo, leitor, situação, prazo, finalizada em, cancelada por |
| Acervo | tudo | tombo, ISBN, título, autores, editora, ano, localização, estado, situação, aquisição, origem |

Como foi implementado (fase 5):
- Pacote `relatorio`: `RelatorioRepository` consulta em SQL direto (`JdbcClient`), sem carregar entidades; os relatórios vão linha a linha para o CSV.
- Formato pensado para o Excel e o LibreOffice em português: UTF-8 com BOM, `;` entre colunas, datas `dd/MM/aaaa`, CRLF. Texto começando com `=`, `+`, `-`, `@`, tabulação ou retorno ganha um apóstrofo na frente, para não virar fórmula (proteção contra injeção de fórmula em CSV).
- O CPF não sai em nenhum relatório. Telefone e e-mail só no de atrasados, que serve para cobrar a devolução.
- Cada exportação fica na auditoria (`RELATORIO_EXPORTADO`, com o tipo e o período).
- No front, o download passa pelo `HttpClient` (a API exige o token) e o nome do arquivo vem do `Content-Disposition`.

## Cadastro por ISBN

O funcionário bipa o código de barras da contracapa, que é o próprio ISBN-13 (EAN 978/979), ou digita o número. O formulário volta preenchido e com a capa. **Nada é salvo sem conferência.**

1. Valida o dígito verificador. ISBN-10 é convertido para ISBN-13.
2. Se a obra já existe no banco, a tela só oferece "Adicionar exemplar".
3. Consulta a **BrasilAPI**: `GET https://brasilapi.com.br/api/isbn/v1/{isbn}`. Sem chave. Agrega CBL, Mercado Editorial, Open Library e Google Books.
4. Consulta o **Google Books**: `GET https://www.googleapis.com/books/v1/volumes?q=isbn:{isbn}&key={GOOGLE_BOOKS_API_KEY}`.
5. Consulta a **Open Library**: `GET https://openlibrary.org/isbn/{isbn}.json`. A capa vem de `https://covers.openlibrary.org/b/isbn/{isbn}-L.jpg?default=false`.
6. Mescla: a primeira fonte define os dados e as seguintes completam os campos vazios.
7. Se nada for encontrado, abre o formulário manual com upload de foto da capa.

Detalhes de implementação:
- Interface `IsbnProvider`, uma implementação por fonte, orquestradas pelo `IsbnLookupService`.
- `RestClient` com timeout de 4 s por fonte. Chamadas em paralelo com virtual threads.
- Cache Caffeine de 24 h por ISBN, incluindo "não encontrado". Se alguma fonte falhou, o resultado não entra no cache.
- Se a obra já está no acervo, `GET /api/v1/isbn/{isbn}` devolve só o `obraId`, sem consultar as fontes externas, e a tela oferece "Adicionar exemplar".
- Fora isso, `GET /api/v1/isbn/{isbn}` responde 200 com `encontrado`, os `dados` mesclados, as `fontes` que contribuíram e as `fontesIndisponiveis`. ISBN inválido dá 400 com o motivo. A URL externa da capa não sai na resposta, só `capaDisponivel`.
- Na prática o Google Books exige `GOOGLE_BOOKS_API_KEY`: sem chave, a cota anônima compartilhada costuma estar esgotada (HTTP 429) e o Google fica de fora.
- A capa é **baixada e guardada localmente**, com uma miniatura de cerca de 200 px. Nunca se usa link externo na tela.
- Capas em `CAPAS_DIR`, como `obra-{id}-{versão}.jpg` (maior lado até 800 px) e `obra-{id}-{versão}-mini.jpg` (200 px de largura). Toda imagem é regravada em JPEG, o que descarta metadados como o GPS da foto do celular, e a orientação EXIF é aplicada antes.
- `GET /isbn/{isbn}/capa` serve a prévia para o formulário de conferência, já baixada e processada pelo backend. O `POST /obras` com `fonteDados` de uma fonte grava essa mesma capa logo depois do cadastro. Se falhar, a obra fica sem capa e o funcionário envia uma foto em `POST /obras/{id}/capa`.
- O download da capa externa só aceita http/https e `image/*` até 5 MB, e recusa endereços da rede interna, inclusive por redirecionamento.

### Importação em lote (carga inicial)

Para cadastrar o acervo que já existe sem bipar livro por livro. Em `/painel/acervo/importar` (só administrador) envia-se um CSV ou TXT com uma linha por exemplar:

```text
ISBN;Tombo;Localização
9788535914849;000123;Estante 4
8535902775;000125
```

- Tombo e localização são opcionais; sem tombo, só a obra é cadastrada. Aceita `;`, `,` ou tabulação, UTF-8 ou o Windows-1252 do Excel, cabeçalho opcional, até 5.000 linhas (1 MB) por arquivo.
- O que dá para conferir na hora vira erro da linha antes de processar: ISBN inválido, ISBN que o Excel transformou em número (`9,78857E+12`), tombo fora do formato, tombo repetido no arquivo.
- O processamento roda em segundo plano, uma planilha de cada vez e uma linha por vez, com 500 ms entre consultas às fontes (`biblioteca.importacao.intervalo`) para não esgotar as cotas. A tela mostra o progresso e pode ser fechada. Se a API reiniciar, continua de onde parou.
- Cada linha termina como: **Cadastrado** (obra nova com os dados e a capa da fonte, mais o exemplar), **Exemplar incluído** (a obra já existia), **Já no acervo** (obra existente, linha sem tombo), **Não encontrado** (nenhuma fonte conhece: cadastrar à mão), **Fonte fora do ar** (dá para tentar de novo com um botão) ou **Erro** (com o motivo).
- Diferente do cadastro na tela, aqui ninguém confere os dados antes: texto maior que os limites do formulário é cortado e ano ou páginas fora da faixa ficam vazios. Vale revisar por amostragem depois.
- Cada linha é uma transação: obra e exemplar entram juntos ou nenhum entra. O envio fica na auditoria (`IMPORTACAO_INICIADA`).
- Tabelas `importacao` e `importacao_item` (migration V5), mantidas como histórico.

---

## Modelo de dados

A obra guarda os dados do ISBN. O exemplar é cada cópia física, identificada pelo tombo. **O empréstimo aponta para o exemplar.**

```mermaid
erDiagram
    OBRA ||--o{ OBRA_AUTOR : tem
    AUTOR ||--o{ OBRA_AUTOR : escreve
    OBRA ||--o{ OBRA_ASSUNTO : tem
    ASSUNTO ||--o{ OBRA_ASSUNTO : classifica
    OBRA ||--o{ EXEMPLAR : possui
    EXEMPLAR ||--o{ EMPRESTIMO : sai_em
    LEITOR ||--o{ EMPRESTIMO : faz
    FUNCIONARIO ||--o{ EMPRESTIMO : registra
    FUNCIONARIO ||--o{ AUDITORIA : gera
    OBRA ||--o{ RESERVA : recebe
    EXEMPLAR ||--o{ RESERVA : fica_preso
    LEITOR ||--o{ RESERVA : faz
    EMPRESTIMO |o--o| RESERVA : origina

    OBRA {
        bigint id PK
        char isbn13 UK
        varchar titulo
        varchar subtitulo
        varchar editora
        smallint ano
        int paginas
        varchar idioma
        text sinopse
        varchar capa_arquivo
        varchar capa_miniatura
        varchar fonte_dados
        timestamp criado_em
    }
    AUTOR {
        bigint id PK
        varchar nome
    }
    OBRA_AUTOR {
        bigint obra_id FK
        bigint autor_id FK
        smallint ordem
    }
    ASSUNTO {
        bigint id PK
        varchar nome
    }
    OBRA_ASSUNTO {
        bigint obra_id FK
        bigint assunto_id FK
    }
    EXEMPLAR {
        bigint id PK
        bigint obra_id FK
        varchar tombo UK
        varchar localizacao
        enum estado "BOM, DANIFICADO"
        enum situacao "DISPONIVEL, RESERVADO, EMPRESTADO, EXTRAVIADO, BAIXADO"
        date data_aquisicao
        enum origem "COMPRA, DOACAO"
    }
    LEITOR {
        bigint id PK
        varchar nome
        char cpf UK
        date data_nascimento
        varchar telefone
        varchar email
        varchar endereco
        varchar responsavel "obrigatório se menor"
        boolean ativo
        date bloqueado_ate
        boolean cadastro_pendente "pré-cadastro pelo site"
    }
    EMPRESTIMO {
        bigint id PK
        bigint exemplar_id FK
        bigint leitor_id FK
        bigint funcionario_saida_id FK
        date data_saida
        date data_prevista
        date data_devolucao "NULL = ativo"
        bigint funcionario_devolucao_id FK
        smallint renovacoes
    }
    FUNCIONARIO {
        bigint id PK
        varchar nome
        varchar login UK
        enum tipo_autenticacao "AD, LOCAL"
        varchar senha_hash "NULL se AD"
        enum perfil "ADMIN, ATENDENTE"
        boolean ativo
    }
    AUDITORIA {
        bigint id PK
        bigint funcionario_id FK
        varchar acao
        varchar entidade
        bigint entidade_id
        timestamp data_hora
        text detalhe
    }
    RESERVA {
        bigint id PK
        varchar protocolo UK
        bigint obra_id FK
        bigint exemplar_id FK
        bigint leitor_id FK
        timestamp criada_em
        date prazo_retirada
        varchar telefone "informado na reserva"
        varchar email "opcional"
        enum status "AGUARDANDO_RETIRADA, RETIRADA, CANCELADA, EXPIRADA"
        bigint emprestimo_id FK "quando retirada"
        timestamp finalizada_em
    }
    FERIADO {
        date data PK
        varchar descricao
        enum tipo "NACIONAL, MUNICIPAL"
    }
    PARAMETRO {
        varchar chave PK
        varchar valor
    }
```

- Um empréstimo está **atrasado** quando `data_devolucao` é nula e `data_prevista` é anterior a hoje. Não existe tabela de histórico: o histórico é a própria tabela `emprestimo`.
- Os **disponíveis** de uma obra são os exemplares com `situacao = DISPONIVEL`.
- O **total de exemplares** de uma obra não conta os baixados. A API devolve `totalExemplares` e `exemplaresDisponiveis` junto com a obra.
- **Tombo:** até 30 letras, números, ponto ou hífen, sem barra nem espaço (vai no caminho da URL). Gravado em maiúsculas e único sem diferenciar caixa. **Opcional no cadastro do exemplar:** em branco, o sistema gera `AUTO-` + o id com 6 dígitos (`AUTO-000123`); os prefixos `AUTO-` e `TMP-` são reservados e não podem ser digitados. Na tela, Enter com o campo vazio não cadastra (evita exemplar sem tombo por bipe falho): é preciso clicar em "Adicionar sem tombo". A importação em lote continua igual: linha sem tombo cadastra só a obra, para não duplicar exemplares ao reimportar a planilha.
- **Situação do exemplar:** `RESERVADO` e `EMPRESTADO` só mudam pelos fluxos de reserva e empréstimo. Manualmente (Admin) só se marca `EXTRAVIADO`, `BAIXADO` ou a volta de um extraviado para `DISPONIVEL`. `BAIXADO` é definitivo: o exemplar não muda mais. Localização e estado podem mudar com o exemplar emprestado.
- Obra, exemplar e leitor com histórico nunca são apagados, só desativados ou baixados.
- O esquema é versionado com Flyway (`V{n}__descricao.sql`). Migration já aplicada não se edita.

---

## Regras de negócio

Os valores ficam na tabela `parametro` e o administrador pode editá-los. **Ainda falta confirmar com a Secretaria de Cultura.**

| Regra | Proposta |
| --- | --- |
| Prazo de empréstimo | 14 dias |
| Livros ao mesmo tempo por leitor | 3 |
| Renovações | até 2, de 14 dias cada |
| Leitor com atraso | Não pega outro livro nem renova |
| Penalidade | Bloqueio pelo mesmo número de dias do atraso, sem multa |
| Exemplar danificado ou extraviado | Sai do empréstimo; registrado na devolução |
| Leitor menor de idade | Exige responsável |
| Reservas | Só com exemplar disponível; contam junto com os empréstimos no limite de 3; no máximo 1 reserva por obra; leitor bloqueado ou com atraso não reserva |
| Prazo de retirada da reserva | 2 dias úteis. Feriados nacionais carregados da BrasilAPI uma vez por ano; os municipais são cadastrados pelo administrador |

Como as regras foram implementadas (fase 2):

- **Dias de atendimento:** segunda a sexta, menos os feriados da tabela `feriado`. Devolução prevista que cai em fim de semana ou feriado passa para o próximo dia útil.
- **Renovação:** conta a partir do dia da renovação (`DIAS_POR_RENOVACAO`), nunca encurta o prazo atual. Não renova empréstimo atrasado nem de leitor bloqueado ou com outro livro atrasado.
- **Bloqueio por atraso:** N dias de atraso bloqueiam o leitor por N dias, contando o dia da devolução (3 dias de atraso: bloqueado hoje, amanhã e depois). Bloqueios só aumentam; o administrador pode desbloquear antes.
- **Devolução com ocorrência:** `DANIFICADO` volta ao acervo marcado como danificado (a equipe decide se dá baixa); `EXTRAVIADO` (o leitor perdeu) deixa o exemplar extraviado.
- **Limite:** empréstimos em aberto + reservas aguardando retirada, até `LIMITE_LIVROS_POR_LEITOR`.
- **Leitor:** CPF obrigatório para todos, inclusive crianças (é o login da reserva pública). Menor de 18 exige o nome do responsável. Com livro emprestado, não pode ser desativado.
- O banco garante um único empréstimo em aberto por exemplar (coluna gerada + índice único), e o exemplar é travado (`SELECT ... FOR UPDATE`) na saída e na devolução.
- Parâmetros (tabela `parametro`): `PRAZO_EMPRESTIMO_DIAS`, `LIMITE_LIVROS_POR_LEITOR`, `MAXIMO_RENOVACOES`, `DIAS_POR_RENOVACAO`, `PRAZO_RETIRADA_RESERVA_DIAS_UTEIS`, cada um com faixa de valores aceitos.

---

## API REST (`/api/v1`)

| Método e rota | Descrição | Perfil |
| --- | --- | --- |
| `POST /auth/login` | Login AD ou local, devolve o JWT | público |
| `GET /auth/eu` · `POST /auth/senha` | Quem está logado · troca da própria senha (usuário local) | qualquer funcionário, inclusive com senha provisória |
| `GET /isbn/{isbn}` | Dados mesclados das APIs externas (não salva) | Atendente |
| `GET/POST /obras`, `PUT /obras/{id}` | Acervo | Atendente |
| `POST /obras/{id}/capa` · `GET /obras/{id}/capa?tamanho=mini` | Capa | Atendente · público |
| `GET /isbn/{isbn}/capa?tamanho=mini` | Prévia da capa da fonte, antes de salvar | Atendente |
| `GET/POST /obras/{id}/exemplares` · `GET /exemplares/tombo/{tombo}` | Exemplares | Atendente |
| `PATCH /exemplares/{id}` | Situação (baixa, extravio), estado e localização | Admin |
| `GET/POST /leitores?q=`, `GET/PUT /leitores/{id}`, `GET /leitores/{id}/situacao` | Leitores (busca por nome ou CPF) | Atendente |
| `POST /leitores/{id}/desbloqueio` | Tira o bloqueio por atraso | Admin |
| `POST /emprestimos`, `/{id}/renovacao`, `/{id}/devolucao` | Balcão | Atendente |
| `GET /emprestimos?status=&leitorId=&tombo=&q=` | `ATIVO` (todos em aberto), `ATRASADO` ou `DEVOLVIDO` | Atendente |
| `GET /parametros` · `PUT /parametros/{chave}` | Prazos e limites | Atendente · Admin |
| `GET /painel` | Contadores do dia, até 10 itens de vence hoje, atrasados e reservas para separar, saídas e devoluções dos últimos 7 dias | Atendente |
| `GET /relatorios/{tipo}.csv?de=&ate=` | CSV: `emprestimos`, `atrasados`, `acervo`, `mais-emprestados`, `reservas` (período padrão: últimos 30 dias; máximo 5 anos) | Atendente |
| `GET /reservas?status=&leitorId=&q=` · `GET /reservas/{id}` · `POST /reservas/{id}/retirada` · `POST /reservas/{id}/cancelamento` | Reservas na gestão (`status` aceita vários; padrão aguardando) | Atendente |
| `GET/POST /funcionarios`, `PUT /funcionarios/{id}`, `POST /funcionarios/{id}/senha-provisoria` | Funcionários | Admin |
| `GET /feriados?ano=` · `POST /feriados`, `DELETE /feriados/{data}`, `POST /feriados/importacao?ano=` | Feriados (importação dos nacionais pela BrasilAPI) | Atendente · Admin |
| `GET /auditoria?acao=` | Auditoria | Admin |
| `POST /importacoes` (multipart `arquivo`) · `GET /importacoes` · `GET /importacoes/{id}` · `GET /importacoes/{id}/itens?resultado=` · `POST /importacoes/{id}/nova-tentativa` | Carga do acervo por planilha de ISBNs | Admin |

### Rotas públicas (`/api/v1/publico`, sem login, com limite por IP)

| Método e rota | Descrição |
| --- | --- |
| `GET /publico/obras?q=&assunto=&disponiveis=true` | Catálogo paginado (só dados da obra e quantidade disponível) |
| `GET /publico/obras/{id}` | Ficha do livro |
| `GET /publico/assuntos` | Lista para os filtros, com a quantidade de obras |
| `GET /publico/biblioteca` | Nome, endereço, horário, telefone, e-mail, prazo de retirada, limite de livros e prazo do empréstimo |
| `POST /publico/reservas` | `{ obraId, nome, cpf, dataNascimento, telefone, email }` → protocolo e prazo. CPF novo vira pré-cadastro. Telefone obrigatório, com DDD, gravado como `(51) 99999-9999`; e-mail opcional |
| `POST /publico/reservas/consulta` | `{ cpf, dataNascimento }` → reservas do leitor |
| `POST /publico/reservas/{protocolo}/cancelamento` | `{ cpf, dataNascimento }` |

Os erros seguem o `ProblemDetail` (RFC 9457) com mensagem em português. A documentação fica em `/swagger-ui.html`.

---

## Variáveis de ambiente

| Variável | Uso |
| --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | MySQL |
| `MYSQL_ROOT_PASSWORD` | Só o container do MySQL no `docker-compose.yml` |
| `JWT_SECRET` | Assinatura do JWT do sistema |
| `AD_AUTH_URL` | URL da API de login AD |
| `AD_AUTH_TOKEN` | Bearer da API de login AD |
| `ADMIN_INICIAL_SENHA` | Senha provisória do primeiro administrador (`admin`), usada só com o banco sem funcionários |
| `GOOGLE_BOOKS_API_KEY` | Chave do Google Books |
| `CAPAS_DIR` | Pasta das capas (padrão `/dados/capas`) |
| `DOMINIO` | Endereço do site no `docker-compose.prod.yml` (`server_name` do Nginx) |
| `BIBLIOTECA_ENDERECO`, `BIBLIOTECA_HORARIO`, `BIBLIOTECA_TELEFONE`, `BIBLIOTECA_EMAIL` | Aparecem no rodapé do site (e os três primeiros na confirmação da reserva) |

Nenhum segredo vai para o repositório. Em desenvolvimento, as variáveis ficam no `.env` da raiz, que o Compose e o backend leem; variáveis de ambiente reais têm prioridade sobre ele.

## Como rodar (local)

```bash
cp .env.example .env                 # uma vez, preencher as senhas
docker compose up -d                 # MySQL 8.4
cd backend && ./mvnw spring-boot:run # API em http://localhost:8080
cd frontend && ng serve              # Front em http://localhost:4200
```

Testes: `cd backend && ./mvnw verify` (precisa do Docker rodando, por causa do Testcontainers) · `cd frontend && ng test --watch=false`

O `ng serve` repassa `/api` para `http://localhost:8080` (`frontend/proxy.conf.json`).

## Implantação

Servidor Linux com Docker e Docker Compose. Tudo sobe com o `docker-compose.prod.yml`:

| Serviço | Imagem | O que faz |
| --- | --- | --- |
| `mysql` | `mysql:8.4` | Banco, no volume `mysql-dados`. Sem porta exposta |
| `backend` | `backend/Dockerfile` (Temurin 25, usuário sem privilégios) | API na porta 8080 da rede interna; capas no volume `capas` |
| `web` | `frontend/Dockerfile` (Nginx 1.28) | Serve o Angular, encaminha `/api` para o backend, HTTPS nas portas 80 e 443 |

Primeira subida:

```bash
git clone <repositório> /opt/biblioteca-imbe && cd /opt/biblioteca-imbe
git checkout main
cp .env.example .env            # preencher: senhas, JWT_SECRET, ADMIN_INICIAL_SENHA, DOMINIO, dados da biblioteca
cp <certificado> deploy/certs/fullchain.pem && cp <chave> deploy/certs/privkey.pem
docker compose -f docker-compose.prod.yml up -d --build
```

Depois, entrar em `https://{DOMINIO}/login` com `admin` e a senha provisória, trocar a senha, cadastrar os funcionários, conferir os feriados municipais e importar a planilha do acervo.

Atualização: `git pull && docker compose -f docker-compose.prod.yml up -d --build` (as migrations do Flyway rodam sozinhas na subida da API). Rodar `./mvnw verify` antes, numa máquina com Docker: a imagem não roda os testes.

**HTTPS.** O Nginx lê `deploy/certs/fullchain.pem` e `privkey.pem` (fora do git). O certificado vem da TI da Prefeitura ou do Let's Encrypt: a porta 80 já serve `/.well-known/acme-challenge/` a partir de `deploy/certbot`, então `certbot certonly --webroot -w deploy/certbot -d biblioteca.imbe.rs.gov.br` funciona com o site no ar (copiar os arquivos gerados para `deploy/certs` e `docker compose -f docker-compose.prod.yml restart web`). Para homologação, um certificado provisório:

```bash
openssl req -x509 -newkey rsa:2048 -nodes -days 90 -subj "/CN=localhost"   -keyout deploy/certs/privkey.pem -out deploy/certs/fullchain.pem
```

O Nginx manda HTTP para HTTPS, liga HSTS, CSP (só as fontes do Google como origem externa), `X-Frame-Options` e `nosniff`, guarda em cache por um ano os arquivos com hash do build e nunca o `index.html`. Ele **substitui** o `X-Forwarded-For` pelo IP de quem conectou, para o limite de tentativas do site não ser enganado. Se a Prefeitura já tiver um proxy na frente, esse proxy é quem precisa passar o IP real (e o Nginx passa a confiar só nele, com `set_real_ip_from`).

**Backup.** `deploy/backup.sh` grava `banco_{data}.sql.gz` (mysqldump com `--single-transaction`, sem parar o sistema) e `capas_{data}.tar.gz` em `./backups` (ou `BACKUP_DIR`) e apaga os mais velhos que `BACKUP_DIAS` (30). Agendar no cron do servidor (`0 2 * * * cd /opt/biblioteca-imbe && deploy/backup.sh`) e copiar a pasta para fora do servidor. `deploy/restaurar.sh <banco> <capas>` volta um backup (pede confirmação digitada, porque apaga o atual). Testar a restauração na homologação antes de precisar dela.

Logs: `docker compose -f docker-compose.prod.yml logs -f backend` (limitados a 5 arquivos de 20 MB por serviço).

## Branches

- `develop`: onde o desenvolvimento acontece, com um commit por etapa.
- `main`: só versões prontas, promovidas a partir da `develop` quando decidido.

O servidor precisa de saída HTTPS para `brasilapi.com.br` (ISBN e feriados), `www.googleapis.com`, `openlibrary.org`, `covers.openlibrary.org` e para a API de login AD.

---

## Diretrizes visuais

- Nada de emojis como ícones. Usar um único conjunto de ícones (Material Symbols).
- Usar uma cor principal da Prefeitura. Verde, âmbar e vermelho ficam só para situação, e sempre com texto junto ("Atrasado 3 dias").
- Sem animações de cards. Só o que é clicável reage, mudando a cor de fundo.
- Botões **Emprestar** e **Devolver** sempre visíveis no topo.
- A tela Início mostra o que vence hoje e os atrasados.
- Menu da gestão com 7 itens: Início, Acervo, Leitores, Empréstimos (abas Ativos, Atrasados e Histórico), Reservas, Relatórios e Configurações.
- A tela Início também mostra as reservas para separar.
- **Site público:** pensado primeiro para celular. Logo da Prefeitura e uma busca grande no topo; grade de capas com "3 disponíveis" ou "Todos emprestados"; ficha do livro com um único botão Reservar; rodapé com contato da biblioteca, como reservar e acesso rápido.
- Capa em miniatura nas listas. Texto base de 15 a 16 px. Formulários em uma coluna.
- Mensagens e confirmações dentro da tela, nunca `alert()` ou `confirm()`.
- **Identidade visual: Manual de Marca da Prefeitura de Imbé (2021-2028).**
  - Cores: azul da marca `#006EB7` como principal, azul-claro `#47BAEC`, marinho `#1F305F`, cinza `#677A90` e o sol de `#FBBC43` a `#EC5F65`. O tema do Material é gerado em `frontend/src/styles/_theme-colors.scss` (comando no `styles.scss`) e as cores da marca ficam em variáveis `--marca-*`.
  - Logos em `frontend/public/marca/`, recortadas do PDF do manual: vertical 1 sem o conceito (colorida e branca), horizontal com "UM LUGAR CHEIO DE VIDA" (colorida e branca) e o símbolo no favicon.
  - Assinatura da biblioteca (`app-assinatura`) no padrão das secretarias (pág. 18): marca vertical 1, fio vertical e "Biblioteca Municipal" alinhado à esquerda. Ela aparece no menu da gestão, no topo do site e no login.
  - Respeitar o manual: a vertical 1 não fica com menos de 100 px de largura e a horizontal com menos de 170 px (pág. 15). Em fundo claro, a versão colorida; em fundo escuro, a branca (pág. 17).
  - Faixa com os degradês do mar e do sol (`--marca-faixa`) no alto das telas. Rodapé do site em marinho, com a marca horizontal branca e o link para `www.imbe.rs.gov.br`.
  - Tipografia: Lexend Exa nos títulos e na assinatura (substituta da Termina), Lexend no texto.
- Cores de situação em variáveis CSS (`--situacao-ok`, `--situacao-alerta`, `--situacao-erro`) no `styles.scss`, com tons do verde, amarelo e vermelho institucionais, escurecidos no texto para manter contraste AA.
- Cadastro pelo leitor USB: o campo de ISBN e o de tombo ficam focados e o Enter do leitor envia. Depois de salvar a obra, a ficha abre com o formulário de exemplar pronto; cada tombo bipado é adicionado e o campo volta a ficar focado para a próxima cópia.

---

## Roadmap

1. **Base e ISBN:** esqueleto, Flyway, `IsbnLookupService` com as 3 fontes, cadastro de obra com capa e exemplares.
2. **Leitores e balcão:** leitor, empréstimo, devolução, renovação e regras.
3. **Login e perfis:** login AD (API) e local, JWT, funcionários e auditoria.
4. **Site público e reservas:** catálogo, ficha, reserva com protocolo, minhas reservas, tela Reservas na gestão, job de expiração e feriados.
5. **Painel e relatórios:** a tela Início (vence hoje, reservas para separar, atrasados), o gráfico de 7 dias e os CSVs.
6. **Implantação:** docker compose, domínio `biblioteca.imbe.rs.gov.br` com HTTPS, backup, homologação e carga inicial do acervo (importação em lote de ISBNs).
7. **Depois do MVP:** fila de espera, aviso por WhatsApp ou e-mail, SSR para indexação no Google, leitura de código de barras pela câmera.

## Decisões em aberto

- [ ] Contrato da API de login AD: endpoint, campos, respostas de erro, certificado e validade do Bearer.
- [ ] Prazo, limite de livros e penalidade por atraso, a confirmar com a Secretaria de Cultura.
- [ ] Onde o sistema vai rodar, se há saída para a internet e a publicação de `biblioteca.imbe.rs.gov.br` (DNS e certificado) com a TI.
- [ ] Tamanho do acervo atual e se há planilha para importar (a importação aceita CSV com ISBN, tombo e localização).
- [ ] Certificado HTTPS (TI da Prefeitura ou Let's Encrypt) e destino da cópia externa dos backups.
- [ ] Servir as fontes (Lexend, Lexend Exa e Material Symbols) do próprio servidor em vez do Google, se o DPO pedir.
- [ ] Padrão de numeração do tombo e etiquetas (o sistema já aceita letras, números, ponto e hífen).
- [ ] Compra de leitor de código de barras USB.
- [ ] Licença web da fonte Termina, a do manual de marca. Até lá, Lexend Exa nos títulos e Lexend no texto.
- [ ] Arquivos vetoriais da marca (SVG ou AI) com a Comunicação da Prefeitura. Hoje as logos são PNG recortados do PDF do manual.
- [ ] Validação do tratamento de dados dos leitores com o DPO da Prefeitura.
- [ ] CAPTCHA na reserva pública ou só limite de tentativas?
- [ ] Feriados municipais de Imbé e telefone da biblioteca (endereço, horário e e-mail já estão no `.env.example`).

## Registro de decisões

| Data | Decisão |
| --- | --- |
| 2026-11-17 | Partir do protótipo HTML com 8 telas e refazer o visual sem cara de IA |
| 2026-11-17 | Cadastro de livro pelo ISBN: BrasilAPI → Google Books → Open Library, com capa armazenada localmente |
| 2026-11-17 | Separar Obra (dados do ISBN) de Exemplar (cópia física com tombo) |
| 2026-11-17 | Monolito modular, Java 25 + Spring Boot 4, Angular 22 |
| 2026-11-17 | Login duplo, AD ou local, como no Contract Management. AD validado por API interna (URL + Bearer), sem LDAP no sistema |
| 2026-11-17 | Banco MySQL 8 |
| 2026-11-17 | Duas modalidades no mesmo domínio `biblioteca.imbe.rs.gov.br`: site público em `/` e gestão em `/login` e `/painel`, num único app Angular |
| 2026-11-17 | Reserva pública com CPF + data de nascimento (só leitor cadastrado), apenas com exemplar disponível, retirada em 2 dias úteis, acompanhamento por protocolo, sem e-mail |
| 2026-11-17 | Branches `develop` (desenvolvimento) e `main` (versões prontas, promovidas quando decidido) |
| 2026-11-17 | MySQL fixado em 8.4 (LTS) no Compose e no Testcontainers; `docker-compose.yml` na raiz lendo o `.env` |
| 2026-11-17 | Até a fase 3 (login), a API fica liberada por um `SecurityConfig` provisório |
| 2026-11-17 | Enums do banco como `VARCHAR` + `CHECK`, não `ENUM` do MySQL |
| 2026-11-17 | Busca por ISBN: fonte fora do ar não vai para o cache; resposta sempre 200 com `encontrado`; URL externa da capa não chega ao front |
| 2026-11-17 | Autores e assuntos são reaproveitados sem diferenciar maiúsculas nem acentos ("JOSE SARAMAGO" = "José Saramago"); o primeiro nome cadastrado é o que aparece |
| 2026-11-17 | `GET /obras?q=` busca por ISBN (se o termo for um ISBN válido) ou por trecho do título ou do autor |
| 2026-11-17 | Capas sempre regravadas em JPEG (até 800 px e miniatura de 200 px), sem metadados; prévia da capa da fonte servida pelo backend |
| 2026-11-17 | Prazos em dias úteis (seg a sex, sem feriados); bloqueio de N dias contando o dia da devolução; renovação conta do dia da renovação; CPF obrigatório também para crianças |
| 2026-11-17 | Tela de cadastro: busca pelo ISBN, conferência em formulário de uma coluna, foto da capa opcional; o ISBN aparece sem hífens (a hifenização correta depende da tabela de faixas do ISBN) |
| 2026-11-17 | Exemplar: tombo alfanumérico com ponto e hífen, em maiúsculas; mudança manual de situação só para extravio, baixa ou volta de extraviado; baixa é definitiva; total da obra sem os baixados |
| 2026-11-17 | Balcão pensado para o leitor de código de barras: o campo do tombo fica sempre focado; na devolução, o primeiro Enter busca e o segundo confirma ("voltou em ordem" já marcado) |
| 2026-11-17 | Login: JWT só com o id do funcionário, perfil e situação lidos do banco a cada requisição; senha provisória só permite trocar a senha; 5 erros travam o login por 15 min; contrato provisório da API do AD documentado no `ValidadorAd` |
| 2026-11-17 | Primeiro administrador criado pela variável `ADMIN_INICIAL_SENHA` quando não há funcionários; token do front no `sessionStorage` |
| 2026-11-17 | Site público: catálogo só com obras em circulação; protocolo de 8 caracteres; reserva bipada no balcão vira retirada; limite de livros não impede a retirada; expiração de hora em hora |
| 2026-11-17 | Limite de tentativas do site em memória (Caffeine), por IP e por CPF, no lugar do Bucket4j; feriados nacionais da BrasilAPI importados sozinhos, sem sobrescrever os municipais |
| 2026-11-17 | Painel e relatórios por SQL direto; CSV com BOM, `;` e proteção contra fórmula; sem CPF nos relatórios; exportação auditada; gestão abre na tela Início |
| 2026-11-17 | Importação em lote por CSV (ISBN;tombo;localização), processada em segundo plano linha a linha, sem conferência humana; ISBN não encontrado fica para o cadastro manual; só administrador |
| 2026-11-17 | Implantação em Docker Compose (MySQL, API, Nginx com HTTPS); certificado fora do git; backup diário por script com mysqldump e capas, retenção de 30 dias |
| 2026-11-17 | Visual pelo Manual de Marca da Prefeitura (2021-2028): azul `#006EB7` como cor principal, logos oficiais, assinatura no padrão das secretarias; Lexend Exa e Lexend no lugar da Termina enquanto não houver licença web |
| 2026-11-17 | Tombo opcional no cadastro manual do exemplar (gerado `AUTO-000123`); importação sem tombo continua só com a obra |
| 2026-11-17 | Reserva pelo site pede telefone (obrigatório) e e-mail (opcional), gravados na reserva (V6) e mostrados à equipe |
| 2026-11-17 | Cadastro de obra não mostra de qual fonte vieram os dados nem quais fontes falharam: só "Livro encontrado", "Não encontramos" ou "Não foi possível buscar agora" (com tentar de novo). A fonte continua gravada na obra |
| 2026-11-17 | Reserva não exige cadastro: CPF novo vira pré-cadastro pendente (V7), conferido pela equipe na retirada; sem a conferência o livro não sai |

## Documentação
- Toda decisão nova ou mudança de escopo atualiza o README.md
  (seção correspondente + linha em "Registro de decisões" com a data)