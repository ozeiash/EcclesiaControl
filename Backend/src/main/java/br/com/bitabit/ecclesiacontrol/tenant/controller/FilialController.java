package br.com.bitabit.ecclesiacontrol.tenant.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.tenant.dto.CreateFilialRequest;
import br.com.bitabit.ecclesiacontrol.tenant.dto.FilialResponse;
import br.com.bitabit.ecclesiacontrol.tenant.dto.UpdateFilialRequest;
import br.com.bitabit.ecclesiacontrol.tenant.service.FilialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Filiais", description = "Cadastro das filiais. Só papéis da sede (FILIAL_MANAGE) criam e editam. Desativar fica para a fase 2.")
@RestController
@RequestMapping("/filiais")
@RequiredArgsConstructor
public class FilialController {

    private final FilialService filialService;

    @Operation(summary = "Listar filiais",
            description = "Token GLOBAL vê todas (sede primeiro); token LOCAL vê só a filial ativa.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FilialResponse>> list() {
        return ResponseEntity.ok(filialService.list());
    }

    @Operation(summary = "Buscar filial por id",
            description = "Com token LOCAL, outra filial responde 404.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FilialResponse> get(@Parameter(description = "Id da filial") @PathVariable UUID id) {
        return ResponseEntity.ok(filialService.get(id));
    }

    @Operation(summary = "Cadastrar filial",
            description = "Cria uma FILIAL ligada à sede. Nome e CNPJ são únicos. Exige FILIAL_MANAGE.")
    @ApiResponse(responseCode = "201", description = "Filial criada")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_FILIAL_MANAGE')")
    public ResponseEntity<FilialResponse> create(@RequestBody @Valid CreateFilialRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(filialService.create(request, principal));
    }

    @Operation(summary = "Editar dados cadastrais da filial",
            description = "Altera nome, razão social, CNPJ e contato. Tipo, sede, situação e início da escrituração não mudam aqui. Exige FILIAL_MANAGE.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_FILIAL_MANAGE')")
    public ResponseEntity<FilialResponse> update(@Parameter(description = "Id da filial") @PathVariable UUID id,
                                                 @RequestBody @Valid UpdateFilialRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(filialService.update(id, request, principal));
    }
}
