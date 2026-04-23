package com.vulntrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assets", indexes = {
    @Index(name = "idx_asset_project_name", columnList = "project_name")
})
@Data
@EqualsAndHashCode(exclude = "tickets")
@ToString(exclude = "tickets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String repoUrl;

    @Column(name = "project_name", length = 200)
    private String projectName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dev_group_id")
    private DevGroup devGroup;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "asset")
    private List<Ticket> tickets = new ArrayList<>();
}
