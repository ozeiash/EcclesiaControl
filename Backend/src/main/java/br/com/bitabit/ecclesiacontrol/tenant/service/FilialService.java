package br.com.bitabit.ecclesiacontrol.tenant.service;

import br.com.bitabit.ecclesiacontrol.core.exception.BusinessRuleException;
import br.com.bitabit.ecclesiacontrol.core.exception.ResourceNotFoundException;
import br.com.bitabit.ecclesiacontrol.core.security.AuthenticatedUser;
import br.com.bitabit.ecclesiacontrol.core.service.AuditActor;
import br.com.bitabit.ecclesiacontrol.core.service.AuditService;
import br.com.bitabit.ecclesiacontrol.core.util.TenantContext;
import br.com.bitabit.ecclesiacontrol.core.validation.Cnpj;
import br.com.bitabit.ecclesiacontrol.tenant.domain.Tenant;
import br.com.bitabit.ecclesiacontrol.tenant.dto.CreateFilialRequest;
import br.com.bitabit.ecclesiacontrol.tenant.dto.FilialResponse;
import br.com.bitabit.ecclesiacontrol.tenant.dto.UpdateFilialRequest;
import br.com.bitabit.ecclesiacontrol.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FilialService {

    /** Sede primeiro (ordem do enum), depois por nome. */
    private static final Comparator<Tenant> ORDER = Comparator.comparing(Tenant::getType)
            .thenComparing(Tenant::getName, String.CASE_INSENSITIVE_ORDER);

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    /** Token GLOBAL vê todas; token LOCAL vê só a filial ativa. */
    @Transactional(readOnly = true)
    public List<FilialResponse> list() {
        if (TenantContext.isGlobalScope()) {
            return tenantRepository.findAll().stream().sorted(ORDER).map(this::toResponse).toList();
        }
        return List.of(toResponse(find(TenantContext.getTenantId())));
    }

    @Transactional(readOnly = true)
    public FilialResponse get(UUID id) {
        if (!TenantContext.isGlobalScope() && !id.equals(TenantContext.getTenantId())) {
            throw new ResourceNotFoundException("Filial não encontrada");   // 404, como nos demais recursos
        }
        return toResponse(find(id));
    }

    @Transactional
    public FilialResponse create(CreateFilialRequest request, AuthenticatedUser principal) {
        String name = request.getName().trim();
        String cnpj = Cnpj.normalize(request.getCnpj());
        if (tenantRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("Já existe uma filial com esse nome");
        }
        if (cnpj != null && tenantRepository.existsByCnpj(cnpj)) {
            throw new BusinessRuleException("Já existe uma filial com esse CNPJ");
        }
        Tenant sede = tenantRepository.findFirstByType(Tenant.TenantType.SEDE)
                .orElseThrow(() -> new BusinessRuleException("Cadastre a sede antes de criar filiais"));

        YearMonth start = request.getAccountingStartMonth();
        Tenant filial = Tenant.builder()
                .type(Tenant.TenantType.FILIAL)
                .parentTenantId(sede.getId())
                .isActive(true)
                .accountingStartMonth(start == null ? null : start.atDay(1))
                .build();
        apply(filial, request, name, cnpj);
        filial = tenantRepository.saveAndFlush(filial);

        FilialResponse after = toResponse(filial);
        auditService.log(AuditActor.from(principal), filial.getId(), "CREATE_FILIAL", "Tenant", filial.getId(), null, after);
        return after;
    }

    @Transactional
    public FilialResponse update(UUID id, UpdateFilialRequest request, AuthenticatedUser principal) {
        Tenant filial = find(id);
        String name = request.getName().trim();
        String cnpj = Cnpj.normalize(request.getCnpj());
        if (tenantRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessRuleException("Já existe uma filial com esse nome");
        }
        if (cnpj != null && tenantRepository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new BusinessRuleException("Já existe uma filial com esse CNPJ");
        }
        FilialResponse before = toResponse(filial);
        apply(filial, request, name, cnpj);
        tenantRepository.saveAndFlush(filial);

        FilialResponse after = toResponse(filial);
        auditService.log(AuditActor.from(principal), id, "UPDATE_FILIAL", "Tenant", id, before, after);
        return after;
    }

    private Tenant find(UUID id) {
        return tenantRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Filial não encontrada"));
    }

    private void apply(Tenant t, UpdateFilialRequest r, String name, String cnpj) {
        t.setName(name);
        t.setLegalName(blankToNull(r.getLegalName()));
        t.setCnpj(cnpj);
        t.setEmail(r.getEmail() == null ? null : blankToNull(r.getEmail().toLowerCase(Locale.ROOT)));
        t.setPhone(blankToNull(r.getPhone()));
        t.setAddress(blankToNull(r.getAddress()));
        t.setCity(blankToNull(r.getCity()));
        t.setState(r.getState() == null ? null : r.getState().toUpperCase(Locale.ROOT));
        t.setPostalCode(r.getPostalCode() == null ? null : r.getPostalCode().replace("-", ""));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private FilialResponse toResponse(Tenant t) {
        return FilialResponse.builder()
                .id(t.getId()).name(t.getName()).legalName(t.getLegalName()).type(t.getType().name())
                .parentTenantId(t.getParentTenantId()).cnpj(t.getCnpj()).email(t.getEmail()).phone(t.getPhone())
                .address(t.getAddress()).city(t.getCity()).state(t.getState()).postalCode(t.getPostalCode())
                .active(Boolean.TRUE.equals(t.getIsActive()))
                .accountingStartMonth(t.getAccountingStartMonth() == null ? null : YearMonth.from(t.getAccountingStartMonth()))
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .build();
    }
}
