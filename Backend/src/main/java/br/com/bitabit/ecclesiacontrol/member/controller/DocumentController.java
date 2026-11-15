package br.com.bitabit.ecclesiacontrol.member.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.member.dto.CertificateRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.TransferLetterRequest;
import br.com.bitabit.ecclesiacontrol.member.service.DocumentGenerationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Secretaria: documentos", description = "PDFs gerados sob demanda (carta de transferência e certificados). Cada emissão é auditada.")
@RestController
@RequestMapping("/members/{memberId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentGenerationService documentService;

    @Operation(summary = "Gerar carta de transferência (PDF)",
            description = "O PDF é gerado na hora e não é armazenado. O timbrado é o da filial do membro, e a emissão fica registrada na auditoria.")
    @ApiResponse(responseCode = "200", description = "Arquivo PDF",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @PostMapping("/transfer-letter")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<byte[]> transferLetter(@Parameter(description = "ID do membro") @PathVariable UUID memberId,
                                                 @RequestBody @Valid TransferLetterRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        byte[] pdf = documentService.generateTransferLetter(memberId, request, AuditActor.from(principal));
        return pdfResponse(pdf, "carta-transferencia.pdf");
    }

    @Operation(summary = "Gerar certificado (PDF)",
            description = "Tipos: BATISMO, MEMBRO, CASAMENTO ou OUTRO. O PDF é gerado na hora, não é armazenado e a emissão fica registrada na auditoria.")
    @ApiResponse(responseCode = "200", description = "Arquivo PDF",
            content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary")))
    @PostMapping("/certificate")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<byte[]> certificate(@Parameter(description = "ID do membro") @PathVariable UUID memberId,
                                              @RequestBody @Valid CertificateRequest request,
                                              @AuthenticationPrincipal AuthenticatedUser principal) {
        byte[] pdf = documentService.generateCertificate(memberId, request, AuditActor.from(principal));
        return pdfResponse(pdf, "certificado.pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}