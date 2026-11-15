package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionRequest;
import br.com.bitabit.ecclesiacontrol.finance.dto.ContributionResponse;
import br.com.bitabit.ecclesiacontrol.finance.dto.IdentifyMemberRequest;
import br.com.bitabit.ecclesiacontrol.finance.service.ContributionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Financeiro: contribuições", description = "Dízimos e ofertas. O contribuinte só aparece para quem tem CONTRIBUTION_DETAIL_READ; pode ser registrado sem membro e identificado depois.")
@RestController
@RequestMapping("/finance/contributions")
@RequiredArgsConstructor
public class ContributionController {

    private final ContributionService contributionService;

    @Operation(summary = "Registrar contribuição (dízimo ou oferta)",
            description = """
                    A conta precisa ser de categoria dízimo, oferta ou missões. Informe `memberId` quando o contribuinte
                    for conhecido (o membro precisa ser da filial ativa). Para oferta geral ou envelope sem nome, envie
                    `anonymous: true`: o contribuinte pode ser identificado depois. Mês fechado recusa o lançamento (422).""")
    @ApiResponse(responseCode = "201", description = "Contribuição registrada")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<ContributionResponse> create(@RequestBody @Valid ContributionRequest request,
                                                       @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contributionService.create(request, AuditActor.from(principal)));
    }

    @Operation(summary = "Identificar o contribuinte depois",
            description = """
                    Vincula um membro a uma contribuição registrada sem identificação (ou troca o contribuinte).
                    A troca é auditada com o valor anterior e o novo. Mês fechado recusa a alteração (422).""")
    @PatchMapping("/{id}/member")
    @PreAuthorize("hasAuthority('PERM_FINANCE_WRITE')")
    public ResponseEntity<ContributionResponse> identifyMember(
            @Parameter(description = "ID da contribuição (devolvido no registro ou na listagem)") @PathVariable UUID id,
            @RequestBody @Valid IdentifyMemberRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(contributionService.identifyMember(id, request, AuditActor.from(principal)));
    }

    @Operation(summary = "Listar contribuições",
            description = """
                    Paginada. `memberId` e `memberName` só vêm preenchidos para quem tem CONTRIBUTION_DETAIL_READ
                    (Tesoureiro e Super Admin); para os demais papéis os campos são omitidos.""")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<Page<ContributionResponse>> findAll(@ParameterObject Pageable pageable,
                                                              Authentication authentication) {
        return ResponseEntity.ok(contributionService.findAll(pageable, hasDetailPermission(authentication)));
    }

    @Operation(summary = "Contribuições de um membro",
            description = "Exige CONTRIBUTION_DETAIL_READ (Tesoureiro e Super Admin). Os demais papéis recebem 403.")
    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAuthority('PERM_CONTRIBUTION_DETAIL_READ')")
    public ResponseEntity<List<ContributionResponse>> findByMember(
            @Parameter(description = "ID do membro") @PathVariable UUID memberId) {
        return ResponseEntity.ok(contributionService.findByMember(memberId, true));
    }

    private boolean hasDetailPermission(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_CONTRIBUTION_DETAIL_READ"));
    }
}