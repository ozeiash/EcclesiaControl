package br.com.bitabit.ecclesiacontrol.finance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class MembersDashboardResponse {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private boolean consolidated;
    private long totalMembers;
    private long admissionsInPeriod;
    private Map<String, Long> byStatus;
    private List<FilialCount> activeByFilial; // vazio quando não consolidado

    @Data @Builder
    public static class FilialCount {
        private UUID filialId; private String filialName; private long activeMembers;
    }
}