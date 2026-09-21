package br.com.bitabit.ecclesiacontrol.member.controller;

import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.member.dto.CertificateRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.TransferLetterRequest;
import br.com.bitabit.ecclesiacontrol.member.service.DocumentGenerationService;
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

@RestController
@RequestMapping("/members/{memberId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentGenerationService documentService;

    @PostMapping("/transfer-letter")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<byte[]> transferLetter(@PathVariable UUID memberId,
                                                 @RequestBody @Valid TransferLetterRequest request,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        byte[] pdf = documentService.generateTransferLetter(memberId, request, AuditActor.from(principal));
        return pdfResponse(pdf, "carta-transferencia.pdf");
    }

    @PostMapping("/certificate")
    @PreAuthorize("hasAuthority('PERM_MEMBER_WRITE')")
    public ResponseEntity<byte[]> certificate(@PathVariable UUID memberId,
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