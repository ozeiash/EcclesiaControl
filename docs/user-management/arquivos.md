# Arquivos da entrega

Caminhos relativos à raiz do repositório.

## Alterados
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/core/config/SecurityConfig.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/core/security/JwtAuthenticationFilter.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/service/AuthService.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/service/UserManagementService.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/controller/UserManagementController.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/repository/UserRepository.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/repository/UserFilialRoleRepository.java

## Criados
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/dto/UserSummaryResponse.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/dto/UserDetailResponse.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/dto/UpdateUserRequest.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/dto/UpdateUserStatusRequest.java
- Backend/src/main/java/br/com/bitabit/ecclesiacontrol/auth/dto/ResetPasswordRequest.java
- Backend/src/test/java/br/com/bitabit/ecclesiacontrol/auth/service/UserManagementServiceTest.java
- docs/user-management/issue.md
- docs/user-management/pull-request.md
- docs/user-management/arquivos.md

## Preservado fora dos commits
- docker-compose.yml: ajuste local preexistente.

## Contrato e testes
Nenhuma divergência do contrato da seção 4. Telefone: máximo de 20 caracteres, conforme V1. Nenhuma migration criada.

mvn test: 30 testes, 0 falhas, 0 erros, 0 ignorados. Inclui 11 testes novos de serviço e HTTP.

Issue e PR não publicados: conector GitHub sem permissão de escrita (403), navegador sem login. Por solicitação do usuário, os textos foram preparados para publicação manual. Substituir NUMERO_DA_ISSUE no texto do PR depois de criar a issue.
