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