package com.trackpro.repository;

import com.trackpro.model.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.name = :role AND u.organisation.id = :orgId AND u.enabled = true")
    Page<UserEntity> findByOrganisationIdAndRoleName(@Param("orgId") UUID orgId, @Param("role") String role, Pageable pageable);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.name = :role AND u.enabled = true")
    Page<UserEntity> findByRoleName(@Param("role") String role, Pageable pageable);
}
