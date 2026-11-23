# EcclesiaControl
EcclesiaControl é um ERP robusto e modular projetado para a gestão de igrejas, oferecendo suporte a uma arquitetura multifilial (sede e congregações). Desenvolvido com Java Spring Boot e React, ele otimiza as finanças independentes ou consolidadas, o acompanhamento de membros e os fluxos de trabalho dos ministérios.


Desenvolvido com:
- **Backend**: Java 21 + Spring Boot 3.3
- **Frontend**: React 18 + TypeScript + Tailwind
- **Banco de Dados**: PostgreSQL 16
- **Infraestrutura**: Docker + Docker Compose

---

## Status

- [x] Infraestrutura base
- [x] Autenticação (em progresso)
- [ ] Gestão de Membros
- [ ] Financeiro
- [ ] Dashboard

## 📋 Setup Local

```bash
docker-compose up -d
cd backend
mvn spring-boot:run

## Quick Start

### **Prerequisitos**
- Docker + Docker Compose
- Java 21 (apenas se desenvolver backend localmente sem Docker)
- Node 20+ (apenas se desenvolver frontend localmente sem Docker)

### **Com Docker (Recomendado)**

```bash
# Clonar repositório
git clone https://github.com/ozeiash/EcclesiaControl.git
cd EcclesiaControl

# Subir toda a stack (PostgreSQL + Backend + Frontend)
docker-compose up -d

# Acessar
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/swagger-ui.html
- Frontend: http://localhost:3000
```

## Banco de dados de teste

O banco `ecclesiacontrol_test` é criado automaticamente pelo script
`init-scripts/01-create-test-db.sql` na primeira subida do container
Postgres (volume novo). Se precisar recriar do zero:

\`\`\`bash
docker compose down -v
docker compose up -d
\`\`\`
## Vínculo de conta existente com membro

`PUT /users/{userId}/member-link` recebe `{ "memberId": "UUID", "filialId": "UUID" }` e devolve o detalhe da conta com `memberId` e `memberName`. Exige USER_MANAGE, respeita o teto de permissões e não permite alterar a própria conta. Para token GLOBAL, informe filialId; para LOCAL, só a filial ativa é aceita.

A operação é transacional: vincula a ficha e garante o papel MEMBRO sem duplicação. Preserva login, senha e outros papéis. A ficha deve pertencer à filial de cadastro da conta, estar ativa, possuir e-mail e data de nascimento e ser de pessoa maior de idade. A conta deve estar ativa. A operação recusa fichas de outra conta e não substitui vínculos já existentes. Bloqueios de linha e a restrição única já existente protegem contra vínculos concorrentes.

`POST /users/{userId}/roles` aceita `memberId` para role MEMBRO e passa a exigir a ficha quando a conta ainda não está vinculada. Papel e vínculo são salvos juntos; uma falha não deixa o papel atribuído parcialmente. Não há busca automática da pessoa por nome ou e-mail.

O caminho `POST /members/{memberId}/portal-access` continua criando uma conta nova vinculada com senha provisória. Não precisa de migration nova: o campo users.member_id e seu índice único já foram introduzidos pela V20. Reinicie o backend após a atualização do código.
