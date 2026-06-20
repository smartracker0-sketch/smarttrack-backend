package com.trackpro.dto.organisation;

import jakarta.validation.constraints.Size;

public record OrganisationUpdateRequest(
        @Size(max = 200) String name,
        String plan,
        String status,
        int vehicleLimit
) {}
