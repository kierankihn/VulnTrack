package com.vulntrack.dto;

import com.vulntrack.entity.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class TicketDto {
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private TicketSource source;
    private Long assetId;
    private String assetName;
    private Long assigneeId;
    private String assigneeName;
    private Long reporterId;
    private String reporterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String scanInfo;
    private String comment;
    private Set<CveDto> cves;
    private List<String> statusHistory;
    private Set<TicketStatus> allowedTransitions;

    public static TicketDto from(Ticket t) {
        return from(t, t.getStatus().getAllowedTransitions());
    }

    public static TicketDto from(Ticket t, Set<TicketStatus> allowedTransitions) {
        TicketDto dto = new TicketDto();
        dto.setId(t.getId());
        dto.setTitle(t.getTitle());
        dto.setDescription(t.getDescription());
        dto.setStatus(t.getStatus());
        dto.setPriority(t.getPriority());
        dto.setSource(t.getSource());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        dto.setResolvedAt(t.getResolvedAt());
        dto.setScanInfo(t.getScanInfo());
        dto.setComment(t.getComment());
        dto.setStatusHistory(t.getStatusHistory());
        dto.setAllowedTransitions(allowedTransitions);
        if (t.getAsset() != null) {
            dto.setAssetId(t.getAsset().getId());
            dto.setAssetName(t.getAsset().getName());
        }
        if (t.getAssignee() != null) {
            dto.setAssigneeId(t.getAssignee().getId());
            dto.setAssigneeName(t.getAssignee().getFullName());
        }
        if (t.getReporter() != null) {
            dto.setReporterId(t.getReporter().getId());
            dto.setReporterName(t.getReporter().getFullName());
        }
        if (t.getCves() != null) {
            dto.setCves(t.getCves().stream().map(CveDto::from).collect(Collectors.toSet()));
        }
        return dto;
    }

    @Data
    public static class CreateRequest {
        private String title;
        private String description;
        private TicketPriority priority;
        private Long assetId;
        private Long assigneeId;
        private Long reporterId;
        private List<String> cveIds;
    }

    @Data
    public static class TransitionRequest {
        private TicketStatus targetStatus;
        private String comment;
    }

    @Data
    public static class AssigneeRequest {
        @NotNull
        private Long assigneeId;
    }
}
