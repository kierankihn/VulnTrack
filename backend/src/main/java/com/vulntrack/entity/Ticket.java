package com.vulntrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_ticket_status", columnList = "status"),
    @Index(name = "idx_ticket_priority", columnList = "priority"),
    @Index(name = "idx_ticket_asset", columnList = "asset_id"),
    @Index(name = "idx_ticket_assignee", columnList = "assignee_id")
})
@Data
@EqualsAndHashCode(exclude = "cves")
@ToString(exclude = "cves")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketSource source = TicketSource.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String scanInfo;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToMany
    @JoinTable(
        name = "ticket_cve",
        joinColumns = @JoinColumn(name = "ticket_id"),
        inverseJoinColumns = @JoinColumn(name = "cve_id")
    )
    private Set<CveEntry> cves = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "ticket_status_history", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "history_entry", columnDefinition = "TEXT")
    private List<String> statusHistory = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
