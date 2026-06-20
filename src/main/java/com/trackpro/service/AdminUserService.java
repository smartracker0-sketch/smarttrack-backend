package com.trackpro.service;

import com.trackpro.dto.admin.AdminCreateUserRequest;
import com.trackpro.dto.admin.AdminUserDto;
import com.trackpro.exception.BadRequestException;
import com.trackpro.exception.NotFoundException;
import com.trackpro.model.RoleEntity;
import com.trackpro.model.RoleName;
import com.trackpro.model.UserEntity;
import com.trackpro.repository.OrganisationRepository;
import com.trackpro.repository.RefreshTokenRepository;
import com.trackpro.repository.RoleRepository;
import com.trackpro.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganisationRepository orgRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            OrganisationRepository orgRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.orgRepository = orgRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<AdminUserDto> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserService::toDto);
    }

    public AdminUserDto get(UUID id) {
        return toDto(userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found")));
    }

    @Transactional
    public AdminUserDto create(AdminCreateUserRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BadRequestException("Email already registered");
        }
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(req.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown role: " + req.role());
        }
        RoleEntity role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Role not seeded: " + roleName));

        UserEntity user = new UserEntity();
        user.setEmail(req.email());
        user.setDisplayName(req.displayName());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setEnabled(true);
        user.setRoles(new LinkedHashSet<>());
        user.getRoles().add(role);

        if (req.organisationId() != null) {
            var org = orgRepository.findById(req.organisationId())
                    .orElseThrow(() -> new NotFoundException("Organisation not found"));
            user.setOrganisation(org);
        }

        return toDto(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto setEnabled(UUID id, boolean enabled) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenRepository.revokeAllForUser(id, java.time.Instant.now());
        }
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        userRepository.delete(user);
    }

    static AdminUserDto toDto(UserEntity u) {
        var org = u.getOrganisation();
        return new AdminUserDto(
                u.getId(),
                u.getEmail(),
                u.getDisplayName(),
                u.getRoles().stream().map(r -> r.getName().name()).toList(),
                u.isEnabled(),
                org != null ? org.getId() : null,
                org != null ? org.getName() : null,
                u.getCreatedAt()
        );
    }
}
