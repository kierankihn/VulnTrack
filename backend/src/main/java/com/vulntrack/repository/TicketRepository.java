package com.vulntrack.repository;

import com.vulntrack.entity.Ticket;
import com.vulntrack.entity.TicketPriority;
import com.vulntrack.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.asset
        LEFT JOIN FETCH t.assignee
        WHERE (:status IS NULL OR t.status = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:assetId IS NULL OR t.asset.id = :assetId)
        AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        AND (:qPattern IS NULL OR LOWER(t.title) LIKE :qPattern)
        ORDER BY
            CASE t.priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END,
            t.createdAt DESC
        """)
    Page<Ticket> search(
        @Param("status") TicketStatus status,
        @Param("priority") TicketPriority priority,
        @Param("assetId") Long assetId,
        @Param("assigneeId") Long assigneeId,
        @Param("qPattern") String qPattern,
        Pageable pageable
    );

    @Query("""
        SELECT t FROM Ticket t
        LEFT JOIN FETCH t.asset
        LEFT JOIN FETCH t.assignee
        LEFT JOIN FETCH t.reporter
        WHERE (:status IS NULL OR t.status = :status)
        AND (:priority IS NULL OR t.priority = :priority)
        AND (:assetId IS NULL OR t.asset.id = :assetId)
        AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
        AND (:qPattern IS NULL OR LOWER(t.title) LIKE :qPattern)
        ORDER BY
            CASE t.priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END,
            t.createdAt DESC
        """)
    List<Ticket> searchAll(
        @Param("status") TicketStatus status,
        @Param("priority") TicketPriority priority,
        @Param("assetId") Long assetId,
        @Param("assigneeId") Long assigneeId,
        @Param("qPattern") String qPattern
    );

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    @Query("SELECT t FROM Ticket t WHERE t.asset.id = :assetId AND t.status NOT IN ('CLOSED', 'WONT_FIX')")
    List<Ticket> findOpenByAssetId(@Param("assetId") Long assetId);

    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END
        FROM Ticket t
        JOIN t.cves c
        WHERE c.cveId = :cveId AND t.asset.id = :assetId
        AND t.status NOT IN ('CLOSED', 'WONT_FIX')
        """)
    boolean existsOpenTicketForCveAndAsset(@Param("cveId") String cveId, @Param("assetId") Long assetId);

    @Query("SELECT t FROM Ticket t JOIN t.cves c WHERE c.id = :cveId")
    List<Ticket> findByCveId(@Param("cveId") Long cveId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignee.id = :userId AND t.status NOT IN ('CLOSED', 'WONT_FIX')")
    long countOpenTicketsByAssignee(@Param("userId") Long userId);
}
