package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.finance.domain.ChartOfAccount;
import br.com.bitabit.ecclesiacontrol.finance.dto.ChartOfAccountResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.ChartOfAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Financeiro: plano de contas", description = "Catálogo de contas compartilhado entre as filiais. Contas de repasse são reservadas.")
@RestController
@RequestMapping("/finance/accounts")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountRepository accountRepository;

    @Operation(summary = "Listar plano de contas ativo",
            description = "Contas ativas, compartilhadas entre as filiais. As contas de repasse são reservadas e não aparecem aqui. Exige FINANCE_READ.")
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<List<ChartOfAccountResponse>> findAllActive(){
        var accounts = accountRepository.findByActiveTrue().stream()
                .filter(a -> !"REPASSE".equals(a.getCategory()))
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    private ChartOfAccountResponse toResponse(ChartOfAccount a) {
        return ChartOfAccountResponse.builder()
                .id(a.getId())
                .code(a.getCode())
                .name(a.getName())
                .type(a.getType().name())
                .category(a.getCategory())
                .build();
    }
}