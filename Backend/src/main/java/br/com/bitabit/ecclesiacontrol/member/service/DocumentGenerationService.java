package br.com.bitabit.ecclesiacontrol.member.service;

import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.CertificateRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.TransferLetterRequest;
import br.com.bitabit.ecclesiacontrol.member.repository.MemberRepository;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MemberRepository memberRepository;
    private final TenantRepository tenantRepository;
    private final TemplateEngine templateEngine;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public byte[] generateTransferLetter(UUID memberId, TransferLetterRequest request, AuditActor actor) {
        Member member = findMemberOrThrow(memberId);
        Tenant tenant = findTenantOrThrow();

        Context context = new Context();
        context.setVariable("tenantName", tenant.getName());
        context.setVariable("tenantAddress", buildAddress(tenant));
        context.setVariable("tenantEmail", tenant.getEmail());
        context.setVariable("tenantPhone", tenant.getPhone());
        context.setVariable("memberName", member.getFullName());
        context.setVariable("memberCpf", member.getCpf()); // já vem descriptografado pelo converter
        context.setVariable("admissionDate", format(member.getAdmissionDate()));
        context.setVariable("destinationChurchName", request.getDestinationChurchName());
        context.setVariable("destinationCity", request.getDestinationCity());
        context.setVariable("reason", request.getReason());
        context.setVariable("issueDate", "Emitido em " + java.time.LocalDate.now().format(DATE_FORMAT));

        byte[] pdf = renderPdf("documents/transfer-letter", context);

        auditService.log(actor, TenantContext.getTenantId(), "GENERATE_DOCUMENT", "Member",
                member.getId(), null, java.util.Map.of("documentType", "TRANSFER_LETTER"));

        return pdf;
    }

    @Transactional(readOnly = true)
    public byte[] generateCertificate(UUID memberId, CertificateRequest request, AuditActor actor) {
        Member member = findMemberOrThrow(memberId);
        Tenant tenant = findTenantOrThrow();

        Context context = new Context();
        context.setVariable("tenantName", tenant.getName());
        context.setVariable("memberName", member.getFullName());
        context.setVariable("certificateBody", buildCertificateBody(request, member));
        context.setVariable("issueDate", "Emitido em " + java.time.LocalDate.now().format(DATE_FORMAT));

        byte[] pdf = renderPdf("documents/certificate", context);

        auditService.log(actor, TenantContext.getTenantId(), "GENERATE_DOCUMENT", "Member",
                member.getId(), null, java.util.Map.of("documentType", "CERTIFICATE", "certificateType", request.getType()));

        return pdf;
    }

    private String buildCertificateBody(CertificateRequest request, Member member) {
        return switch (request.getType()) {
            case BATISMO -> "foi batizado(a) nas águas em " + format(member.getBaptismDate()) +
                    ", professando sua fé perante esta comunidade.";
            case MEMBRO -> "é membro ativo desta igreja desde " + format(member.getAdmissionDate()) + ".";
            case CASAMENTO -> "teve seu matrimônio abençoado por esta igreja em " + format(member.getWeddingDate()) + ".";
            case OUTRO -> request.getCustomMessage() != null ? request.getCustomMessage() : "";
        };
    }

    private byte[] renderPdf(String templateName, Context context) {
        String html = templateEngine.process(templateName, context);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao gerar PDF", e);
        }
    }

    private String buildAddress(Tenant tenant) {
        return String.join(", ",
                java.util.Arrays.asList(tenant.getAddress(), tenant.getCity(), tenant.getState())
                        .stream().filter(java.util.Objects::nonNull).toList());
    }

    private String format(java.time.LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "data não informada";
    }

    private Member findMemberOrThrow(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
    }

    private Tenant findTenantOrThrow() {
        return tenantRepository.findById(TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada"));
    }
}