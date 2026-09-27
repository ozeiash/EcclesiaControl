package br.com.bitabit.ecclesiacontrol.finance.controller;

import br.com.bitabit.ecclesiacontrol.finance.domain.ChartOfAccount;
import br.com.bitabit.ecclesiacontrol.finance.dto.ChartOfAccountResponse;
import br.com.bitabit.ecclesiacontrol.finance.repository.ChartOfAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/finance/accounts")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountRepository accountRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_FINANCE_READ')")
    public ResponseEntity<List<ChartOfAccountResponse>> findAllActive() {
        var accounts = accountRepository.findByActiveTrue().stream()
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