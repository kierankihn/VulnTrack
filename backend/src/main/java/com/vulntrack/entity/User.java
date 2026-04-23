package com.vulntrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_username", columnList = "username", unique = true)
})
@Data
@EqualsAndHashCode(exclude = {"assignedTickets", "reportedTickets"})
@ToString(exclude = {"assignedTickets", "reportedTickets"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, length = 200)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.DEVELOPER;

    @Column(nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dev_group_id")
    private DevGroup devGroup;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "assignee")
    private List<Ticket> assignedTickets = new ArrayList<>();

    @OneToMany(mappedBy = "reporter")
    private List<Ticket> reportedTickets = new ArrayList<>();
}
