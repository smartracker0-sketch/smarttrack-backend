package com.trackpro.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminCreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String role,
        UUID organisationId
) {}
