package com.trackpro.repository;

import com.trackpro.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    Page<Expense> findByOrganisationIdOrderByExpenseDateDesc(UUID orgId, Pageable pageable);
    Page<Expense> findByOrganisationIsNullOrderByCreatedAtDesc(Pageable pageable);
}
