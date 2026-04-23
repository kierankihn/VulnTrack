package com.vulntrack.dto;

import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.CveSeverity;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CveDto {
    private Long id;
    private String cveId;
    private String description;
    private CveSeverity severity;
    private Double cvssV3Score;
    private Double cvssV2Score;
    private String cvssV3Vector;
    private LocalDate publishedDate;
    private LocalDate lastModifiedDate;
    private String affectedProducts;
    private String references;
    private LocalDateTime syncedAt;
    private int ticketCount;

    public static CveDto from(CveEntry e) {
        CveDto dto = new CveDto();
        dto.setId(e.getId());
        dto.setCveId(e.getCveId());
        dto.setDescription(e.getDescription());
        dto.setSeverity(e.getSeverity());
        dto.setCvssV3Score(e.getCvssV3Score());
        dto.setCvssV2Score(e.getCvssV2Score());
        dto.setCvssV3Vector(e.getCvssV3Vector());
        dto.setPublishedDate(e.getPublishedDate());
        dto.setLastModifiedDate(e.getLastModifiedDate());
        dto.setAffectedProducts(e.getAffectedProducts());
        dto.setReferences(e.getReferences());
        dto.setSyncedAt(e.getSyncedAt());
        dto.setTicketCount(e.getTickets() != null ? e.getTickets().size() : 0);
        return dto;
    }
}
