# 🧁 Cupcake API

Loja de cupcakes com **Java 21**, **Spring Boot** e **PostgreSQL**, integrada a um frontend em **HTML, CSS e JavaScript**.

---

## Descrição 🔗

A aplicação permite consultar cupcakes, selecionar produtos na sacola e realizar pedidos com endereço de entrega. Clientes, produtos, pedidos e itens são persistidos no banco.

- Catálogo com busca, filtro por sabor e imagens.
- Cadastro de clientes durante o checkout.
- Histórico de pedidos e cancelamento antes do preparo.
- Simulação local de pagamento por crédito ou débito.
- API organizada por domínio, com controllers, services, repositories e DTOs.

O frontend e a API são servidos pelo Spring Boot na mesma porta. A aplicação não utiliza autenticação.

---

## Requisitos ⚙️

- JDK 21.
- PostgreSQL em execução.
- Acesso às dependências Maven na primeira execução. O Maven Wrapper está incluído.
- Node.js 24+ para os testes do frontend; não é necessário para abrir a loja.
- Google Chrome para o teste automatizado de navegador.

---

## Configuração local 🔧

### Banco de dados

Crie o banco PostgreSQL ou utilize um banco existente. O usuário configurado precisa ter permissão para criar o schema `cupcake`, tabelas e índices.

As propriedades de `src/main/resources/application.properties` aceitam estas variáveis:

| Variável | Valor padrão | Descrição |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres?currentSchema=cupcake` | URL JDBC |
| `DB_USERNAME` | `postgres` | Usuário PostgreSQL |
| `DB_PASSWORD` | `postgres` | Senha PostgreSQL |

Exemplo no PowerShell, usando um banco `cupcake_store` já criado:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/cupcake_store?currentSchema=cupcake"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "sua-senha"
```

O Flyway executa automaticamente as migrations de `src/main/resources/db/migration`. O Hibernate valida o schema com `ddl-auto=validate`.

Não edite migrations já aplicadas: crie uma nova versão para alterar o banco. Em bases anteriores à V4, resolva e-mails duplicados antes da atualização; essa migration normaliza os e-mails e aplica unicidade.

### Perfis

| Perfil | Configuração |
| --- | --- |
| Padrão | PostgreSQL configurado pelas variáveis de ambiente |
| `local` | PostgreSQL, pagamento simulado, logs locais e acesso por `127.0.0.1` |
| `test` | H2 em memória, definido em `src/test/resources/application-test.properties` |

O perfil `test` deve ser usado com o classpath de testes. Ele não faz parte do JAR normal.

---

## Executar localmente ▶️

Na raiz do projeto, após configurar o banco:

**Windows / PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

No IntelliJ, execute `ApiApplication` com **Active profiles** definido como `local` e as variáveis do banco na configuração de execução.

| Recurso | Endereço |
| --- | --- |
| Loja | http://127.0.0.1:8080/ |
| Catálogo da API | http://127.0.0.1:8080/api/cupcakes |

Não é necessário iniciar um servidor separado para o frontend ou configurar CORS. Para trocar a porta, defina `SERVER_PORT` antes de iniciar.

### Preparar o catálogo

Cadastre os cupcakes por `POST /api/cupcakes` para que apareçam na loja. Não há carga automática de produtos.

As imagens estão em `static/images/cupcakes`. O frontend associa chocolate, morango, baunilha, limão, red velvet, cenoura, coco e doce de leite às imagens locais, com prioridade sobre `imagemUrl`. Outros sabores usam a URL cadastrada ou uma imagem padrão.

A sacola e a identificação do cadastro ficam no `localStorage`; o histórico é consultado na API. O checkout reutiliza o cadastro identificado no mesmo navegador. Um e-mail existente não pode ser cadastrado novamente.

---

## 📌 Contrato dos endpoints

Base URL: `http://127.0.0.1:8080`. Requisições com corpo utilizam `Content-Type: application/json`. IDs são inteiros positivos; substitua os exemplos por IDs existentes no banco.

### 🧁 Cupcakes

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `GET` | `/api/cupcakes` | Lista cupcakes ativos |
| `GET` | `/api/cupcakes/{id}` | Consulta um cupcake ativo |
| `POST` | `/api/cupcakes` | Cadastra um cupcake |
| `PUT` | `/api/cupcakes/{id}` | Atualiza o cadastro completo |
| `PATCH` | `/api/cupcakes/{id}/disponibilidade` | Altera a disponibilidade |
| `DELETE` | `/api/cupcakes/{id}` | Inativa, preservando o histórico |

Corpo de `POST` e `PUT`:

```json
{
  "nome": "Cupcake de Chocolate",
  "descricao": "Massa de chocolate com cobertura de ganache.",
  "preco": 12.50,
  "sabor": "Chocolate",
  "imagemUrl": "/images/cupcakes/chocolate.png",
  "disponivel": true
}
```

