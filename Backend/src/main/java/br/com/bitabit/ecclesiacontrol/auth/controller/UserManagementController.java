package br.com.bitabit.ecclesiacontrol.auth.controller;

import br.com.bitabit.ecclesiacontrol.auth.dto.*;
import br.com.bitabit.ecclesiacontrol.auth.service.UserManagementService;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import br.com.bitabit.ecclesiacontrol.auth.domain.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Usuários", description = "Gestão de usuários e papéis por filial. Exige USER_MANAGE e respeita o teto de permissões: ninguém gerencia contas com permissões que não possui.")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @Operation(summary = "Criar usuário",
            description = """
                    Cria a conta na filial ativa, sem nenhum papel: em seguida, atribua um papel. O e-mail é único na
                    plataforma (sem diferenciar maiúsculas) e a senha inicial tem no mínimo 8 caracteres.""")
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<UserResponse> create(@RequestBody @Valid CreateUserRequest request,
                                               @AuthenticationPrincipal AuthenticatedUser principal) {
        var created = userManagementService.createUser(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Atribuir papel a um usuário",
            description = """
                    **Teto de permissões:** só é possível atribuir um papel cujas permissões você mesmo possui (o Super
                    Admin é isento). Não é possível atribuir papel a si mesmo. Token LOCAL: a filial é sempre a ativa e só
                    papéis LOCAL são aceitos. Token GLOBAL: informe `filialId`. Para MEMBRO, selecione `memberId`: papel e vínculo são salvos juntos; contas já vinculadas podem reutilizar a ficha atual.""")
    @ApiResponse(responseCode = "201", description = "Papel atribuído")
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<UserRoleAssignmentResponse> assignRole(
            @Parameter(description = "ID do usuário") @PathVariable UUID userId,
            @RequestBody @Valid AssignRoleRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        var assignment = userManagementService.assignRole(userId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @Operation(summary = "Vincular uma conta existente à ficha de membro",
            description = "Preserva login, senha e papéis. Atribui MEMBRO na filial da ficha, se necessário. "
                    + "Informe memberId e, para token GLOBAL, filialId. Não substitui vínculos existentes.")
    @PutMapping("/{userId}/member-link")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public UserDetailResponse linkMember(@PathVariable UUID userId,
                                        @RequestBody @Valid LinkMemberRequest request,
                                        @AuthenticationPrincipal AuthenticatedUser principal) {
        return userManagementService.linkMember(userId, request, principal);
    }

    @Operation(summary = "Listar usuários",
            description = """
                    O vínculo com a filial vem do cadastro ou de um papel. Token LOCAL: somente a filial ativa. Ordenação
                    aceita: fullName, email, status, lastLogin e createdAt; outro campo retorna 422.""")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public Page<UserSummaryResponse> list(
            @Parameter(description = "Filtra por filial. Token LOCAL só pode informar a própria")
            @RequestParam(required = false) UUID filialId,
            @Parameter(description = "Filtra por situação da conta")
            @RequestParam(required = false) User.UserStatus status,
            @ParameterObject @PageableDefault(sort = "fullName") Pageable pageable) {
        return userManagementService.listUsers(filialId, status, pageable);
    }

    @Operation(summary = "Detalhar usuário",
            description = "Para token LOCAL, usuário de outra filial retorna 404 e os papéis de outras filiais não são exibidos.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public UserDetailResponse detail(@Parameter(description = "ID do usuário") @PathVariable UUID id) {
        return userManagementService.getUser(id);
    }

    @Operation(summary = "Editar nome, e-mail e telefone",
            description = """
                    O e-mail é normalizado para minúsculas e continua único. Alterar o e-mail invalida os tokens de acesso
                    em uso por esse usuário: ele renova o acesso pelo token de renovação. Contas com permissões que você
                    não possui não podem ser editadas.""")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public UserDetailResponse update(@Parameter(description = "ID do usuário") @PathVariable UUID id,
                                     @RequestBody @Valid UpdateUserRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser principal) {
        return userManagementService.updateUser(id, request, principal);
    }

    @Operation(summary = "Ativar, inativar ou suspender",
            description = """
                    Usuário inativo ou suspenso perde o acesso na hora (login, renovação e tokens em uso) e tem os tokens
                    de renovação revogados. Não é possível alterar o próprio status.""")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public UserDetailResponse status(@Parameter(description = "ID do usuário") @PathVariable UUID id,
                                     @RequestBody @Valid UpdateUserStatusRequest request,
                                     @AuthenticationPrincipal AuthenticatedUser principal) {
        return userManagementService.updateStatus(id, request, principal);
    }

    @Operation(summary = "Remover um papel do usuário",
            description = """
                    Revoga os tokens de renovação do usuário. Token LOCAL só remove papéis LOCAL da filial ativa. O último
                    papel pode ser removido, mas sem papéis o usuário não consegue entrar. Não é possível remover o
                    próprio papel.""")
    @ApiResponse(responseCode = "204", description = "Papel removido")
    @DeleteMapping("/{userId}/roles/{assignmentId}")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<Void> removeRole(@Parameter(description = "ID do usuário") @PathVariable UUID userId,
                                           @Parameter(description = "ID do vínculo de papel (devolvido na atribuição e no detalhe)")
                                           @PathVariable UUID assignmentId,
                                           @AuthenticationPrincipal AuthenticatedUser principal) {
        userManagementService.removeRole(userId, assignmentId, principal);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Redefinir a senha",
            description = """
                    Revoga todos os tokens de renovação do usuário. Não é possível redefinir a própria senha por aqui, nem
                    a de contas com permissões que você não possui.""")
    @ApiResponse(responseCode = "204", description = "Senha redefinida")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('PERM_USER_MANAGE')")
    public ResponseEntity<Void> resetPassword(@Parameter(description = "ID do usuário") @PathVariable UUID id,
                                              @RequestBody @Valid ResetPasswordRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser principal) {
        userManagementService.resetPassword(id, request, principal);
        return ResponseEntity.noContent().build();
    }
}
