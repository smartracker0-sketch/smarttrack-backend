package com.trackpro.repository;

import com.trackpro.model.SmsAuditLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmsAuditLogRepository extends JpaRepository<SmsAuditLog, UUID> {

    @Query("""
            SELECT s FROM SmsAuditLog s
            WHERE s.direction = 'OUTBOUND'
              AND s.messageId IS NOT NULL
              AND s.status IN ('SENT', 'PENDING')
              AND s.sentAt >= :since
            """)
    List<SmsAuditLog> findDeliveryChecksDue(@Param("since") Instant since);
}
