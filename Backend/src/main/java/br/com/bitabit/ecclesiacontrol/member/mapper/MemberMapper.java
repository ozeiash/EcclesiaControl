package br.com.bitabit.ecclesiacontrol.member.mapper;

import br.com.bitabit.ecclesiacontrol.member.domain.Member;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberRequest;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberResponse;
import br.com.bitabit.ecclesiacontrol.member.dto.MemberSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class MemberMapper {

    public void applyRequest(Member member, MemberRequest req) {
        member.setFullName(req.getFullName());
        member.setSocialName(req.getSocialName());
        member.setBirthDate(req.getBirthDate());
        member.setGender(req.getGender());
        member.setCpf(normalizeDigits(req.getCpf()));
        member.setRg(req.getRg());
        member.setEmail(req.getEmail());
        member.setPhonePrimary(req.getPhonePrimary());
        member.setPhoneSecondary(req.getPhoneSecondary());
        member.setPostalCode(req.getPostalCode());
        member.setStreet(req.getStreet());
        member.setNumber(req.getNumber());
        member.setComplement(req.getComplement());
        member.setNeighborhood(req.getNeighborhood());
        member.setCity(req.getCity());
        member.setState(req.getState());
        member.setAdmissionDate(req.getAdmissionDate());
        member.setAdmissionWay(req.getAdmissionWay());
        member.setBaptized(req.isBaptized());
        member.setBaptismDate(req.getBaptismDate());
        member.setConfirmedOrProfessed(req.getConfirmedOrProfessed());
        member.setMaritalStatus(req.getMaritalStatus());
        member.setWeddingDate(req.getWeddingDate());
        member.setSpouseName(req.getSpouseName());
        member.setFatherName(req.getFatherName());
        member.setMotherName(req.getMotherName());
        member.setCurrentRole(req.getCurrentRole());
        member.setPhotoUrl(req.getPhotoUrl());
        member.setObservations(req.getObservations());
    }

    public MemberResponse toResponse(Member m) {
        return MemberResponse.builder()
                .id(m.getId())
                .fullName(m.getFullName())
                .socialName(m.getSocialName())
                .birthDate(m.getBirthDate())
                .gender(m.getGender())
                .cpf(m.getCpf())
                .rg(m.getRg())
                .email(m.getEmail())
                .phonePrimary(m.getPhonePrimary())
                .phoneSecondary(m.getPhoneSecondary())
                .postalCode(m.getPostalCode())
                .street(m.getStreet())
                .number(m.getNumber())
                .complement(m.getComplement())
                .neighborhood(m.getNeighborhood())
                .city(m.getCity())
                .state(m.getState())
                .membershipStatus(m.getMembershipStatus().name())
                .admissionDate(m.getAdmissionDate())
                .admissionWay(m.getAdmissionWay())
                .exitDate(m.getExitDate())
                .exitWay(m.getExitWay())
                .baptized(m.isBaptized())
                .baptismDate(m.getBaptismDate())
                .confirmedOrProfessed(m.getConfirmedOrProfessed())
                .maritalStatus(m.getMaritalStatus())
                .weddingDate(m.getWeddingDate())
                .spouseName(m.getSpouseName())
                .fatherName(m.getFatherName())
                .motherName(m.getMotherName())
                .currentRole(m.getCurrentRole())
                .photoUrl(m.getPhotoUrl())
                .observations(m.getObservations())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    public MemberSummaryResponse toSummary(Member m) {
        return MemberSummaryResponse.builder()
                .id(m.getId())
                .fullName(m.getFullName())
                .membershipStatus(m.getMembershipStatus().name())
                .currentRole(m.getCurrentRole())
                .phonePrimary(m.getPhonePrimary())
                .build();
    }

    private String normalizeDigits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }
}