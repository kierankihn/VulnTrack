package com.vulntrack.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_settings")
@Data
public class SystemSetting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