`imagemUrl` é opcional. Preço mínimo: `0.01`, com até dez dígitos inteiros e duas casas decimais. Nome e sabor aceitam até 120 caracteres; descrição e URL, até 1.000. A resposta contém esses campos e `id`; a listagem retorna um array.

Corpo de `PATCH /api/cupcakes/{id}/disponibilidade`:

```json
{
  "disponivel": false
}
```

### 👤 Clientes

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `GET` | `/api/clientes` | Lista clientes |
| `GET` | `/api/clientes/{id}` | Consulta um cliente |
| `POST` | `/api/clientes` | Cadastra um cliente |
| `PUT` | `/api/clientes/{id}` | Atualiza o cadastro completo |
| `GET` | `/api/clientes/{id}/pedidos` | Histórico, do pedido mais recente ao mais antigo |

Corpo de `POST` e `PUT`:

```json
{
  "nome": "Ana Silva",
  "email": "ana@example.com",
  "telefone": "71999999999"
}
```

Todos os campos são obrigatórios. Nome aceita até 120 caracteres; e-mail, até 254; telefone, até 30. O e-mail deve ser válido e único; é normalizado para minúsculas, sem espaços nas extremidades. A resposta contém `id`, `nome`, `email` e `telefone`; a listagem retorna um array.

### 🛍️ Pedidos

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `POST` | `/api/pedidos` | Cria um pedido |
| `GET` | `/api/pedidos` | Lista pedidos |
| `GET` | `/api/pedidos/{id}` | Consulta um pedido |
| `PATCH` | `/api/pedidos/{id}/status` | Avança o status |
| `PATCH` | `/api/pedidos/{id}/cancelamento` | Cancela antes do preparo; sem corpo |

Corpo de `POST /api/pedidos`:

```json
{
  "clienteId": 1,
  "itens": [
    {
      "cupcakeId": 1,
      "quantidade": 2
    }
  ],
  "endereco": {
    "logradouro": "Rua das Flores",
    "numero": "123",
    "complemento": "Apartamento 101",
    "bairro": "Centro",
    "cidade": "Salvador",
    "estado": "BA",
    "cep": "40000-000"
  }
}
```

- Cliente e cupcakes devem existir; produtos precisam estar ativos e disponíveis.
- O pedido aceita de 1 a 100 itens distintos, com quantidade de 1 a 100 por item. Não repita o mesmo cupcake em itens diferentes.
- `complemento` é opcional. Estado utiliza duas letras maiúsculas; CEP aceita oito dígitos, com ou sem hífen.
- Logradouro aceita até 200 caracteres; número, até 30; complemento, bairro e cidade, até 120 cada.
- A API calcula o total e preserva nome e preço dos produtos no momento da compra. Não envie `valorTotal` ou `precoUnitario` na requisição.
- A entrega é gratuita e o endereço fica registrado no pedido.

Exemplo de resposta:

```json
{
  "id": 1,
  "clienteId": 1,
  "itens": [
    {
      "cupcakeId": 1,
      "nome": "Cupcake de Chocolate",
      "quantidade": 2,
      "precoUnitario": 12.50
    }
  ],
  "valorTotal": 25.00,
  "status": "AGUARDANDO_PAGAMENTO",
  "statusPagamento": "PENDENTE",
  "formaPagamento": null,
  "dataCriacao": "2026-11-15T12:00:00Z",
  "endereco": {
    "logradouro": "Rua das Flores",
    "numero": "123",
    "complemento": "Apartamento 101",
    "bairro": "Centro",
    "cidade": "Salvador",
    "estado": "BA",
    "cep": "40000-000"
  }
}
```

Consultas e alterações de pedidos retornam esse formato. Listagens e histórico retornam arrays.

Corpo de `PATCH /api/pedidos/{id}/status`:

```json
{
  "status": "PREPARANDO"
}
```

Fluxo:

```text
AGUARDANDO_PAGAMENTO → PEDIDO_ENVIADO → PREPARANDO → PRONTO → ENTREGUE
```

O pagamento simulado realiza a primeira transição; as demais avançam uma etapa por vez pelo endpoint de status. O cancelamento é permitido em `AGUARDANDO_PAGAMENTO` ou `PEDIDO_ENVIADO`. Pedidos cancelados não podem ser reativados.

### 💳 Pagamento simulado

`POST /api/pedidos/{id}/pagamento-simulado` — disponível somente com o perfil `local`.

```json
{
  "formaPagamento": "CREDITO"
}
```

Também aceita `DEBITO`. Não envie número de cartão, validade ou CVV. Não há cobrança ou integração bancária.

A aprovação retorna o pedido com `statusPagamento: APROVADO_SIMULADO` e `status: PEDIDO_ENVIADO`. Repetir a mesma forma retorna a aprovação existente; tentar outra forma após a aprovação retorna `409`. Cancelar um pedido aprovado muda o pagamento para `CANCELADO_SIMULADO`.

Na loja, abra **Meus pedidos** e selecione **Simular crédito** ou **Simular débito**. Sem o perfil `local`, o endpoint retorna `404`.

### Respostas HTTP

