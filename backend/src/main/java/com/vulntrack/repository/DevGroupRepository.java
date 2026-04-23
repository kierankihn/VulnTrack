package com.vulntrack.repository;

import com.vulntrack.entity.DevGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DevGroupRepository extends JpaRepository<DevGroup, Long> {

    Optional<DevGroup> findByName(String name);

    boolean existsByName(String name);

    Optional<DevGroup> findByLeaderId(Long leaderId);
}
