package br.com.bitabit.ecclesiacontrol.member.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.*;
import br.com.bitabit.ecclesiacontrol.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "Cadastrar membro",
            description = "Cria um membro na filial ativa. O CPF é único por filial e é guardado criptografado. Exige MEMBER_WRITE.")
    @ApiResponse(responseCode = "201", description = "Membro criado")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberResponse> create(@RequestBody @Valid MemberRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        MemberResponse created = memberService.create(request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Buscar membro por id",
            description = "Só encontra membros da filial ativa; de outra filial responde 404. Exige MEMBER_READ.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_MEMBER_READ')")
    public ResponseEntity<MemberResponse> findById(
            @Parameter(description = "Id do membro") @PathVariable UUID id) {
        return ResponseEntity.ok(memberService.findById(id));
    }

    @Operation(summary = "Listar membros",
            description = "Lista paginada da filial ativa, com filtro opcional por situação. Exige MEMBER_READ.")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_MEMBER_READ')")
    public ResponseEntity<Page<MemberSummaryResponse>> findAll(
            @Parameter(description = "Filtra pela situação do membro") @RequestParam(required = false) Member.MembershipStatus status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(memberService.findAll(status, pageable));
    }

    @Operation(summary = "Atualizar membro",
            description = "Substitui os dados cadastrais. A alteração é auditada (sem CPF/RG no log). Exige MEMBER_WRITE.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberResponse> update(
            @Parameter(description = "Id do membro") @PathVariable UUID id,
                                                 @RequestBody @Valid MemberRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(memberService.update(id, request, AuditActor.from(principal)));
    }

    @Operation(summary = "Alterar situação do membro",
            description = "Muda a situação (ex.: ativo, inativo, DUPLICATE). Não existe exclusão física. Exige MEMBER_WRITE.")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberResponse> updateStatus(
            @Parameter(description = "Id do membro") @PathVariable UUID id,
                                                       @RequestBody @Valid MemberStatusUpdateRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(memberService.updateStatus(id, request, AuditActor.from(principal)));
    }
}