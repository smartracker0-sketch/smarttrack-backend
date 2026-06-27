package com.trackpro.controller;

import com.trackpro.model.UserEntity;
import com.trackpro.repository.UserRepository;
import com.trackpro.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final UserRepository userRepository;

    public DriverController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Page<DriverDto> list(@PageableDefault(size = 50) Pageable pageable) {
        var principal = CurrentUser.principal();
        UUID orgId = userRepository.findById(principal.userId())
                .map(u -> u.getOrganisation() != null ? u.getOrganisation().getId() : null)
                .orElse(null);

        Page<UserEntity> page = orgId != null
                ? userRepository.findByOrganisationIdAndRoleName(orgId, "ROLE_DRIVER", pageable)
                : userRepository.findByRoleName("ROLE_DRIVER", pageable);

        return page.map(DriverController::toDto);
    }

    private static DriverDto toDto(UserEntity u) {
        return new DriverDto(
                u.getId(),
                u.getDisplayName(),
                u.getEmail(),
                u.getScoreTotal(),
                u.getScoreBand(),
                u.getTotalTripsScored(),
                u.getLastScoredAt(),
                u.getCreatedAt()
        );
    }

    public record DriverDto(
            UUID id,
            String displayName,
            String email,
            BigDecimal scoreTotal,
            String scoreBand,
            Integer totalTripsScored,
            Instant lastScoredAt,
            Instant createdAt) {}
}
