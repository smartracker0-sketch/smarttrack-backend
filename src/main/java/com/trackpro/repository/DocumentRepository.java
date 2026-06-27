package com.trackpro.repository;

import com.trackpro.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByOrganisationIdOrderByExpiryDateAsc(UUID orgId, Pageable pageable);
    Page<Document> findByOrganisationIsNullOrderByCreatedAtDesc(Pageable pageable);
}
