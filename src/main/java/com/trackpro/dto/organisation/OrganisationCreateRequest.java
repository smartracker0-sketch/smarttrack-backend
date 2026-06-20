package com.trackpro.dto.organisation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrganisationCreateRequest(
        @NotBlank @Size(max = 200)
        String name,

        @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,120}$", message = "Slug must be lowercase letters, digits or hyphens")
        String slug,

        @NotBlank @Email
        String adminEmail,

        String plan,

        int vehicleLimit
) {}
