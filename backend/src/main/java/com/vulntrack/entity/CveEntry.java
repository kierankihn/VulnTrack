package com.vulntrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cves", indexes = {
    @Index(name = "idx_cve_id", columnList = "cve_id", unique = true),
    @Index(name = "idx_cve_severity", columnList = "severity"),
    @Index(name = "idx_cve_published", columnList = "published_date")
})
@Data
@EqualsAndHashCode(exclude = "tickets")
@ToString(exclude = "tickets")
public class CveEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cve_id", nullable = false, unique = true, length = 30)
    private String cveId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CveSeverity severity = CveSeverity.NONE;

    @Column(name = "cvss_v3_score")
    private Double cvssV3Score;

    @Column(name = "cvss_v2_score")
    private Double cvssV2Score;

    @Column(name = "cvss_v3_vector", columnDefinition = "TEXT")
    private String cvssV3Vector;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "last_modified_date")
    private LocalDate lastModifiedDate;

    @Column(name = "affected_products", columnDefinition = "TEXT")
    private String affectedProducts;

    @Column(name = "reference_urls", columnDefinition = "TEXT")
    private String references;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt = LocalDateTime.now();

    @ManyToMany(mappedBy = "cves")
    private Set<Ticket> tickets = new HashSet<>();
}
