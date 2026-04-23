package com.vulntrack.repository;

import com.vulntrack.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByProjectName(String projectName);

    List<Asset> findByDevGroupId(Long devGroupId);

    @Query("""
        SELECT a FROM Asset a
        LEFT JOIN FETCH a.devGroup
        WHERE (:qPattern IS NULL OR LOWER(a.name) LIKE :qPattern
               OR LOWER(a.projectName) LIKE :qPattern)
        """)
    Page<Asset> search(@Param("qPattern") String qPattern, Pageable pageable);
}
