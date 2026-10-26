# [EP04-FND-06] Listar, editar e desativar usuários

## Descrição
**Como** secretário de filial ou administrador da sede,
**Eu quero** listar, detalhar, editar, ativar/desativar usuários, remover papéis e redefinir senhas,
**Para que** eu possa manter os acessos atualizados respeitando o escopo de cada filial.

## Critérios de Aceite
- [ ] Listagem paginada com filtros de filial e status e ordenação padrão por fullName.
- [ ] Vínculo com filial considera cadastro ou papel; LOCAL vê somente papéis da filial ativa.
- [ ] Edição de nome, e-mail normalizado e único e telefone de até 20 caracteres.
- [ ] LOCAL não altera usuários com acesso GLOBAL ou em outras filiais.
- [ ] GLOBAL sem SUPER_ADMIN não altera SUPER_ADMIN nem atribui esse papel.
- [ ] Desativação/suspensão bloqueia login, refresh e access token existente; reativação permite login.
- [ ] Próprio status, papel e senha não podem ser alterados por estas rotas.
- [ ] Remoção de papel valida usuário e escopo; último papel pode ser removido.
- [ ] Redefinição de senha usa PasswordEncoder e revoga refresh tokens.
- [ ] Auditoria de alterações sem registrar senhas.
- [ ] Seis rotas protegidas por PERM_USER_MANAGE conforme contrato do frontend.
- [ ] Testes de integração e mvn test aprovados.

Relacionado à #4.