| Status | Significado |
| --- | --- |
| `200` | Consulta, cadastro ou atualização concluída |
| `204` | Cupcake inativado; resposta sem corpo |
| `400` | JSON inválido, campos desconhecidos ou falha de validação |
| `404` | Recurso não encontrado ou simulação desabilitada |
| `409` | E-mail duplicado, produto indisponível, transição inválida ou outro conflito |

Erros tratados pela API utilizam `ProblemDetail`. Falhas de validação incluem `campos`:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Dados invalidos.",
  "instance": "/api/pedidos",
  "campos": [
    {
      "campo": "clienteId",
      "mensagem": "must not be null"
    }
  ]
}
```

---

## Estrutura de pastas 📁

```text
api/
├── src/
│   ├── main/
│   │   ├── java/com/cupcake/api/
│   │   │   ├── ApiApplication.java
│   │   │   ├── cliente/             # Cadastro e persistência de clientes
│   │   │   ├── cupcake/             # Catálogo e disponibilidade
│   │   │   ├── pedido/              # Pedidos, itens, endereço e status
│   │   │   ├── pagamento/           # Simulação no perfil local
│   │   │   └── shared/              # Exceções, tratamento de erros e logs
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-local.properties
│   │       ├── db/migration/        # Migrations Flyway
│   │       └── static/
│   │           ├── index.html      # Interface da loja
│   │           ├── styles.css      # Estilos e responsividade
│   │           ├── app.js          # Eventos e integração com a API
│   │           ├── store.mjs       # Sacola, payloads e funções compartilhadas
│   │           └── images/cupcakes/
│   └── test/
│       ├── java/com/cupcake/api/   # Testes Java e integração HTTP/SQL
│       ├── frontend/              # Testes JavaScript e de navegador
│       └── resources/
│           └── application-test.properties
├── .mvn/                         # Maven Wrapper
├── .prettierrc.json               # Formatação do frontend
├── mvnw
├── mvnw.cmd
└── pom.xml
```

Cada domínio concentra suas classes. Os **controllers** expõem os contratos HTTP, os **services** aplicam regras de negócio e transações, os **repositories** acessam o banco via Spring Data JPA e os **DTOs** validam entradas e representam respostas.

---

## Logs e troubleshooting 🔎

O projeto utiliza **Log4j2**. No perfil local, o console inclui `requestId`; a resposta HTTP de pagamento devolve o mesmo identificador em `X-Request-Id`.

| Evento | Uso |
| --- | --- |
| `pagamento_requisicao_recebida` | Entrada da requisição |
| `pagamento_aprovado_simulado` | Aprovação após commit |
| `pagamento_repetido` | Reutilização da aprovação existente |
| `pagamento_rejeitado` | Motivo da rejeição por regra de negócio |
| `pagamento_requisicao_concluida` | Sucesso HTTP e duração |
| `pagamento_requisicao_rejeitada` | Resposta 4xx e duração |
| `pagamento_erro_inesperado` / `pagamento_requisicao_falhou` | Exceção inesperada ou resposta 5xx |

Os logs não registram corpo da requisição, dados pessoais ou dados de cartão. Níveis e formato são configurados em `application-local.properties`.

Se a aplicação não iniciar, confira o PostgreSQL, a existência do banco, as credenciais e as permissões. Se o catálogo estiver vazio, verifique os cadastros em `GET /api/cupcakes`. Para pagamento com resposta `404`, confirme o perfil `local`.

---

## Testes 🧪

### Backend

```powershell
.\mvnw.cmd verify
```

Os testes utilizam H2 em modo PostgreSQL, executam as migrations e verificam compra, persistência, validação, histórico, status, cancelamento, imagens e logs de pagamento.

### Frontend

```powershell
node --test src/test/frontend/store.test.mjs
```

Verificação de formatação:

```powershell
npm exec --yes --package=prettier -- prettier --check "src/main/resources/static/*.{html,css,js,mjs}" "src/test/frontend/*.mjs"
```

### Fluxo no navegador

Inicie um servidor isolado com H2 na porta `18080`:

```powershell
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.profiles=test,local" "-Dspring-boot.run.arguments=--server.port=18080"
```

Em outro terminal:

```powershell
node src/test/frontend/browser.mjs
```

O teste cria registros nesse servidor e verifica imagens, sacola após recarga, checkout, histórico, pagamento, cancelamento e layout mobile. Requer Chrome no caminho padrão do Windows ou a variável `CHROME_PATH`. A captura fica em `target/loja-mobile.png`. Encerre o servidor ao terminar.

---

## Tecnologias

- Java 21 e Spring Boot 4.1.2.
- Spring Web MVC, Spring Data JPA e Bean Validation.
- PostgreSQL, Flyway e H2 para testes.
- Log4j2 e Lombok.
- HTML, CSS e JavaScript modular.
- Maven Wrapper, JUnit e Node.js Test Runner.

[![Tecnologias](https://skillicons.dev/icons?i=java,spring,postgres,maven,html,css,js)](https://skillicons.dev)
