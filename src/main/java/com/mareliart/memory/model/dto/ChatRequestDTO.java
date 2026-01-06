package com.mareliart.memory.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank
        String sessionId,
        @NotBlank
        String message
) {
}
