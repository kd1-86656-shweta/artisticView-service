package com.artisticview.persistence.moderation;

import com.artisticview.common.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    long countByStatus(ReportStatus status);
}
