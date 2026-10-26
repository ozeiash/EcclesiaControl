# feat(users): gestão de usuários por filial e bloqueio de usuários inativos

Base: `main` · Head: `feature/ep04-fnd-user-management`

> Antes de publicar: criar a issue usando `issue.md` e substituir `NUMERO_DA_ISSUE` abaixo pelo número criado.

Closes #NUMERO_DA_ISSUE
Relacionado à #4.

Usuários INACTIVE ou SUSPENDED agora recebem 401 no login e no refresh, e access tokens existentes deixam de autenticar imediatamente. A gestão de usuários respeita o cadastro ou papel na filial, limita escritas LOCAL a usuários sem acesso global ou em outras filiais e oculta vínculos externos nas respostas LOCAL.

Inclui normalização de e-mail, auditoria das alterações, revogação de refresh tokens na desativação e redefinição de senha, e proteção contra atribuição ou alteração de SUPER_ADMIN por outros administradores. O CORS do Expo web foi registrado em commit separado. Nenhuma migration foi criada; o ajuste local de docker-compose.yml fica fora do PR.

## Rotas
Prefixo: `/api/v1`. Todas exigem `PERM_USER_MANAGE`.

| Método e rota | Corpo | Resposta |
|---|---|---|
| GET /users?filialId=&status=&page=&size=&sort= | — | 200 Page<UserSummaryResponse> |
| GET /users/{id} | — | 200 UserDetailResponse |
| PUT /users/{id} | UpdateUserRequest | 200 UserDetailResponse |
| PATCH /users/{id}/status | UpdateUserStatusRequest | 200 UserDetailResponse |
| DELETE /users/{userId}/roles/{assignmentId} | — | 204 |
| POST /users/{id}/reset-password | ResetPasswordRequest | 204 |

Ordenação padrão: `fullName,asc`. `phone` aceita até 20 caracteres. Sem divergências do contrato acordado com o frontend.

## Como testar pelo Swagger
1. Pare `ecclesiastic-api`, caso esteja rodando. Inicie pelo IntelliJ com `DB_PORT=5433`, `SPRING_PROFILES_ACTIVE=dev` e as demais variáveis do `.env`.
2. Abra `http://localhost:8080/api/v1/swagger-ui/index.html`.
3. Faça login com `admin@dev.local` / `admin123` e use o accessToken em Authorize.
4. Crie um usuário de teste via POST /users e atribua um papel LOCAL via POST /users/{userId}/roles. Use esse usuário como alvo, preservando as contas do seeder.
5. Liste usuários com e sem filialId/status; teste paginação e sort. Consulte o detalhe, edite nome/e-mail/telefone e confira os campos retornados.
6. Faça login com o alvo e guarde accessToken e refreshToken. Desative-o com a conta admin. Login e refresh devem retornar 401; o accessToken antigo também deve retornar 401 em uma rota protegida. Reative e confirme o login.
7. Redefina a senha do alvo (mínimo 8 caracteres). Confirme que a nova senha funciona e a antiga falha; o refresh anterior deve retornar 401.
8. Remova um papel do alvo e confira 204. Um assignment pertencente a outro usuário retorna 404. É permitido remover o último papel.
9. Entre como `secretaria@dev.local` / `secretaria123`: confira a listagem restrita à filial ativa e 403 ao informar outra filial ou tentar alterar usuário GLOBAL/multi-filial. O detalhe de usuário externo retorna 404.
10. Tente mudar o próprio status, remover o próprio papel ou redefinir a própria senha: 422. Edite e-mail para um já usado: 422. Como GLOBAL sem SUPER_ADMIN, tente atribuir SUPER_ADMIN: 403.

## Validação
Testes de integração em UserManagementServiceTest cobrem escopo, privacidade dos papéis, edição, status, autenticação e tokens, senha, remoção de papel e proteção de SUPER_ADMIN. Resultado final de `mvn test`: **30 testes, 0 falhas, 0 erros, 0 ignorados**, incluindo 11 testes novos. Executado com Java 21 e PostgreSQL local em `ecclesiacontrol_test` (porta 5433).

Solicitar revisão de alguém do grupo, incluindo Ozéias, antes do merge. Não habilitar merge automático.
