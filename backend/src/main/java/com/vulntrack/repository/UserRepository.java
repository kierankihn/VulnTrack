package com.vulntrack.repository;

import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByRole(UserRole role);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.devGroup
        WHERE LOWER(u.username) = LOWER(:identifier)
           OR LOWER(u.email) = LOWER(:identifier)
        """)
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);

    List<User> findByDevGroupIdAndActiveTrue(Long devGroupId);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.devGroup
        WHERE (:qPattern IS NULL OR LOWER(u.username) LIKE :qPattern
               OR LOWER(u.email) LIKE :qPattern
               OR LOWER(u.fullName) LIKE :qPattern)
        AND (:role IS NULL OR u.role = :role)
        """)
    Page<User> search(@Param("qPattern") String qPattern, @Param("role") UserRole role, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN u.assignedTickets t
        WHERE u.devGroup.id = :devGroupId
          AND u.active = true
          AND u.role IN ('DEVELOPER', 'GROUP_LEAD')
        GROUP BY u.id
        ORDER BY COUNT(CASE WHEN t.status NOT IN ('CLOSED', 'WONT_FIX') THEN 1 END) ASC
        """)
    List<User> findLeastLoadedMemberInGroup(@Param("devGroupId") Long devGroupId);
}
