package com.vulntrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dev_groups")
@Data
@EqualsAndHashCode(exclude = {"members", "assets", "leader"})
@ToString(exclude = {"members", "assets", "leader"})
public class DevGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private User leader;

    @OneToMany(mappedBy = "devGroup")
    private List<User> members = new ArrayList<>();

    @OneToMany(mappedBy = "devGroup")
    private List<Asset> assets = new ArrayList<>();
}
