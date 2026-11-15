package br.com.bitabit.ecclesiacontrol.member.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRoleHistoryRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRoleHistoryResponse;
import br.com.bitabit.ecclesiacontrol.member.service.MemberRoleHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Membros: histórico ministerial", description = "Funções e títulos eclesiásticos do membro, atuais e anteriores.")
@RestController
@RequestMapping("/members/{memberId}/role-history")
@RequiredArgsConstructor
public class MemberRoleHistoryController {

    private final MemberRoleHistoryService service;


    @Operation(summary = "Registrar função ministerial",
            description = "Abre uma entrada no histórico do membro (função ou título eclesiástico). Exige MEMBER_WRITE.")
    @ApiResponse(responseCode = "201", description = "Entrada criada")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberRoleHistoryResponse> add(
            @Parameter(description = "Id do membro") @PathVariable UUID memberId,
            @RequestBody @Valid MemberRoleHistoryRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal){
        var created = service.addEntry(memberId, request, AuditActor.from(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Listar histórico ministerial do membro",
            description = "Funções atuais e anteriores. Exige MEMBER_READ.")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_MEMBER_READ')")
    public ResponseEntity<List<MemberRoleHistoryResponse>> findByMember(
            @Parameter(description = "Id do membro") @PathVariable UUID memberId) {
        return ResponseEntity.ok(service.findByMember(memberId));
    }

    @Operation(summary = "Encerrar função ministerial",
            description = "Fecha a entrada do histórico. Sem `endedAt`, usa a data de hoje. Exige MEMBER_WRITE.")
    @PatchMapping("/{historyId}/close")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<MemberRoleHistoryResponse> close(
            @Parameter(description = "Id do membro") @PathVariable UUID memberId,
            @Parameter(description = "Id da entrada do histórico") @PathVariable UUID historyId,
            @Parameter(description = "Data de encerramento (AAAA-MM-DD). Opcional.") @RequestParam(required = false) LocalDate endedAt,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(service.closeEntry(memberId, historyId, endedAt, AuditActor.from(principal)));
    }
}