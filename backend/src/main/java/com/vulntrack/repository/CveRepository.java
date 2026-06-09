package com.vulntrack.repository;

import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.CveSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface CveRepository extends JpaRepository<CveEntry, Long>, JpaSpecificationExecutor<CveEntry> {

    Optional<CveEntry> findByCveId(String cveId);

    boolean existsByCveId(String cveId);

    @Query("""
        SELECT c FROM CveEntry c
        WHERE (:qPattern IS NULL OR LOWER(c.cveId) LIKE :qPattern
               OR LOWER(c.description) LIKE :qPattern)
        AND (:severity IS NULL OR c.severity = :severity)
        AND (:minScore IS NULL OR c.cvssV3Score >= :minScore)
        ORDER BY c.publishedDate DESC, c.cvssV3Score DESC
        """)
    Page<CveEntry> search(
        @Param("qPattern") String qPattern,
        @Param("severity") CveSeverity severity,
        @Param("minScore") Double minScore,
        Pageable pageable
    );

    @Query("SELECT COUNT(c) FROM CveEntry c WHERE c.severity = :severity")
    long countBySeverity(@Param("severity") CveSeverity severity);

    Optional<CveEntry> findTopByOrderByLastModifiedDateDesc();

    long countByPublishedDateAfter(LocalDate date);
}
