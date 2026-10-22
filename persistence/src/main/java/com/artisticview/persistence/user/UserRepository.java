package com.artisticview.persistence.user;

import com.artisticview.common.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    long countByCreatedAtAfter(Instant since);

    long countByStatus(UserStatus status);

    @Query("""
            SELECT u FROM User u
            WHERE (:q IS NULL OR u.username LIKE CONCAT('%', CAST(:q AS string), '%')
                              OR u.email LIKE CONCAT('%', CAST(:q AS string), '%'))
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> adminSearch(@Param("q") String q, @Param("status") UserStatus status, Pageable pageable);
}
