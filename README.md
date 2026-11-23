# EcclesiaControl

ERP para igrejas com **sede e filiais**: cadastro de membros, financeiro com fechamento mensal, painéis gerenciais e portal do membro. Cada filial enxerga só os próprios dados, a sede enxerga o consolidado, e a contribuição individual é protegida por permissão.

Este repositório contém o **backend** (API REST). O aplicativo (React Native + Expo) está em outro repositório:
**[Junyr/EcclesiaControl-Frontend](https://github.com/Junyr/EcclesiaControl-Frontend)**.

Projeto de Trabalho de Conclusão de Curso do curso de Engenharia de Software da Faculdade SENAI FATESG (2026).

---

## Estado do projeto

Primeira etapa concluída. Os módulos abaixo estão implementados e cobertos por testes automatizados.

| Módulo | O que faz |
|---|---|
| Autenticação | Login por e-mail e senha, JWT, renovação com rotação de *refresh token*, logout, troca de filial |
| Filiais e usuários | Cadastro de filiais (CNPJ numérico e alfanumérico), usuários, papéis e permissões, com teto de permissões |
| Membros e secretaria | Cadastro, situação, histórico ministerial, carta de transferência e certificado em PDF |
| Financeiro | Plano de contas, lançamentos, contribuições (dízimo e oferta), contas a pagar, fechamento e reabertura de períodos |
| Painéis | Dashboard operacional e dashboard oficial (só meses fechados, até 24 meses) |
| Portal do membro | Perfil, senha, declaração de contribuição confirmada pelo tesoureiro, extrato e chave PIX da filial |
| Auditoria | Registro de operações de escrita sensíveis, sem CPF nem RG |

**Planejado para a segunda etapa (não implementado):** transferência de membros entre filiais, repasses entre filiais e sede, orçamento extraordinário e pedidos de serviço.

**Limitações conhecidas desta versão:** sem TLS, limitação de taxa nem MFA; sem análise estática ou de dependências no CI; sem auditoria de leitura de dados pessoais; documentação interativa (Swagger) aberta em todos os perfis. A lista completa e os riscos residuais estão no Relatório Técnico (seção de segurança). **Não use com dados reais de membros sem tratar esses pontos.**

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem e framework | Java 21, Spring Boot 3.3.2 (Web, Security, Data JPA, Validation) |
| Banco de dados | PostgreSQL 16, migrações com Flyway (V1 a V22) |
| Segurança | JWT (HS512, JJWT 0.11.5), BCrypt (custo 12), AES-GCM para CPF e RG, HMAC para busca por CPF |
| Documentação da API | springdoc-openapi 2.3.0 (Swagger UI) |
| PDF | openhtmltopdf + Thymeleaf |
| Testes | JUnit 5, Spring Boot Test, MockMvc, PostgreSQL real; cobertura com JaCoCo |
| Infraestrutura | Docker e Docker Compose; integração contínua no GitHub Actions |

---

## Como executar

### Pré-requisitos

- Docker e Docker Compose
- Java 21 e Maven (só para rodar o backend fora do contêiner e para os testes)

### 1. Configurar o ambiente

```bash
git clone https://github.com/ozeiash/EcclesiaControl.git
cd EcclesiaControl
cp .env.example .env
```

Edite o `.env` e gere os segredos. **Nenhum valor de exemplo serve para uso real.**

```bash
openssl rand -base64 64 | tr -d '\n'   # JWT_SECRET (mínimo de 64 caracteres)
openssl rand -base64 32                # ENCRYPT_KEY (chave AES de 256 bits)
```

| Variável | Descrição |
|---|---|
| `POSTGRES_DB` | Nome do banco da aplicação |
| `POSTGRES_USER`, `POSTGRES_PASSWORD` | Credenciais do PostgreSQL |
| `JWT_SECRET` | Segredo de assinatura do JWT (mínimo de 64 caracteres; com menos, a assinatura HS512 falha no primeiro login) |
| `ENCRYPT_KEY` | Chave AES-GCM em Base64 (32 bytes) que cifra CPF e RG |
| `SPRING_PROFILES_ACTIVE` | `dev` (padrão local) ou `prod` |
| `DB_HOST`, `DB_PORT` | Opcionais; padrão `localhost` e `5432` |

A aplicação **não sobe** sem `JWT_SECRET` e `ENCRYPT_KEY`, e a chave de cifragem é validada na inicialização. É proposital: não existem segredos padrão.

### 2. Subir o banco e a API

**Opção A: tudo em contêineres**

```bash
docker compose up -d postgres api
```

**Opção B: banco em contêiner e API na sua máquina (melhor para desenvolver)**

```bash
docker compose up -d postgres
cd Backend
export $(grep -v '^#' ../.env | xargs)   # carrega o .env no shell
mvn spring-boot:run
```

Endereços locais:

| Recurso | URL |
|---|---|
| API | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8080/api/v1/swagger-ui.html` |
| Contrato OpenAPI | `http://localhost:8080/api/v1/v3/api-docs` |

### 3. Usuários de desenvolvimento

No perfil `dev`, a aplicação cria dados de teste na primeira subida (o código roda **somente** nesse perfil):

| E-mail | Senha | Papel |
|---|---|---|
| `admin@dev.local` | `admin123` | `SUPER_ADMIN` (sede) |
| `pastor@dev.local` | `pastor123` | `PASTOR_FILIAL` |
| `secretaria@dev.local` | `secretaria123` | `SECRETARIO_FILIAL` |
| `tesoureiro@dev.local` | `tesoureiro123` | `TESOUREIRO_FILIAL` |

Para popular uma filial com membros, lançamentos e contribuições de demonstração pela própria API (passando por todas as regras do sistema), com a API no ar:

```bash
python3 Backend/scripts/seed_demo.py
```

### 4. Primeira chamada

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@dev.local","password":"admin123"}'
```

Use o `accessToken` retornado como `Authorization: Bearer <token>`. Perfis globais (como o `SUPER_ADMIN`) precisam trocar de filial (`POST /auth/switch-filial`) antes de acessar dados de uma filial.

---

## Testes

Os testes de ponta a ponta usam **PostgreSQL real** (não H2 nem Testcontainers; veja a ADR-009). O banco `ecclesiacontrol_test` é criado automaticamente pelo script `init-scripts/01-create-test-db.sql` na primeira subida do contêiner. Para recriar do zero:

```bash
docker compose down -v
docker compose up -d postgres
```

Executar a suíte (com o PostgreSQL no ar e as variáveis `POSTGRES_USER` e `POSTGRES_PASSWORD` no ambiente):

```bash
cd Backend
mvn test
```

- **83 testes** (e2e com MockMvc, serviços e repositórios), todos aprovados.
- Os testes de ponta a ponta limpam as tabelas com `TRUNCATE`, **protegido para só rodar em banco cujo nome termina em `_test`**.
- O relatório de cobertura (JaCoCo) é gerado em `Backend/target/site/jacoco/index.html`.
- Na última medição: linhas 73%, instruções 36%, ramos 9%.

O fluxo `.github/workflows/test.yml` roda `mvn test` a cada `push` e `pull_request`, com PostgreSQL 16 de serviço.

---

## Arquitetura em resumo

- **Monólito modular em camadas:** `Controller → Service → Repository`, com pacotes por módulo (`auth`, `core`, `finance`, `member`, `tenant`).
- **Multi-tenancy por coluna `tenant_id`** com filtro do Hibernate (`@Filter`), repositórios escopados e `TenantGuard`. Dado de outra filial responde **404**, não 403.
- **Autorização por permissão** (`MEMBER_READ`, `FINANCE_WRITE`, `PERIOD_CLOSE` etc.), recalculada a cada requisição, com papéis de escopo global (sede) e local (filial).
- **Sigilo da contribuição:** a identidade de quem contribuiu só aparece com `CONTRIBUTION_DETAIL_READ`.
- **Integridade contábil:** mês fechado não aceita alterações; reabertura em cascata, com motivo e evento registrado.
- **Dados pessoais:** CPF e RG cifrados (AES-GCM); busca por CPF via `cpf_hash` (HMAC); auditoria nunca grava CPF nem RG.

Estrutura:

```text
EcclesiaControl/
├── Backend/                 # API Spring Boot
│   ├── src/main/java/br/com/bitabit/ecclesiacontrol/
│   │   ├── auth/  core/  finance/  member/  tenant/
│   ├── src/main/resources/db/migration/   # Flyway V1..V22
│   ├── src/test/java/                     # 83 testes
│   ├── scripts/seed_demo.py               # dados de demonstração via API
│   └── Dockerfile
├── init-scripts/            # cria o banco de testes no PostgreSQL
├── docs/                    # documentação de apoio
├── .github/workflows/       # integração contínua
├── docker-compose.yml
└── .env.example
```

---

## Documentação

A documentação completa do trabalho acompanha a entrega:

- **Monografia** e **Relatório Técnico de Sistema** (requisitos, arquitetura, dicionário de dados, segurança, testes, operação).
- **Registro de decisões de arquitetura (ADR-001 a ADR-013)**.
- **Contrato da API:** Swagger UI, com a aplicação no ar.

---

## Como contribuir

1. Crie uma branch a partir da `main`: `feat/…`, `fix/…` ou `chore/…`.
2. Ao criar um endpoint: exija a permissão com `@PreAuthorize`, coloque a lógica em um serviço `@Transactional`, acesse dados por repositório escopado e escreva o **teste negativo de acesso entre filiais**. Evite `findById`, `existsById` e `getReferenceById` do Spring Data em entidades por filial.
3. Mudanças de banco entram em **nova migração** (`V<n>__descricao.sql`). Nunca edite uma migração já aplicada.
4. Abra um Pull Request. A integração contínua precisa passar antes da fusão.

---

## Equipe

| Integrante | Atuação |
|---|---|
| Ozéias Campos de Souza | Product Owner; backend |
| Gabriel Fernandes Gomes | Scrum Master |
| João Vitor Oliveira de Almeida | Time de desenvolvimento |
| Willian Junior Custódio | Time de desenvolvimento |

Orientador: Prof. M.Sc. Reinaldo de Souza Junior.

## Licença

Distribuído sob a licença MIT. Veja o arquivo [LICENSE](LICENSE).